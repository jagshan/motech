package org.motechproject.mds.config;

import org.motechproject.commons.api.MotechException;
import org.motechproject.commons.sql.service.SqlDBManager;
import org.motechproject.mds.util.Constants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.motechproject.mds.util.Constants.Config.DATANUCLEUS_FILE;

/**
 * Class responsible for handling MDS configuration.
 * Since MDS does not use Server Config, everything connected
 * to the MDS configuration needs to be handled by the
 * module itself.
 */
public class MdsConfig {

    private static final String FLYWAY_MYSQL_MIGRATION_PATH = "db/migration/mysql";
    private static final String FLYWAY_DEFAULT_MIGRATION_PATH = "db/migration/default";

    private Map<String, Properties> config = new HashMap<>();

    private SqlDBManager sqlDBManager;
    private Properties mdsSqlProperties;

    public MdsConfig() {}

    public void init() {
        if (mdsSqlProperties == null) {
            mdsSqlProperties = getProperties("datanucleus.properties");
        }

        //Create database if it doesn't exists
        sqlDBManager.createDatabase(
            mdsSqlProperties.getProperty("javax.jdo.option.ConnectionURL")
        );
    }

    public void setConfig(List<Resource> resources) {
        for (Resource configFile : resources) {
            try (InputStream is = configFile.getInputStream()) {
                Properties props = new Properties();
                props.load(is);

                config.put(getResourceFileName(configFile), props);
            } catch (IOException e) {
                throw new MotechException("Cant load config file " + configFile.getFilename(), e);
            }
        }
    }

    public void setSqlDBManager(SqlDBManager sqlDBManager) {
        this.sqlDBManager = sqlDBManager;
    }

    public void setMdsSqlProperties(Properties mdsSqlProperties) {
        this.mdsSqlProperties = mdsSqlProperties;
    }

    public  String getResourceFileName(Resource resource) {
        String name = resource.getFilename();

        if (resource instanceof ClassPathResource) {
            name = ((ClassPathResource) resource).getPath();
        } else {
            int colonIndex = name.indexOf(':');
            if (colonIndex >= 0) {
                name = name.substring(colonIndex + 1);
            }
        }
        return name;
    }

    public Properties asProperties() {
        Properties result = new Properties();
        for (Properties p : config.values()) {
            result.putAll(p);
        }
        return result;
    }

    public Properties getProperties(String filename) {
        Properties result = config.get(filename);
        return (result == null ? new Properties() : result);
    }

    public Properties getDataNucleusProperties() {
        return getProperties(DATANUCLEUS_FILE);
    }

    public String getFlywayLocations() {
        String driverName = sqlDBManager.getChosenSQLDriver();
        return driverName.equals(Constants.Config.MYSQL_DRIVER_CLASSNAME) ? FLYWAY_MYSQL_MIGRATION_PATH : FLYWAY_DEFAULT_MIGRATION_PATH;
    }

}
