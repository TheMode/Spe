package io.spe;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.llvm.LLVM.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.bytedeco.llvm.global.LLVM.*;

public final class Spe {
    private static final Map<Class<?>, FactoryImpl<?>> FACTORIES = new ConcurrentHashMap<>();
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

    public static synchronized void free() {
        FACTORIES.forEach((aClass, speFactory) -> LLVMOrcDisposeLLJIT(speFactory.jit()));
        FACTORIES.clear();
    }

    public static <T> @NotNull T compileAndCreate(Class<T> interfaceType, Class<? extends T> implementationType) {
        final SpeFactory<T> factory = compile(interfaceType, implementationType);
        return factory.create();
    }

    public static <T> @NotNull SpeFactory<T> compile(Class<T> interfaceType, Class<? extends T> implementationType) {
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
        LLVMDisposePassManager(pm);
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

        final FactoryImpl<T> factory = new FactoryImpl<>(generated, jit);
        synchronized (Spe.class) {
            FACTORIES.put(implementationType, factory);
        }
        return factory;
    }

    private static long addressOf(LLVMOrcLLJITRef jit, String name) {
        final LongPointer res = new LongPointer(1);
        if ((err = LLVMOrcLLJITLookup(jit, res, name)) != null) {
            LLVMConsumeError(err);
            throw new RuntimeException("Failed to look up symbol: " + name);
        }
        return res.get();
    }

    private record FactoryImpl<T>(Class<T> type, LLVMOrcLLJITRef jit) implements SpeFactory<T> {
        @Override
        public @NotNull T create() {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
