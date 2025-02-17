package mg.itu.prom16.util;

import java.util.HashMap;
import java.util.Map;

public class ValidationValue {
    private Map<String, String> value = new HashMap<>();

    public void addValue(String fieldName, String errorMessage) {
        value.put(fieldName, errorMessage);
    }

    public boolean hasValue() {
        return !value.isEmpty();
    }

    public String getError(String fieldName) {
        return value.get(fieldName);
    }

    public Map<String, String> getAllValue() {
        return value;
    }
}
