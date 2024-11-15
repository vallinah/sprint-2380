package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// Annotation pour une plage de valeurs
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
    String message() default "This field is out of range";
}


