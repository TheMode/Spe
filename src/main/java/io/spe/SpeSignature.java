package io.spe;

import java.lang.foreign.MemoryLayout;
import java.util.Map;

import static java.lang.foreign.ValueLayout.*;
import static java.util.Map.entry;

final class SpeSignature {
    private static final Map<Class<?>, MemoryLayout> BASICS = Map.ofEntries(
            entry(boolean.class, JAVA_BOOLEAN), entry(byte.class, JAVA_BYTE),
            entry(char.class, JAVA_CHAR), entry(short.class, JAVA_SHORT),
            entry(int.class, JAVA_INT), entry(long.class, JAVA_LONG),
            entry(float.class, JAVA_FLOAT), entry(double.class, JAVA_DOUBLE));

    static MemoryLayout toLayout(Class<?> type) {
        final MemoryLayout layout = BASICS.get(type);
        if (layout == null) throw new IllegalArgumentException("Unsupported type: " + type);
        return layout;
    }
}
