package com.netcracker.cloud.configserver.config.controller;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.InvalidEnvironmentRequestException;
import org.springframework.cloud.config.server.support.PathUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tolerates whitespace in comma-separated profile lists (e.g. {@code default, test})
 * without disabling Spring Cloud Config profile validation globally.
 * <p>
 * Validates a trimmed copy so PathUtils does not return 400, then forwards the
 * original profiles string to the repository (legacy lookup + response shape).
 */
public class WhitespaceTolerantEnvironmentController extends EnvironmentController {

    public WhitespaceTolerantEnvironmentController(EnvironmentRepository repository, JsonMapper mapper) {
        super(repository, mapper);
        setValidateProfiles(false);
    }

    @Override
    public Environment getEnvironment(String name, String profiles, String label, boolean includeOrigin) {
        if (PathUtils.isInvalidProfiles(normalizeProfiles(profiles))) {
            throw new InvalidEnvironmentRequestException("Invalid request");
        }
        return super.getEnvironment(name, profiles, label, includeOrigin);
    }

    static String normalizeProfiles(String profiles) {
        if (profiles == null) {
            return null;
        }
        if (!profiles.contains(",")) {
            return profiles.trim();
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
