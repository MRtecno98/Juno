package dev.mrtecno.juno.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Manifest {
	String name();
	String version() default "";
	String[] dependencies() default {};
	int major() default 0;
	int minor() default 0;
	int patch() default 0;
}
