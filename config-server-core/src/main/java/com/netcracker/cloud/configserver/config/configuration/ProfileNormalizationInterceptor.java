package com.netcracker.cloud.configserver.config.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Trims whitespace around comma-separated profile names in the {profiles} path variable
 * before Spring Cloud Config Server 5.0.x validates them via PathUtils.isInvalidProfiles().
 *
 * Spring Cloud Config 5.0.4 added strict character validation that rejects spaces in profile
 * names. The integration test sends "default, test" (space after comma), which previously
 * worked but now triggers a 400 Bad Request. This interceptor normalizes "default, test"
 * to "default,test" so backward-compatible clients continue to work.
 */
public class ProfileNormalizationInterceptor implements HandlerInterceptor {

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> vars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null || !vars.containsKey("profiles")) {
            return true;
        }
        String raw = vars.get("profiles");
        String normalized = normalizeProfiles(raw);
        if (!normalized.equals(raw)) {
            Map<String, String> mutable = new HashMap<>(vars);
            mutable.put("profiles", normalized);
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mutable);
        }
        return true;
    }

    static String normalizeProfiles(String profiles) {
        if (profiles == null || !profiles.contains(",")) {
            return profiles == null ? null : profiles.trim();
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
