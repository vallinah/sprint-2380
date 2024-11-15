package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation pour les champs requis
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
    String message() default "This field is required";
}