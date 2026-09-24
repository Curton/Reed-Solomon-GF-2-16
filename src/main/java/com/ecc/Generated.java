package com.ecc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as non-production code (demo / self-test entry point).
 * JaCoCo (0.8.2+) automatically excludes methods carrying an annotation whose
 * simple name is {@code Generated} from coverage reports, so the demo does not
 * dilute the codec library's coverage numbers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Generated {
}
