package io.spe;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Cleaner;

import static org.bytedeco.llvm.global.LLVM.*;

public final class Spe {
    private static final Cleaner CLEANER = Cleaner.create();
    // a 'char *' used to retrieve error messages from LLVM
    private static final BytePointer error = new BytePointer();

    public static void init() {
        // Initialize LLVM components
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    public static <T> @NotNull T compile(String name, Class<T> type) {
        LLVMContextRef context = LLVMContextCreate();
        LLVMTypeRef i32Type = LLVMInt32TypeInContext(context);
        LLVMModuleRef module = createModule(name, context);

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

        // Execute the code using MCJIT
        LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
        LLVMMCJITCompilerOptions options = new LLVMMCJITCompilerOptions();
        if (LLVMCreateMCJITCompilerForModule(engine, module, options, 3, error) != 0) {
            LLVMDisposeMessage(error);
            throw new RuntimeException("Failed to create JIT compiler: " + error.getString());
        }

        var fun = LLVMGetNamedFunction(module, name);

        var factorial = new HelloWorld.Factorial(){
            @Override
            public int factorial(int n) {
                LLVMGenericValueRef argument = LLVMCreateGenericValueOfInt(i32Type, n, /* signExtend */ 0);
                LLVMGenericValueRef result = LLVMRunFunction(engine, fun, /* argumentCount */ 1, argument);
                return (int) LLVMGenericValueToInt(result, /* signExtend */ 0);
            }
        };
        CLEANER.register(factorial, () -> {
            // Stage 6: Dispose of the allocated resources
            LLVMDisposeExecutionEngine(engine);
            LLVMDisposePassManager(pm);
            //LLVMDisposeBuilder(builder);
            LLVMContextDispose(context);
        });
        return (T) factorial;
    }

    private static LLVMModuleRef createModule(String name, LLVMContextRef context) {
        LLVMModuleRef module = LLVMModuleCreateWithNameInContext(name, context);
        LLVMBuilderRef builder = LLVMCreateBuilderInContext(context);
        LLVMTypeRef i32Type = LLVMInt32TypeInContext(context);
        LLVMTypeRef factorialType = LLVMFunctionType(i32Type, i32Type, /* argumentCount */ 1, /* isVariadic */ 0);

        LLVMValueRef factorial = LLVMAddFunction(module, name, factorialType);
        LLVMSetFunctionCallConv(factorial, LLVMCCallConv);

        LLVMValueRef n = LLVMGetParam(factorial, /* parameterIndex */0);
        LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
        LLVMValueRef one = LLVMConstInt(i32Type, 1, /* signExtend */ 0);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, factorial, "entry");
        LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlockInContext(context, factorial, "if_false");
        LLVMBasicBlockRef exit = LLVMAppendBasicBlockInContext(context, factorial, "exit");

        LLVMPositionBuilderAtEnd(builder, entry);
        LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntEQ, n, zero, "condition = n == 0");
        LLVMBuildCondBr(builder, condition, exit, ifFalse);

        LLVMPositionBuilderAtEnd(builder, ifFalse);
        LLVMValueRef nMinusOne = LLVMBuildSub(builder, n, one, "nMinusOne = n - 1");
        PointerPointer<Pointer> arguments = new PointerPointer<>(1)
                .put(0, nMinusOne);
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
        LLVMAddIncoming(phi, phiValues, phiBlocks, /* pairCount */ 2);
        LLVMBuildRet(builder, phi);

        return module;
    }
}
