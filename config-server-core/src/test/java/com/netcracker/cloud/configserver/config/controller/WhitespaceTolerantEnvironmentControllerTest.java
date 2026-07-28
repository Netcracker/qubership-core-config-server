package com.netcracker.cloud.configserver.config.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.InvalidEnvironmentRequestException;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhitespaceTolerantEnvironmentControllerTest {

    private final EnvironmentRepository repository = (application, profile, label) ->
            new Environment(application, StringUtils.commaDelimitedListToStringArray(profile));

    private final WhitespaceTolerantEnvironmentController controller =
            new WhitespaceTolerantEnvironmentController(repository, new JsonMapper());

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "default|default",
        "default,test|default,test",
        "default, test|default,test",
        "default , test|default,test",
    })
    void normalizeProfiles(String input, String expected) {
        assertThat(WhitespaceTolerantEnvironmentController.normalizeProfiles(input)).isEqualTo(expected);
    }

    @Test
    void getEnvironment_acceptsSpacedProfiles_andPassesOriginalToRepository() {
        Environment result = controller.getEnvironment("testApp", "default, test", null, false);

        // Repository still receives original string → split keeps leading space on " test"
        assertThat(result.getProfiles()).containsExactly("default", " test");
    }

    @Test
    void getEnvironment_rejectsInvalidProfiles() {
        assertThatThrownBy(() -> controller.getEnvironment("testApp", "!!!", null, false))
                .isInstanceOf(InvalidEnvironmentRequestException.class);
    }
}
