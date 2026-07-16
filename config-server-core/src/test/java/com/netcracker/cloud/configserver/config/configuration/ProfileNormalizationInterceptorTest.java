package com.netcracker.cloud.configserver.config.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileNormalizationInterceptorTest {

    private final ProfileNormalizationInterceptor interceptor = new ProfileNormalizationInterceptor();

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "default|default",                 // single profile — unchanged
        "default,test|default,test",       // no spaces — unchanged
        "default, test|default,test",      // space after comma — trimmed
        "default , test|default,test",     // space before and after comma — trimmed
        " default , test |default,test",   // leading/trailing spaces — trimmed
        "a, b, c|a,b,c",                   // three profiles — all trimmed
        "p1,  p2,   p3|p1,p2,p3",          // multiple spaces — trimmed
    })
    void normalizeProfiles(String input, String expected) {
        assertThat(ProfileNormalizationInterceptor.normalizeProfiles(input)).isEqualTo(expected);
    }

    @Test
    void preHandle_modifiesProfilesAttribute_whenSpacePresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/testApp/default, test");
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "testApp");
        vars.put("profiles", "default, test");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, vars);

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, String> updated = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        assertThat(updated.get("profiles")).isEqualTo("default,test");
        assertThat(updated.get("name")).isEqualTo("testApp");
    }

    @Test
    void preHandle_doesNotModify_whenNoSpaces() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/testApp/default,test");
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "testApp");
        vars.put("profiles", "default,test");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, vars);
        Object originalVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .isSameAs(originalVars);
    }

    @Test
    void preHandle_doesNotModify_whenNoProfilesVar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/testApp");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, new HashMap<>());

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_doesNotFail_whenNoVarsAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/testApp/default");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }
}
