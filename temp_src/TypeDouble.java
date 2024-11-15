package mg.itu.prom16.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation pour les valeurs décimales
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeDouble {
    String message() default "This field must be a decimal";
}

