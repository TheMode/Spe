package io.spe;

import org.jetbrains.annotations.NotNull;

public sealed interface SpeValue permits SpeValue.Int {
    static @NotNull Int Integer(int value) {
        return new SpeValueImpl.Int(value);
    }

    non-sealed interface Int extends SpeValue {
        int get();
    }
}
