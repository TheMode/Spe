package io.spe.api;

import io.spe.Spe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Test
@ExtendWith(SpeTest.InitExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpeTest {
    class InitExtension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            Spe.init();
        }
    }
}
