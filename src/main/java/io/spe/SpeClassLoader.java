package io.spe;

final class SpeClassLoader extends ClassLoader {
    Class<?> defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }
}
