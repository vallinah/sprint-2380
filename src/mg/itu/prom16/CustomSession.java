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

    // public boolean checkAuthorization(Method method, String error) throws
    // Exception {
    // Auth auth = method.getAnnotation(Auth.class);
    // if (auth == null) {
    // if (method.isAnnotationPresent(Auth.class)) {
    // auth = method.getAnnotation(Auth.class);
    // }
    // }

    // if (auth != null) {
    // String requiredRole = auth.value();
    // System.out.println(
    // "requiredROle : " + requiredRole + " found " +
    // get(ConfigManager.getRoleSessionKey()) + "");

    // if (get(ConfigManager.getAuthSessionKey()) == null) {
    // error = "L'utilisateur doit être authentifié.";
    // return false;
    // }

    // if (!requiredRole.isEmpty() &&
    // !get(ConfigManager.getRoleSessionKey()).equals(requiredRole)) {
    // error = "Accès refusé : rôle insuffisant.Il faut être connecter en tant que "
    // + requiredRole + "";
    // return false;
    // }
    // }
    // return true;
    // }

    public boolean checkAuthorization(Method method, StringBuilder error) throws Exception {
        Auth auth = method.getAnnotation(Auth.class);

        if (auth == null && method.isAnnotationPresent(Auth.class)) {
            auth = method.getAnnotation(Auth.class);
        }

        if (auth != null) {
            String requiredRole = auth.value();

            System.out.println("Required Role: " + requiredRole + " Found: " + get(ConfigManager.getRoleSessionKey()));

            // Vérifier si l'utilisateur est connecté
            if (get(ConfigManager.getAuthSessionKey()) == null) {
                error.append("L'utilisateur doit être authentifié.");
                return false;
            }

            // Si un rôle spécifique est requis, vérifier qu'il correspond
            if (!requiredRole.isEmpty() && !get(ConfigManager.getRoleSessionKey()).equals(requiredRole)) {
                error.append("Accès refusé : rôle insuffisant. Il faut être connecté en tant que " + requiredRole);
                return false;
            }
        }
        return true;
    }

}
