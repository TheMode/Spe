package io.spe;

import org.objectweb.asm.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import static java.lang.foreign.ValueLayout.*;
import static org.objectweb.asm.Opcodes.*;

final class SpeClassWriter {
    private static final SpeClassLoader LOADER = new SpeClassLoader();

    static <T> Class<T> generate(Class<T> interfaceType, List<MethodEntry> methods) {
        final String name = interfaceType.getName() + "Native";
        final byte[] bytes = dump(interfaceType, methods);
        return (Class<T>) LOADER.defineClass(name, bytes);
    }

    private static byte[] dump(Class<?> interfaceType, List<MethodEntry> methods) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;

        final String fileName = interfaceType.getSimpleName() + "Native";
        final String className = (interfaceType.getName() + "Native").replace('.', '/');
        final String internalDescriptor = "L" + className + ";";

        classWriter.visit(V19 | V_PREVIEW, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, className, null, "java/lang/Object", new String[]{Type.getInternalName(interfaceType)});
        classWriter.visitSource(fileName + ".java", null);

        // Fields
        {
            for (MethodEntry method : methods) {
                fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, method.constantName(), "Ljava/lang/invoke/MethodHandle;", null, null);
                fieldVisitor.visitEnd();
            }
        }
        // Constructor
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", internalDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(-1, -1);
            methodVisitor.visitEnd();
        }
        // Methods
        {
            for (Method m : interfaceType.getMethods()) {
                final String methodName = m.getName();
                final String methodDescriptor = Type.getMethodDescriptor(m);
                final String constantName = toConstantName(methodName);
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, null);
                methodVisitor.visitCode();
                methodVisitor.visitFieldInsn(GETSTATIC, className, constantName, "Ljava/lang/invoke/MethodHandle;");
                methodVisitor.visitVarInsn(SpeSignature.loadOpcode(m.getReturnType()), 1);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", methodDescriptor, false);
                methodVisitor.visitInsn(SpeSignature.returnOpcode(m.getReturnType()));
            }
            methodVisitor.visitMaxs(-1, -1);
            methodVisitor.visitEnd();
        }
        // Fields initializer
        {
            methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            for (MethodEntry entry : methods) {
                final String constantName = entry.constantName();
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/Linker", "nativeLinker", "()Ljava/lang/foreign/Linker;", true);
                methodVisitor.visitLdcInsn(entry.address());
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/MemoryAddress", "ofLong", "(J)Ljava/lang/foreign/MemoryAddress;", true);
                // Return
                final MemoryLayout returnLayout = entry.descriptor().returnLayout().get();
                methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/foreign/ValueLayout", getter(returnLayout), Type.getDescriptor(returnLayout.getClass()));
                // Arguments
                final List<MemoryLayout> argumentTypes = entry.descriptor().argumentLayouts();
                appendInteger(methodVisitor, argumentTypes.size());
                methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/foreign/MemoryLayout");
                methodVisitor.visitInsn(DUP);
                for (int i = 0; i < argumentTypes.size(); i++) {
                    appendInteger(methodVisitor, i);
                    final MemoryLayout layout = argumentTypes.get(i);
                    methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/foreign/ValueLayout", getter(layout), Type.getDescriptor(layout.getClass()));
                    methodVisitor.visitInsn(AASTORE);
                }
                // Create method handle
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/FunctionDescriptor", "of", "(Ljava/lang/foreign/MemoryLayout;[Ljava/lang/foreign/MemoryLayout;)Ljava/lang/foreign/FunctionDescriptor;", false);
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/lang/foreign/Linker", "downcallHandle", "(Ljava/lang/foreign/Addressable;Ljava/lang/foreign/FunctionDescriptor;)Ljava/lang/invoke/MethodHandle;", true);
                methodVisitor.visitFieldInsn(PUTSTATIC, className, constantName, "Ljava/lang/invoke/MethodHandle;");
            }
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(-1, -1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    record MethodEntry(String name, FunctionDescriptor descriptor, long address) {
        String constantName() {
            return toConstantName(name);
        }
    }

    private static String toConstantName(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    private static String getter(MemoryLayout layout) {
        if (layout == JAVA_BOOLEAN) return "JAVA_BOOLEAN";
        if (layout == JAVA_BYTE) return "JAVA_BYTE";
        if (layout == JAVA_CHAR) return "JAVA_CHAR";
        if (layout == JAVA_SHORT) return "JAVA_SHORT";
        if (layout == JAVA_INT) return "JAVA_INT";
        if (layout == JAVA_LONG) return "JAVA_LONG";
        if (layout == JAVA_DOUBLE) return "JAVA_DOUBLE";
        if (layout == JAVA_FLOAT) return "JAVA_FLOAT";
        throw new IllegalArgumentException("Unsupported layout: " + layout);
    }

    private static void appendInteger(MethodVisitor methodVisitor, int n) {
        if (n >= 0 && n <= 5) methodVisitor.visitInsn(ICONST_0 + n);
        else methodVisitor.visitLdcInsn(n);
    }
}
