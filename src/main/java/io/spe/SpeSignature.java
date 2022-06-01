package io.spe;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.util.Arrays;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static java.util.Map.entry;
import static org.objectweb.asm.Opcodes.*;

final class SpeSignature {
    private static final Map<Class<?>, MemoryLayout> TYPE_TO_LAYOUT = Map.ofEntries(
            entry(boolean.class, JAVA_BOOLEAN), entry(byte.class, JAVA_BYTE),
            entry(char.class, JAVA_CHAR), entry(short.class, JAVA_SHORT),
            entry(int.class, JAVA_INT), entry(long.class, JAVA_LONG),
            entry(float.class, JAVA_FLOAT), entry(double.class, JAVA_DOUBLE));

    private static final Map<Class<?>, Integer> TYPE_TO_LOAD = Map.ofEntries(
            entry(boolean.class, ILOAD), entry(byte.class, ILOAD),
            entry(char.class, ILOAD), entry(short.class, ILOAD),
            entry(int.class, ILOAD), entry(long.class, LLOAD),
            entry(float.class, FLOAD), entry(double.class, DLOAD));

    private static final Map<Class<?>, Integer> TYPE_TO_RETURN = Map.ofEntries(
            entry(void.class, RETURN),
            entry(boolean.class, IRETURN), entry(byte.class, IRETURN),
            entry(char.class, IRETURN), entry(short.class, IRETURN),
            entry(int.class, IRETURN), entry(long.class, LRETURN),
            entry(float.class, FRETURN), entry(double.class, DRETURN));

    public static FunctionDescriptor descriptor(Class<?> result, Class<?>[] parameterTypes) {
        final MemoryLayout[] params = Arrays.stream(parameterTypes).map(SpeSignature::toLayout).toArray(MemoryLayout[]::new);
        if (result == void.class) {
            return FunctionDescriptor.ofVoid(params);
        } else {
            final MemoryLayout returnType = SpeSignature.toLayout(result);
            return FunctionDescriptor.of(returnType, params);
        }
    }

    private static MemoryLayout toLayout(Class<?> type) {
        final MemoryLayout layout = TYPE_TO_LAYOUT.get(type);
        if (layout == null) throw new IllegalArgumentException("Unsupported type: " + type);
        return layout;
    }

    static int loadOpcode(Class<?> type) {
        final Integer opcode = TYPE_TO_LOAD.get(type);
        if (opcode == null) throw new IllegalArgumentException("Unsupported type: " + type);
        return opcode;
    }

    static int returnOpcode(Class<?> type) {
        final Integer opcode = TYPE_TO_RETURN.get(type);
        if (opcode == null) throw new IllegalArgumentException("Unsupported type: " + type);
        return opcode;
    }
}
