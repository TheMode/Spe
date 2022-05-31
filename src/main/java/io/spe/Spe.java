package io.spe;

import org.bytedeco.javacpp.*;
import org.bytedeco.libffi.ffi_cif;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.Cleaner;

import static org.bytedeco.libffi.global.ffi.*;
import static org.bytedeco.llvm.global.LLVM.*;

public final class Spe {
    private static final Cleaner CLEANER = Cleaner.create();
    // a 'char *' used to retrieve error messages from LLVM
    private static final BytePointer error = new BytePointer();
    public static LLVMErrorRef err = null;

    static {
        // Initialize LLVM components
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    public static <T> @NotNull T compile(Class<T> type) {
        LLVMModuleRef module = LLVMModuleCreateWithName(type.getSimpleName());
        LLVMBuilderRef builder = LLVMCreateBuilder();
        try {
            SpeCompiler.compile(module, builder, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            LLVMDisposeBuilder(builder);
        }

        // Verify the module using LLVMVerifier
        if (LLVMVerifyModule(module, LLVMPrintMessageAction, error) != 0) {
            LLVMDisposeMessage(error);
            throw new RuntimeException();
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

        // Call the function
        ffi_cif cif = new ffi_cif();
        PointerPointer<Pointer> arguments = new PointerPointer<>(1).put(0, ffi_type_sint);
        if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, 1, ffi_type_sint, arguments) != FFI_OK) {
            throw new RuntimeException("Failed to prepare the libffi cif");
        }

        Pointer function = new Pointer() {{
            address = addressOf(jit, "factorial");
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
            // Dispose of the allocated resources
            LLVMOrcDisposeLLJIT(jit);
            LLVMDisposePassManager(pm);
        });
        return (T) factorial;
    }

    private static long addressOf(LLVMOrcLLJITRef jit, String name) {
        final LongPointer res = new LongPointer(1);
        if ((err = LLVMOrcLLJITLookup(jit, res, name)) != null) {
            LLVMConsumeError(err);
            throw new RuntimeException("Failed to look up symbol: " + name);
        }
        return res.get();
    }
}
