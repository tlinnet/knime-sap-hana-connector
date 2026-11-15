package org.knime.database.extension.sap.hana.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_APPEND_JDBC_INITIAL_PARAMETER_SEPARATOR;
import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_APPEND_JDBC_LAST_PARAMETER_SUFFIX;
import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_APPEND_JDBC_PARAMETER_SEPARATOR;
import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_APPEND_JDBC_PARAMETER_TO_URL;
import static org.knime.database.connection.DBConnectionManagerAttributes.ATTRIBUTE_JDBC_PROPERTIES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.credentials.base.oauth.api.AccessTokenAccessor;
import org.knime.database.VariableContext;
import org.knime.database.attribute.AttributeValueRepository;
import org.knime.database.connection.AbstractConnectionProvider;
import org.knime.database.connection.DBConnectionController;
import org.knime.database.util.DerivableProperties;
import org.knime.database.util.DerivableProperties.ValueType;
import org.mockito.ArgumentCaptor;

class SAPHANAJWTConnectionTest {

    @Test
    void jwtIsSuppliedToDriverThroughProperties() throws Exception {
        verifyConnection(false);
    }

    @Test
    void jwtStaysOutOfUrlWhenOtherParametersAreAppended() throws Exception {
        verifyConnection(true);
    }

    private static void verifyConnection(final boolean appendToUrl) throws Exception {
        final AccessTokenAccessor accessor = mock(AccessTokenAccessor.class);
        when(accessor.getAccessToken()).thenReturn("encoded.jwt.token");
        final var controller = new SAPHANAJWTDBConnectionController(accessor, "jdbc:sap://host:30015");
        final AttributeValueRepository attributes = mock(AttributeValueRepository.class);
        final DerivableProperties configuredProperties = new DerivableProperties();
        configuredProperties.setDerivableProperty("encrypt", ValueType.LITERAL, "true");
        when(attributes.get(ATTRIBUTE_JDBC_PROPERTIES)).thenReturn(configuredProperties);
        when(attributes.get(ATTRIBUTE_APPEND_JDBC_PARAMETER_TO_URL)).thenReturn(appendToUrl);
        when(attributes.get(ATTRIBUTE_APPEND_JDBC_INITIAL_PARAMETER_SEPARATOR)).thenReturn("?");
        when(attributes.get(ATTRIBUTE_APPEND_JDBC_PARAMETER_SEPARATOR)).thenReturn("&");
        when(attributes.get(ATTRIBUTE_APPEND_JDBC_LAST_PARAMETER_SUFFIX)).thenReturn("");

        final Driver driver = mock(Driver.class);
        final Connection connection = mock(Connection.class);
        when(driver.acceptsURL(anyString())).thenReturn(true);
        when(driver.connect(anyString(), any(Properties.class))).thenReturn(connection);
        final ExecutionMonitor monitor = mock(ExecutionMonitor.class);
        assertNotNull(TestConnectionProvider.open(controller, attributes, driver, mock(VariableContext.class), monitor));

        final ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Properties> properties = ArgumentCaptor.forClass(Properties.class);
        verify(driver).connect(url.capture(), properties.capture());
        assertFalse(url.getValue().contains("encoded.jwt.token"));
        assertEquals("", properties.getValue().getProperty("user"));
        assertEquals("encoded.jwt.token", properties.getValue().getProperty("password"));
        if (appendToUrl) {
            assertEquals("jdbc:sap://host:30015?encrypt=true", url.getValue());
        } else {
            assertEquals("jdbc:sap://host:30015", url.getValue());
            assertEquals("true", properties.getValue().getProperty("encrypt"));
        }
    }

    @Test
    void driverExceptionDoesNotExposeToken() throws Exception {
        final AccessTokenAccessor accessor = mock(AccessTokenAccessor.class);
        when(accessor.getAccessToken()).thenReturn("encoded.jwt.token");
        final var controller = new SAPHANAJWTDBConnectionController(accessor, "jdbc:sap://host:30015");
        final AttributeValueRepository attributes = mock(AttributeValueRepository.class);
        when(attributes.get(ATTRIBUTE_JDBC_PROPERTIES)).thenReturn(new DerivableProperties());
        when(attributes.get(ATTRIBUTE_APPEND_JDBC_PARAMETER_TO_URL)).thenReturn(false);
        final Driver driver = mock(Driver.class);
        when(driver.acceptsURL(anyString())).thenReturn(true);
        when(driver.connect(anyString(), any(Properties.class))).thenThrow(new SQLException("encoded.jwt.token"));
        final ExecutionMonitor monitor = mock(ExecutionMonitor.class);
        final SQLException error = assertThrows(SQLException.class,
            () -> TestConnectionProvider.open(controller, attributes, driver, mock(VariableContext.class), monitor));
        assertFalse(error.toString().contains("encoded.jwt.token"));
        assertEquals(null, error.getCause());
    }

    private abstract static class TestConnectionProvider extends AbstractConnectionProvider {
        static Connection open(final DBConnectionController controller, final AttributeValueRepository attributes,
            final Driver driver, final VariableContext context, final ExecutionMonitor monitor)
            throws CanceledExecutionException, SQLException {
            return createConnection(controller, attributes, driver, context, monitor);
        }
    }
}
