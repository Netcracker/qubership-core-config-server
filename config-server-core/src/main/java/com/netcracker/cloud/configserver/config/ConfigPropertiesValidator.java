package com.netcracker.cloud.configserver.config;

import com.netcracker.cloud.configserver.config.service.ConsulService;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * This class is used for config properties validation,
 * which is performed before saving properties to database.
 * Validation result is returned as {@link ValidationResult}
 * instance and can be used for detailed error message.
 */
public class ConfigPropertiesValidator {

    private static final String CONFLICT_WITH = "%s conflicts with %s";
    private static final String START_WITH_DOT = "%s start with dot";

    public ValidationResult validate(Map<String, ConfigProperty> source) {
        ValidationResult validationResult = new ValidationResult();
        validateDotProperty(source, validationResult);
        validateConflictProperties(source, validationResult);

        if (validationResult.getConflictingProperties().isEmpty() && validationResult.getDotProperties().isEmpty()) {
            validationResult.valid();
        }
        return validationResult;
    }

    private void validateConflictProperties(Map<String, ConfigProperty> source, ValidationResult validationResult) {
        // encode each key once: the comparison below is O(n^2)
        Map<String, String> paths = new HashMap<>();
        for (String key : source.keySet()) {
            paths.put(key, ConsulService.toConsulRelativeKey(key));
        }
        for (String key : source.keySet()) {
            for (String anotherKey : source.keySet()) {
                if (pathsConflict(paths.get(key), paths.get(anotherKey))) {
                    if (!validationResult.getConflictingProperties().contains(String.format(CONFLICT_WITH, anotherKey, key))) {
                        validationResult.getConflictingProperties().add(String.format(CONFLICT_WITH, key, anotherKey));
                    }
                }
            }
        }
    }

    /** Keys conflict when one is a Consul "folder" of the other, so the stored paths are compared. */
    private boolean pathsConflict(String path1, String path2) {
        if (path1.length() == path2.length()) return false;
        String longerPath, shorterPath;
        if (path1.length() > path2.length()) {
            longerPath = path1;
            shorterPath = path2;
        } else {
            longerPath = path2;
            shorterPath = path1;
        }
        return longerPath.startsWith(shorterPath) && longerPath.charAt(shorterPath.length()) == '/';
    }

    private void validateDotProperty(Map<String, ConfigProperty> mapOfConfigProperties, ValidationResult validationResult) {

        mapOfConfigProperties.keySet()
                .stream()
                .filter(key -> key.startsWith("."))
                .forEach(key -> validationResult.getDotProperties().add(String.format(START_WITH_DOT, key)));
    }

    /**
     * Represents config properties validation results.
     * Instance of this class contains conflicting properties keys,
     * which can be used in error message details.
     */
    @NoArgsConstructor
    public static class ValidationResult {
        @Getter
        private boolean valid = false;
        @Getter
        private Set<String> conflictingProperties = new HashSet<>();
        @Getter
        private Set<String> dotProperties = new HashSet<>();

        public void valid() {
            valid = true;
        }
    }
}
