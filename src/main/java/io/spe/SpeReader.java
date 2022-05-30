package io.spe;

import org.objectweb.asm.*;

import java.io.IOException;
import java.util.Arrays;

final class SpeReader {
    static void readClass(String targetMethod, Class<?> clazz) throws IOException {
        System.out.println("read: " + clazz);
        ClassReader cr = new ClassReader( clazz.getName());
        var visitor = new SpeClassVisitor(targetMethod);
        cr.accept(visitor, ClassReader.SKIP_DEBUG);
    }

    private static final class SpeClassVisitor extends ClassVisitor {
        private final String targetMethod;

        SpeClassVisitor(String targetMethod) {
            super(Opcodes.ASM9);
            this.targetMethod = targetMethod;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals(targetMethod)) {
                System.out.println("class " + access + " " + name + " " + descriptor + " " + signature + " " + Arrays.toString(exceptions));
                return new SpeMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
            }
            return null;
        }
    }

    private static final class SpeMethodVisitor extends MethodVisitor {
        SpeMethodVisitor(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        @Override
        public void visitInsn(int opcode) {
            System.out.println("single " + opcode);
            //super.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            System.out.println("int " + opcode + " " + operand);
            //super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            System.out.println("jump " + opcode + " " + label);
            //super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(Label label) {
            System.out.println("label " + label);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            System.out.println("variable " + opcode + " " + varIndex);
            //super.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            System.out.println("field " + opcode + " " + owner + " " + name + " " + descriptor);
            //super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            System.out.println("method " + opcode + " " + owner + " " + name + " " + descriptor + " " + isInterface);
            //super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
