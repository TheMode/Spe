package io.spe;

import org.objectweb.asm.*;

import java.lang.foreign.FunctionDescriptor;
import java.util.List;
import java.util.Locale;

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
        final String internalDescriptor = "L"+className+";";

        classWriter.visit(V19 | V_PREVIEW, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, className, null, "java/lang/Object", new String[]{Type.getInternalName(interfaceType)});
        classWriter.visitSource(fileName + ".java", null);

        // Fields
        {
            for (MethodEntry method : methods) {
                final String constantName = method.constantName();
                fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, constantName, "Ljava/lang/invoke/MethodHandle;", null, null);
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
            for (var m : interfaceType.getMethods()) {
                final String methodName = m.getName();
                final String methodDescriptor = Type.getMethodDescriptor(m);
                final String constantName = toConstantName(methodName);
                methodVisitor = classWriter.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                Label label1 = new Label();
                Label label2 = new Label();
                methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
                methodVisitor.visitLabel(label0);
                methodVisitor.visitFieldInsn(GETSTATIC, className, constantName, "Ljava/lang/invoke/MethodHandle;");
                methodVisitor.visitVarInsn(ILOAD, 1);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "(I)I", false);
                methodVisitor.visitLabel(label1);
                methodVisitor.visitInsn(IRETURN);
                methodVisitor.visitLabel(label2);
                methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
                methodVisitor.visitVarInsn(ASTORE, 2);
                Label label3 = new Label();
                methodVisitor.visitLabel(label3);
                methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
                methodVisitor.visitInsn(DUP);
                methodVisitor.visitVarInsn(ALOAD, 2);
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
                methodVisitor.visitInsn(ATHROW);
                Label label4 = new Label();
                methodVisitor.visitLabel(label4);
                methodVisitor.visitLocalVariable("e", "Ljava/lang/Throwable;", null, label3, label4, 2);
                methodVisitor.visitLocalVariable("this", internalDescriptor, null, label0, label4, 0);
                methodVisitor.visitLocalVariable("n", "I", null, label0, label4, 1);
            }
            methodVisitor.visitMaxs(-1, -1);
            methodVisitor.visitEnd();
        }
        // Fields initializer
        {
            methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            for (var entry : methods) {
                final String constantName = entry.constantName();
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/Linker", "nativeLinker", "()Ljava/lang/foreign/Linker;", true);
                methodVisitor.visitLdcInsn(entry.address());
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/foreign/MemoryAddress", "ofLong", "(J)Ljava/lang/foreign/MemoryAddress;", true);
                methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT", "Ljava/lang/foreign/ValueLayout$OfInt;");
                methodVisitor.visitInsn(ICONST_1);
                methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/foreign/MemoryLayout");
                methodVisitor.visitInsn(DUP);
                methodVisitor.visitInsn(ICONST_0);
                methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/foreign/ValueLayout", "JAVA_INT", "Ljava/lang/foreign/ValueLayout$OfInt;");
                methodVisitor.visitInsn(AASTORE);
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
}
