package org.knime.database.extension.sap.hana.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class SAPHANAJWTDBConnectionControllerTest {

    @Test
    void jwtIsPassedAsPasswordAndUsernameIsBlank() throws SQLException {
        final Properties properties = new Properties();
        properties.setProperty("user", "configured-user");
        properties.setProperty("PASSWORD", "configured-password");

        SAPHANAJWTDBConnectionController.applyJwtCredentials(properties, "encoded.jwt.token");

        assertEquals("", properties.getProperty("user"));
        assertEquals("encoded.jwt.token", properties.getProperty("password"));
    }

    @Test
    void missingTokenIsRejected() {
        assertThrows(SQLException.class,
            () -> SAPHANAJWTDBConnectionController.applyJwtCredentials(new Properties(), " "));
    }

    @Test
    void jwtCredentialsAreNotAppendedToJdbcUrlParameters() {
        final Properties properties = new Properties();
        properties.setProperty("user", "");
        properties.setProperty("password", "encoded.jwt.token");
        properties.setProperty("USER", "wrong-case-user");
        properties.setProperty("PASSWORD", "wrong-case-password");
        properties.setProperty("encrypt", "true");

        final Properties urlProperties =
            SAPHANAJWTDBConnectionController.copyPropertiesWithoutCredentials(properties);

        assertFalse(urlProperties.containsKey("user"));
        assertFalse(urlProperties.containsKey("password"));
        assertFalse(urlProperties.containsKey("USER"));
        assertFalse(urlProperties.containsKey("PASSWORD"));
        assertEquals("true", urlProperties.getProperty("encrypt"));
    }
}
