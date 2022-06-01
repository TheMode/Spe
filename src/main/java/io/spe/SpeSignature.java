package io.spe;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.util.Arrays;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static java.util.Map.entry;
import static org.objectweb.asm.Opcodes.*;

final class SpeSignature {
    private static final Map<Class<?>, MemoryLayout> BASICS = Map.ofEntries(
            entry(boolean.class, JAVA_BOOLEAN), entry(byte.class, JAVA_BYTE),
            entry(char.class, JAVA_CHAR), entry(short.class, JAVA_SHORT),
            entry(int.class, JAVA_INT), entry(long.class, JAVA_LONG),
            entry(float.class, JAVA_FLOAT), entry(double.class, JAVA_DOUBLE));

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
        final MemoryLayout layout = BASICS.get(type);
        if (layout == null) throw new IllegalArgumentException("Unsupported type: " + type);
        return layout;
    }

    static int loadOpcode(Class<?> type) {
        if (type == boolean.class || type == byte.class || type == char.class || type == short.class || type == int.class)
            return ILOAD;
        if (type == long.class) return LLOAD;
        if (type == float.class) return FLOAD;
        if (type == double.class) return DLOAD;
        throw new IllegalArgumentException("Unsupported layout: " + type);
    }

    static int returnOpcode(Class<?> type) {
        if (type == void.class) return RETURN;
        if (type == boolean.class || type == byte.class || type == char.class || type == short.class || type == int.class)
            return IRETURN;
        if (type == long.class) return LRETURN;
        if (type == float.class) return FRETURN;
        if (type == double.class) return DRETURN;
        throw new IllegalArgumentException("Unsupported layout: " + type);
    }
}
