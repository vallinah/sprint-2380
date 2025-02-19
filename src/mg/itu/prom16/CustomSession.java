package mg.itu.prom16;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpSession;
import mg.itu.prom16.annotations.Auth;
import mg.itu.prom16.annotations.OnErrorValidation;
import mg.itu.prom16.util.ConfigManager;

public class CustomSession {
    private HttpSession session;

    public CustomSession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        return this.session.getAttribute(key);
    }

    public void add(String key, Object objet) {
        this.session.setAttribute(key, objet);
    }

    public void delete(String key) {
        this.session.removeAttribute(key);
    }

    public boolean checkAuthorization(Class<?> controllerClass, Method method, StringBuilder error) throws Exception {
        Auth auth = method.getAnnotation(Auth.class);
        System.out.println("class: " + controllerClass + ",method:" + method + "");

        if (auth == null) {
            if (controllerClass.isAnnotationPresent(Auth.class)) {
                System.out.println("controllerClass as auth");
                auth = controllerClass.getAnnotation(Auth.class);
            }
            if (method.isAnnotationPresent(Auth.class)) {
                System.out.println("method as auth");
                auth = method.getAnnotation(Auth.class);
            }
        }

        if (auth != null) {
            String requiredRole = auth.value();

            System.out.println("Required Role: " + requiredRole + " Found: " + get(ConfigManager.getRoleSessionKey()));

            // Vérifier si l'utilisateur est connecté
            if (get(ConfigManager.getAuthSessionKey()) == null) {
                System.out.println("L'utilisateur doit être authentifié.");
                error.append("L'utilisateur doit être authentifié.");
                return false;
            }

            // Si un rôle spécifique est requis, vérifier qu'il correspond
            if (!requiredRole.isEmpty() && !get(ConfigManager.getRoleSessionKey()).equals(requiredRole)) {
                System.out
                        .println("Accès refusé : rôle insuffisant. Il faut être connecté en tant que " + requiredRole);
                error.append("Accès refusé : rôle insuffisant. Il faut être connecté en tant que " + requiredRole);
                return false;
            }
        }
        return true;
    }

}
