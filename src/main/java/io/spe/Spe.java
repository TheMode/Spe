package io.spe;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.ref.Cleaner;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

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

    public static <T> @NotNull T compile(Class<T> interfaceType, Class<? extends T> implementationType) {
        LLVMModuleRef module = LLVMModuleCreateWithName(implementationType.getSimpleName());
        LLVMBuilderRef builder = LLVMCreateBuilder();
        try {
            SpeCompiler.compile(module, builder, implementationType);
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

        final String methodName = interfaceType.getMethods()[0].getName();
        final long address = addressOf(jit, methodName);
        final Class<T> generated = SpeClassWriter.generate(interfaceType,
                List.of(new SpeClassWriter.MethodEntry(methodName, FunctionDescriptor.of(JAVA_INT, JAVA_INT), address)));

        final T factorial;
        try {
            factorial = generated.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        CLEANER.register(factorial, () -> {
            // Dispose of the allocated resources
            LLVMOrcDisposeLLJIT(jit);
            LLVMDisposePassManager(pm);
        });
        return factorial;
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
