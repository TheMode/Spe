package io.spe;

import org.bytedeco.javacpp.*;
import org.bytedeco.libffi.ffi_cif;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Cleaner;

import static org.bytedeco.libffi.global.ffi.*;
import static org.bytedeco.llvm.global.LLVM.*;

public final class Spe {
    private static final String MAIN = "main";
    private static final Cleaner CLEANER = Cleaner.create();
    // a 'char *' used to retrieve error messages from LLVM
    private static final BytePointer error = new BytePointer();
    public static LLVMErrorRef err = null;

    public static void init() {
        // Initialize LLVM components
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    public static <T> @NotNull T compile(String name, Class<T> type, T fallback) {
        LLVMModuleRef module = LLVMModuleCreateWithName(name);
        fillModule(module);

        // Verify the module using LLVMVerifier
        if (LLVMVerifyModule(module, LLVMPrintMessageAction, error) != 0) {
            LLVMDisposeMessage(error);
            throw new RuntimeException("Module verification failed: " + error.getString());
        }

        // Create a pass pipeline using the legacy pass manager
        LLVMPassManagerRef pm = LLVMCreatePassManager();
        LLVMAddAggressiveInstCombinerPass(pm);
        LLVMAddNewGVNPass(pm);
        LLVMAddCFGSimplificationPass(pm);
        LLVMRunPassManager(pm, module);
        LLVMDumpModule(module);

        LLVMOrcThreadSafeContextRef threadContext = LLVMOrcCreateNewThreadSafeContext();
        LLVMOrcThreadSafeModuleRef threadModule = LLVMOrcCreateNewThreadSafeModule(module, threadContext);
        // Execute using OrcJIT
        LLVMOrcLLJITRef jit = new LLVMOrcLLJITRef();
        LLVMOrcLLJITBuilderRef jitBuilder = LLVMOrcCreateLLJITBuilder();
        if ((err = LLVMOrcCreateLLJIT(jit, jitBuilder)) != null) {
            LLVMConsumeError(err);
            throw new RuntimeException("Failed to create LLJIT");
        }
        LLVMOrcJITDylibRef mainDylib = LLVMOrcLLJITGetMainJITDylib(jit);
        if ((err = LLVMOrcLLJITAddLLVMIRModule(jit, mainDylib, threadModule)) != null) {
            System.err.println("Failed to add LLVM IR module: " + LLVMGetErrorMessage(err));
            LLVMConsumeError(err);
            throw new RuntimeException("Failed to add LLVM IR module");
        }
        final LongPointer res = new LongPointer(1);
        if ((err = LLVMOrcLLJITLookup(jit, res, MAIN)) != null) {
            System.err.println("Failed to look up '" + MAIN + "' symbol: " + LLVMGetErrorMessage(err));
            LLVMConsumeError(err);
            throw new RuntimeException("Failed to look up '" + MAIN + "' symbol");
        }

        // Call the function
        ffi_cif cif = new ffi_cif();
        PointerPointer<Pointer> arguments = new PointerPointer<>(1).put(0, ffi_type_sint);
        if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, 1, ffi_type_sint, arguments) != FFI_OK) {
            throw new RuntimeException("Failed to prepare the libffi cif");
        }

        Pointer function = new Pointer() {{
            address = res.get();
        }};
        var factorial = new Factorial() {
            @Override
            public int factorial(int n) {
                PointerPointer<Pointer> values = new PointerPointer<>(1).put(new IntPointer(1).put(n));
                IntPointer returns = new IntPointer(1);
                ffi_call(cif, function, returns, values);
                return returns.get();
            }
        };
        CLEANER.register(factorial, () -> {
            // Stage 6: Dispose of the allocated resources
            LLVMOrcDisposeLLJIT(jit);
            LLVMDisposePassManager(pm);
        });
        return (T) factorial;
    }

    private static void fillModule(LLVMModuleRef module) {
        LLVMBuilderRef builder = LLVMCreateBuilder();
        try {
            LLVMTypeRef i32Type = LLVMInt32Type();
            LLVMTypeRef factorialType = LLVMFunctionType(i32Type, i32Type, /* argumentCount */ 1, /* isVariadic */ 0);

            LLVMValueRef factorial = LLVMAddFunction(module, MAIN, factorialType);
            LLVMSetFunctionCallConv(factorial, LLVMCCallConv);

            LLVMValueRef n = LLVMGetParam(factorial, 0);
            LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
            LLVMValueRef one = LLVMConstInt(i32Type, 1, 0);
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(factorial, "entry");
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(factorial, "if_false");
            LLVMBasicBlockRef exit = LLVMAppendBasicBlock(factorial, "exit");

            LLVMPositionBuilderAtEnd(builder, entry);
            LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntEQ, n, zero, "condition = n == 0");
            LLVMBuildCondBr(builder, condition, exit, ifFalse);

            LLVMPositionBuilderAtEnd(builder, ifFalse);
            LLVMValueRef nMinusOne = LLVMBuildSub(builder, n, one, "nMinusOne = n - 1");
            PointerPointer<Pointer> arguments = new PointerPointer<>(1).put(0, nMinusOne);
            LLVMValueRef factorialResult = LLVMBuildCall2(builder, factorialType, factorial, arguments, 1, "factorialResult = factorial(nMinusOne)");
            LLVMValueRef resultIfFalse = LLVMBuildMul(builder, n, factorialResult, "resultIfFalse = n * factorialResult");
            LLVMBuildBr(builder, exit);

            LLVMPositionBuilderAtEnd(builder, exit);
            LLVMValueRef phi = LLVMBuildPhi(builder, i32Type, "result");
            PointerPointer<Pointer> phiValues = new PointerPointer<>(2)
                    .put(0, one)
                    .put(1, resultIfFalse);
            PointerPointer<Pointer> phiBlocks = new PointerPointer<>(2)
                    .put(0, entry)
                    .put(1, ifFalse);
            LLVMAddIncoming(phi, phiValues, phiBlocks, 2);
            LLVMBuildRet(builder, phi);
        } finally {
            LLVMDisposeBuilder(builder);
        }
    }
}
