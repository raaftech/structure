package tech.raaf.structure.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The schemas of a message handler. An empty path is not specified.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaPath {
  // The schema of the request args.
  String input() default "";

  // The schema of the result data of a successful response.
  String output() default "";
}
