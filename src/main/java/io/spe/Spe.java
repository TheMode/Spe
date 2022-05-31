package io.spe;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;

import static java.lang.foreign.ValueLayout.JAVA_INT;
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

        final long address = addressOf(jit, "factorial");
        final MethodHandle method = Linker.nativeLinker()
                .downcallHandle(MemoryAddress.ofLong(address),
                        FunctionDescriptor.of(JAVA_INT, JAVA_INT));

        var factorial = new Factorial() {
            @Override
            public int factorial(int n) {
                try {
                    return (int) method.invokeExact(n);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
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
