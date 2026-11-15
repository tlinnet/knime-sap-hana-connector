package org.knime.database.extension.sap.hana.connector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObject;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.credentials.base.oauth.api.AccessTokenAccessor;
import org.knime.credentials.base.oauth.api.JWTCredential;
import org.knime.database.connection.DBConnectionController;
import org.knime.database.connection.UserDBConnectionController;
import org.knime.database.node.connector.server.UnauthenticatedServerDBConnectorNodeModel2;

@SuppressWarnings("restriction")
final class SAPHANADBConnectorNodeModel
    extends UnauthenticatedServerDBConnectorNodeModel2<SAPHANADBConnectorNodeSettings> {

    private final boolean m_hasCredentialPort;

    SAPHANADBConnectorNodeModel(final PortsConfiguration configuration) {
        super(SAPHANAType.get(), configuration, SAPHANADBConnectorNodeSettings.class);
        m_hasCredentialPort = Arrays.stream(configuration.getInputPorts())
            .anyMatch(CredentialPortObject.TYPE::equals);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (m_hasCredentialPort) {
            setWarningMessage("JWT-authenticated DB connection cannot be restored. Please re-execute the node.");
            return;
        }
        super.loadInternals(nodeInternDir, exec);
    }

    @Override
    protected DBConnectionController createConnectionController(final SAPHANADBConnectorNodeSettings settings)
        throws InvalidSettingsException {
        final var credentials = settings.m_authentication.getCredentials();
        return new UserDBConnectionController(getDBUrl(settings), getAuthenticationType(settings),
            credentials.getUsername(), credentials.getPassword(), null, null, settings.m_host);
    }

    private static AuthenticationType getAuthenticationType(final SAPHANADBConnectorNodeSettings settings) {
        return switch (settings.m_authentication.getType()) {
            case NONE -> AuthenticationType.NONE;
            case USER_PWD -> AuthenticationType.USER_PWD;
            default -> throw new IllegalArgumentException("Unsupported SAP HANA authentication type.");
        };
    }

    private DBConnectionController createConnectionControllerWithInput(final CredentialPortObjectSpec credentialSpec,
        final SAPHANADBConnectorNodeSettings settings) throws InvalidSettingsException {
        final JWTCredential credential;
        try {
            credential = credentialSpec.resolveCredential(JWTCredential.class);
        } catch (NoSuchCredentialException ex) {
            throw new InvalidSettingsException("The Credentials input must provide a JWT access token.");
        }
        return new SAPHANAJWTDBConnectionController((AccessTokenAccessor)credential, getDBUrl(settings));
    }

    @Override
    protected DBConnectionController createConnectionController(final List<PortObject> inputObjects,
        final SAPHANADBConnectorNodeSettings settings, final ExecutionMonitor monitor)
        throws InvalidSettingsException {
        if (!inputObjects.isEmpty() && inputObjects.get(0) instanceof CredentialPortObject credentialPort) {
            return createConnectionControllerWithInput(credentialPort.getSpec(), settings);
        }
        if (!inputObjects.isEmpty()) {
            throw new InvalidSettingsException("The Credentials input must provide a JWT credential.");
        }
        return createConnectionController(settings);
    }

    @Override
    protected void validateSessionInfoSettings(final SAPHANADBConnectorNodeSettings settings)
        throws InvalidSettingsException {
        settings.validateAuthenticationSettings();
    }
}
