package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE }) // Peut être appliqué aux classes et méthodes
@Retention(RetentionPolicy.RUNTIME) // Accessible à l'exécution
public @interface Auth {
    String value() default ""; // Rôle requis ("admin", "user", etc.)
}
