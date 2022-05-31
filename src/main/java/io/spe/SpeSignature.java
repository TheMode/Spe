package io.spe;

import java.lang.foreign.MemoryLayout;
import java.util.Map;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.util.Map.entry;

final class SpeSignature {
    private static final Map<Class<?>, MemoryLayout> BASICS = Map.ofEntries(
            entry(int.class, JAVA_INT), entry(long.class, JAVA_LONG));

    static MemoryLayout toLayout(Class<?> type) {
        return BASICS.get(type);
    }
}
