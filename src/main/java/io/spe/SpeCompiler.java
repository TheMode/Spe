package io.spe;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.objectweb.asm.Opcodes.*;

final class SpeCompiler {
    static void compile(LLVMModuleRef module, LLVMBuilderRef builder, Class<?> type) throws IOException {
        new SpeCompiler(module, builder, type);
    }

    private final LLVMModuleRef module;
    private final LLVMBuilderRef builder;
    private final Class<?> type;

    private SpeCompiler(LLVMModuleRef module, LLVMBuilderRef builder, Class<?> type) throws IOException {
        this.module = module;
        this.builder = builder;
        this.type = type;

        ClassReader cr = new ClassReader(type.getName());
        var visitor = new SpeClassVisitor();
        cr.accept(visitor, ClassReader.SKIP_DEBUG);
    }

    private final class SpeClassVisitor extends ClassVisitor {
        SpeClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals("<init>"))
                return null; // Constructor not supported
            System.out.println("class " + access + " " + name + " " + descriptor + " " + signature + " " + Arrays.toString(exceptions));
            return new SpeMethodVisitor(name, super.visitMethod(access, name, descriptor, signature, exceptions));
        }
    }

    private final class SpeMethodVisitor extends MethodVisitor {
        final LLVMTypeRef type;
        final LLVMValueRef function;
        ArrayDeque<LLVMValueRef> stack = new ArrayDeque<>();
        Map<Label, LLVMBasicBlockRef> labels = new HashMap<>();

        Map<Integer, LLVMValueRef> variables = new HashMap<>();

        SpeMethodVisitor(String name, MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
            this.type = LLVMFunctionType(LLVMInt32Type(), LLVMInt32Type(), 1, 0);
            this.function = LLVMAddFunction(module, name, type);
            LLVMSetFunctionCallConv(function, LLVMCCallConv);

            variables.put(0, function);
            variables.put(1, LLVMGetParam(function, 0));

            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(function, "entry");
            LLVMPositionBuilderAtEnd(builder, entry);
        }

        @Override
        public void visitInsn(int opcode) {
            System.out.println("single " + opcode);
            switch (opcode) {
                case ICONST_M1 -> stack.push(LLVMConstInt(LLVMInt32Type(), -1, 0));
                case ICONST_0 -> stack.push(LLVMConstInt(LLVMInt32Type(), 0, 0));
                case ICONST_1 -> stack.push(LLVMConstInt(LLVMInt32Type(), 1, 0));
                case ICONST_2 -> stack.push(LLVMConstInt(LLVMInt32Type(), 2, 0));
                case ICONST_3 -> stack.push(LLVMConstInt(LLVMInt32Type(), 3, 0));
                case ICONST_4 -> stack.push(LLVMConstInt(LLVMInt32Type(), 4, 0));
                case ICONST_5 -> stack.push(LLVMConstInt(LLVMInt32Type(), 5, 0));
                case LCONST_0 -> stack.push(LLVMConstInt(LLVMInt64Type(), 0, 0));
                case LCONST_1 -> stack.push(LLVMConstInt(LLVMInt64Type(), 1, 0));
                case FCONST_0 -> stack.push(LLVMConstReal(LLVMFloatType(), 0.0f));
                case FCONST_1 -> stack.push(LLVMConstReal(LLVMFloatType(), 1.0f));
                case FCONST_2 -> stack.push(LLVMConstReal(LLVMFloatType(), 2.0f));
                case DCONST_0 -> stack.push(LLVMConstReal(LLVMDoubleType(), 0.0));
                case DCONST_1 -> stack.push(LLVMConstReal(LLVMDoubleType(), 1.0));

                case ISUB -> {
                    LLVMValueRef right = stack.pop();
                    LLVMValueRef left = stack.pop();
                    stack.push(LLVMBuildSub(builder, left, right, "ISUB"));
                }

                case IMUL -> {
                    LLVMValueRef right = stack.pop();
                    LLVMValueRef left = stack.pop();
                    stack.push(LLVMBuildMul(builder, left, right, "IMUL"));
                }

                case IRETURN -> {
                    LLVMValueRef value = stack.pop();
                    LLVMBuildRet(builder, value);
                }
                default -> throw new IllegalArgumentException("Unsupported opcode: " + opcode);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            System.out.println("int " + opcode + " " + operand);
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            System.out.println("jump " + opcode + " " + label);
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(function, "if_false");
            LLVMBasicBlockRef exit = LLVMAppendBasicBlock(function, "exit");
            LLVMValueRef condition = switch (opcode) {
                case IFEQ ->
                        LLVMBuildICmp(builder, LLVMIntEQ, stack.poll(), LLVMConstInt(LLVMInt32Type(), 0, 0), "condition = value == 0");
                case IFNE ->
                        LLVMBuildICmp(builder, LLVMIntNE, stack.poll(), LLVMConstInt(LLVMInt32Type(), 0, 0), "condition = value != 0");
                default -> throw new IllegalArgumentException("Unsupported jump opcode: " + opcode);
            };
            LLVMBuildCondBr(builder, condition, exit, ifFalse);
            LLVMPositionBuilderAtEnd(builder, ifFalse);

            this.labels.put(label, exit);
        }

        @Override
        public void visitLabel(Label label) {
            System.out.println("label " + label);
            LLVMBasicBlockRef block = this.labels.remove(label);
            if (block == null) throw new IllegalArgumentException("Unknown label: " + label);
            LLVMPositionBuilderAtEnd(builder, block);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            System.out.println("variable " + opcode + " " + varIndex);
            switch (opcode) {
                case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> stack.push(variables.get(varIndex));
                default -> throw new IllegalArgumentException("Unsupported opcode: " + opcode);
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            System.out.println("field " + opcode + " " + owner + " " + name + " " + descriptor);
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            System.out.println("method " + opcode + " " + owner + " " + name + " " + descriptor + " " + isInterface);
            switch (opcode) {
                case INVOKEVIRTUAL -> {
                    LLVMValueRef param = stack.pop();
                    LLVMValueRef objectRef = stack.pop();
                    PointerPointer<Pointer> arguments = new PointerPointer<>(1).put(param);
                    stack.push(LLVMBuildCall2(builder, type, function, arguments, 1, "factorialResult = factorial(nMinusOne)"));
                }
                default -> throw new IllegalArgumentException("Unsupported opcode: " + opcode);
            }
        }
    }
}
