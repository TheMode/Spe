package io.spe.api;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Test
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpeTest {
}
