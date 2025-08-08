package com.netcracker.cloud.configserver.config.migration;

import com.netcracker.cloud.configserver.config.ConfigProfile;
import com.netcracker.cloud.configserver.config.ConfigProperty;
import com.netcracker.cloud.configserver.config.SpringUtility;
import com.netcracker.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@NoArgsConstructor
public class V1_008__ConfigServerRepository extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        ConfigProfile tenantActivatorDefaultProfile = ConfigProfile.builder()
                .application("dmp-tenant-activator")
                .profile("default")
                .property(new ConfigProperty("tenant.shoppingFrontend.templateName", "qubership-cloud-shopping-frontend", false))
                .build();
        repository.save(tenantActivatorDefaultProfile);

    }
}
