package com.bekri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BekriApplicationTest {

    @Test
    void springBootApplicationAnnotationIsPresent() {
        SpringBootApplication annotation = BekriApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation);
    }

    @Test
    void mainMethodIsPublicStaticVoid() throws NoSuchMethodException {
        Method mainMethod = BekriApplication.class.getDeclaredMethod("main", String[].class);

        assertTrue(Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
        assertTrue(Void.TYPE.equals(mainMethod.getReturnType()));
    }
}
