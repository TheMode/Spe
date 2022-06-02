package io.spe;

import org.jetbrains.annotations.NotNull;

public interface SpeAccess {
    <T> @NotNull T compileAndCreate(@NotNull Class<T> interfaceType, @NotNull Class<? extends T> implementationType);

    <T> @NotNull SpeFactory<T> compile(@NotNull Class<T> interfaceType, @NotNull Class<? extends T> implementationType);

    void free();
}
