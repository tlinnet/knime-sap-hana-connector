package org.knime.database.extension.sap.hana.connector;

import java.sql.SQLException;
import java.util.Properties;

final class JWTJdbcProperties {

    private JWTJdbcProperties() {
    }

    static void apply(final Properties properties, final String token) throws SQLException {
        if (token == null || token.isBlank()) {
            throw new SQLException("The connected JWT credential did not provide an access token.");
        }
        properties.stringPropertyNames().stream().filter(JWTJdbcProperties::isCredentialProperty)
            .toList().forEach(properties::remove);
        properties.setProperty("user", "");
        properties.setProperty("password", token);
    }

    static Properties withoutCredentials(final Properties properties) {
        final Properties safeProperties = new Properties();
        safeProperties.putAll(properties);
        safeProperties.stringPropertyNames().stream().filter(JWTJdbcProperties::isCredentialProperty)
            .toList().forEach(safeProperties::remove);
        return safeProperties;
    }

    private static boolean isCredentialProperty(final String name) {
        return "user".equalsIgnoreCase(name) || "password".equalsIgnoreCase(name);
    }
}
