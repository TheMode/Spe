package io.spe;

import org.jetbrains.annotations.NotNull;

public interface SpeFactory<T> {
    @NotNull T create();
}
