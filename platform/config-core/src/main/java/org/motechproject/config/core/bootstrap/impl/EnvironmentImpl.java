package org.motechproject.config.core.bootstrap.impl;

import org.motechproject.config.core.bootstrap.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentImpl implements Environment {
    @Override
    public String getConfigDir() {
        return getValue(MOTECH_CONFIG_DIR);
    }

    @Override
    public String getSqlUrl() {
        return getValue(MOTECH_SQL_URL);
    }

    @Override
    public String getSqlUsername() {
        return getValue(MOTECH_SQL_USERNAME);
    }

    @Override
    public String getSqlPassword() {
        return getValue(MOTECH_SQL_PASSWORD);
    }

    @Override
    public String getTenantId() {
        return getValue(MOTECH_TENANT_ID);
    }

    @Override
    public String getConfigSource() {
        return getValue(MOTECH_CONFIG_SOURCE);
    }

    @Override
    public String getSqlDriver() {
        return getValue(MOTECH_SQL_DRIVER);
    }

    @Override
    public String getOsgiFrameworkStorage() {
        return getValue(MOTECH_OSGI_FRAMEWORK_STORAGE);
    }

    @Override
    public String getQueueUrl() {
        return getValue(MOTECH_QUEUE_URL);
    }

    @Override
    public String getActiveMqProperties() {
        return getValue(MOTECH_ACTIVEMQ_PROPERTIES);
    }

    public String getValue(String variableName) {
        return System.getenv(variableName);
    }
}
