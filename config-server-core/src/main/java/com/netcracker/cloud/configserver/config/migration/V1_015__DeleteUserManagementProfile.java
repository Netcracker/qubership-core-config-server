package com.netcracker.cloud.configserver.config.migration;

import com.netcracker.cloud.configserver.config.ConfigProfile;
import com.netcracker.cloud.configserver.config.SpringUtility;
import com.netcracker.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.util.List;

@Slf4j
@NoArgsConstructor
public class V1_015__DeleteUserManagementProfile extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) {
        List<ConfigProfile> profiles = repository.findByApplicationAndProfile("user-management", "default");
        repository.deleteAll(profiles);
        log.info("Deleted user-management profile");
    }
}
