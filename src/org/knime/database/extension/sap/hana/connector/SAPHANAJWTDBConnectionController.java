package org.knime.database.extension.sap.hana.connector;

import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_APPEND_JDBC_PARAMETER_TO_URL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.credentials.base.oauth.api.AccessTokenAccessor;
import org.knime.database.VariableContext;
import org.knime.database.attribute.AttributeValueRepository;
import org.knime.database.connection.UrlDBConnectionController;

final class SAPHANAJWTDBConnectionController extends UrlDBConnectionController {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SAPHANAJWTDBConnectionController.class);

    private final AccessTokenAccessor m_tokenAccessor;

    SAPHANAJWTDBConnectionController(final AccessTokenAccessor tokenAccessor, final String jdbcUrl) {
        super(jdbcUrl);
        m_tokenAccessor = tokenAccessor;
    }

    SAPHANAJWTDBConnectionController(final NodeSettingsRO settings) throws InvalidSettingsException {
        super(settings);
        throw new InvalidSettingsException(
            "Restoring this database connection is not supported. Please re-execute the node.");
    }

    @Override
    protected Connection createConnection(final AttributeValueRepository attributeValues, final Driver driver,
        final VariableContext variableContext, final ExecutionMonitor monitor)
        throws CanceledExecutionException, SQLException {
        final Properties preparedProperties = prepareJdbcProperties(attributeValues, variableContext, monitor);
        final String jdbcUrl;
        final Properties driverProperties;
        if (attributeValues.get(ATTRIBUTE_APPEND_JDBC_PARAMETER_TO_URL)) {
            jdbcUrl = appendPropertiesToURL(attributeValues, getJdbcUrl(),
                copyPropertiesWithoutCredentials(preparedProperties));
            driverProperties = new Properties();
            driverProperties.setProperty("user", preparedProperties.getProperty("user"));
            driverProperties.setProperty("password", preparedProperties.getProperty("password"));
        } else {
            jdbcUrl = getJdbcUrl();
            driverProperties = preparedProperties;
        }
        if (!(driver instanceof ControlledDriver controlledDriver)) {
            throw new SQLException("The SAP HANA JDBC driver was not initialized by the database connection manager.");
        }
        controlledDriver.setConnectionJdbcUrl(null);
        controlledDriver.setConnectionJdbcProperties(null);
        if (!driver.acceptsURL(jdbcUrl)) {
            throw new SQLException("Selected JDBC driver does not support the configured SAP HANA JDBC URL.");
        }
        try {
            final Connection connection = controlledDriver.getDriver().connect(jdbcUrl, driverProperties);
            if (connection == null) {
                throw new SQLException("The selected SAP HANA JDBC driver did not create a connection.");
            }
            controlledDriver.setConnectionJdbcUrl(jdbcUrl);
            controlledDriver.setConnectionJdbcProperties(driverProperties);
            return connection;
        } catch (SQLException ex) {
            throw new SQLException("SAP HANA connection failed. Check the JDBC driver, URL, and authentication.",
                ex.getSQLState(), ex.getErrorCode());
        }
    }

    @Override
    protected Properties prepareJdbcProperties(final AttributeValueRepository attributeValues,
        final VariableContext variableContext, final ExecutionMonitor monitor)
        throws CanceledExecutionException, SQLException {
        final Properties properties =
            getDerivableJdbcProperties(attributeValues).getDerivedProperties(variableContext);
        final String token;
        try {
            token = m_tokenAccessor.getAccessToken();
        } catch (IOException ex) {
            throw new SQLException("Unable to retrieve a JWT access token from the Credentials input.", ex);
        }
        if (hasProperty(properties, "user")) {
            LOGGER.warn("The JDBC user parameter is overridden by the JWT Credentials input.");
        }
        if (hasProperty(properties, "password")) {
            LOGGER.warn("The JDBC password parameter is overridden by the JWT Credentials input.");
        }
        applyJwtCredentials(properties, token);
        monitor.checkCanceled();
        return properties;
    }

    static void applyJwtCredentials(final Properties properties, final String token) throws SQLException {
        if (token == null || token.isBlank()) {
            throw new SQLException("The connected JWT credential did not provide an access token.");
        }
        properties.stringPropertyNames().stream().filter(SAPHANAJWTDBConnectionController::isCredentialProperty)
            .toList().forEach(properties::remove);
        properties.setProperty("user", "");
        properties.setProperty("password", token);
    }

    static Properties copyPropertiesWithoutCredentials(final Properties properties) {
        final Properties safeProperties = new Properties();
        safeProperties.putAll(properties);
        safeProperties.stringPropertyNames().stream()
            .filter(SAPHANAJWTDBConnectionController::isCredentialProperty)
            .toList().forEach(safeProperties::remove);
        return safeProperties;
    }

    private static boolean hasProperty(final Properties properties, final String name) {
        return properties.stringPropertyNames().stream()
            .anyMatch(property -> name.equalsIgnoreCase(property));
    }

    private static boolean isCredentialProperty(final String name) {
        return "user".equalsIgnoreCase(name) || "password".equalsIgnoreCase(name);
    }
}
