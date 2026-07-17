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
 * EnvironmentController that tolerates whitespace around commas in profile lists
 * (e.g. {@code default, test}) without changing the original profiles string.
 * <p>
 * Spring Cloud Config Server 5.0.3+ rejects such lists with HTTP 400 via
 * {@link PathUtils#isInvalidProfiles(String)}. We validate a whitespace-normalized
 * copy, but pass the <em>original</em> profiles through to the repository and response
 * so existing clients/ITs that assert on {@code ["default, test"]} keep working.
 */
public class WhitespaceTolerantEnvironmentController extends EnvironmentController {

    public WhitespaceTolerantEnvironmentController(EnvironmentRepository repository, JsonMapper mapper) {
        super(repository, mapper);
        // Parent validation is replaced by normalize-then-check in getEnvironment().
        setValidateProfiles(false);
    }

    @Override
    public Environment getEnvironment(String name, String profiles, String label, boolean includeOrigin) {
        if (PathUtils.isInvalidProfiles(normalizeProfiles(profiles))) {
            throw new InvalidEnvironmentRequestException("Invalid request");
        }
        Environment environment = super.getEnvironment(name, profiles, label, includeOrigin);
        // Keep legacy response shape: spaced list stays a single profiles entry as requested.
        if (profiles != null && !profiles.equals(normalizeProfiles(profiles))) {
            environment.setProfiles(new String[]{profiles});
        }
        return environment;
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
