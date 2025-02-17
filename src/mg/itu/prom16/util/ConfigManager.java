package mg.itu.prom16.util;

import jakarta.servlet.ServletConfig;

public class ConfigManager {
    private static String authSessionKey;
    private static String roleSessionKey;
    private static String loginUrl;

    public static void init(ServletConfig config) {
        authSessionKey = config.getInitParameter("authSessionKey");
        roleSessionKey = config.getInitParameter("roleSessionKey");
        loginUrl = config.getInitParameter("loginUrl");
        System.out.println("" + authSessionKey + "," + roleSessionKey + "," + loginUrl + "");
    }

    public static String getAuthSessionKey() {
        return authSessionKey;
    }

    public static String getRoleSessionKey() {
        return roleSessionKey;
    }

    public static String getLoginUrl() {
        return loginUrl;
    }
}
