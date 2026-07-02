package ch.ensnert.api.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Add this Annotation to "Records Construction Parameter", or for non records: to "Getters" and "Consturctor Parameter"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD })
public @interface Column
{
	String value();

	int id() default -1;

	@SuppressWarnings({"unused", "UnusedReturnValue"}) // reason: future proof
	boolean index() default false;
}
