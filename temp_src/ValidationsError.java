package mg.itu.prom16.util;

import java.util.HashMap;
import java.util.Map;

public class ValidationsError {
    private Map<String, String> errors = new HashMap<>();

    public void addError(String fieldName, String errorMessage) {
        errors.put(fieldName, errorMessage);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

    public Map<String, String> getAllErrors() {
        return errors;
    }
}
