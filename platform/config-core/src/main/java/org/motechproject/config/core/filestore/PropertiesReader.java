package org.motechproject.config.core.filestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A utility class for loading properties.
 */
public final class PropertiesReader {

    /**
     * Loads the properties from given {@code File}.
     *
     * @param file  the file with properties
     * @return the loaded properties
     * @throws IOException if I/O error occurred
     */
    public static Properties getPropertiesFromFile(File file) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            properties.load(fileInputStream);
        }
        return properties;
    }

    /**
     * Loads the properties from given {@code String}.
     * The format of this {@code String} should be "key1=value1;key2=value2;key3=value3;...".
     *
     * @param string  the string with properties
     * @return the loaded properties
     */
    public static Properties getPropertiesFromString(String string) {
        if (string == null) {
            return null;
        }

        Properties properties = new Properties();
        String stringToParse = string;

        // The properties should look like "key1=value1;key2=value2;key3=value3;..."
        while (!stringToParse.isEmpty()) {
            String property;
            int endOfProperty = stringToParse.indexOf(';');

            if (endOfProperty >= 0) {
                property = stringToParse.substring(0, endOfProperty);
            } else {
                property = stringToParse;
                endOfProperty = stringToParse.length() - 1;
            }

            int endOfKey = property.indexOf('=');

            String key = property.substring(0, endOfKey).trim();
            String value = property.substring(endOfKey + 1).trim();
            stringToParse = stringToParse.substring(endOfProperty + 1);

            properties.setProperty(key, value);
        }
        return properties.isEmpty() ? null : properties;
    }

    /**
     * This is an utility class and should not be initiated.
     */
    private PropertiesReader() {
    }
}
