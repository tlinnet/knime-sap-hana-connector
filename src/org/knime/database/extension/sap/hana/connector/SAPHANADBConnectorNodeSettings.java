package org.knime.database.extension.sap.hana.connector;

import java.util.Arrays;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.database.node.connector.DBConnectorNodeSettingsUtils.DBDialectChoicesProvider;
import org.knime.database.node.connector.DBConnectorNodeSettingsUtils.DBDriverChoicesProvider;
import org.knime.database.node.connector.server.UnauthenticatedServerDBConnectorNodeSettings;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.modification.Modification;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.credentials.AuthenticationFromInPortUtil.AuthenticationManagedByPortMessage;
import org.knime.node.parameters.widget.credentials.AuthenticationFromInPortUtil.HasCredentialPort;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters.AuthenticationType;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.message.TextMessage;

@SuppressWarnings("restriction")
@Modification({SAPHANADBConnectorNodeSettings.ConnectorModification.class,
    SAPHANADBConnectorNodeSettings.DescriptionModification.class})
public final class SAPHANADBConnectorNodeSettings extends UnauthenticatedServerDBConnectorNodeSettings {

    private static final int DEFAULT_PORT = 30015;

    public SAPHANADBConnectorNodeSettings() {
        super(SAPHANAType.get(), DEFAULT_PORT);
    }

    static final class ConnectorModification extends ChoicesProviderModification {

        @Override
        protected Class<? extends DBDialectChoicesProvider> getDialectProvider() {
            return SAPHANADBDialectChoicesProvider.class;
        }

        @Override
        protected Class<? extends DBDriverChoicesProvider> getDriverProvider() {
            return SAPHANADBDriverChoicesProvider.class;
        }

        @Override
        protected String getJDBCDescription() {
            return """
                    Define SAP HANA JDBC driver parameters here. The value of a parameter can be a constant, \
                    credential user, credential password or KNIME URL. See the SAP HANA Client Interface \
                    Programming Reference for driver-specific properties.
                    """;
        }
    }

    static final class SAPHANADBDialectChoicesProvider extends DBDialectChoicesProvider {
        SAPHANADBDialectChoicesProvider() {
            super(SAPHANAType.get(), true);
        }
    }

    static final class SAPHANADBDriverChoicesProvider extends DBDriverChoicesProvider {
        SAPHANADBDriverChoicesProvider() {
            super(SAPHANAType.get());
        }
    }

    static final class DescriptionModification extends ServerSettingsDescriptionModification {
        @Override
        protected String getDBTypeName() {
            return SAPHANAType.get().getName();
        }

        @Override
        protected int getDefaultPort() {
            return DEFAULT_PORT;
        }
    }

    @Layout(AuthenticationSection.class)
    @Modification(ChangeAvailableAuthenticationModification.class)
    @Effect(predicate = HasCredentialPortPredicate.class, type = EffectType.HIDE)
    AuthenticationParameters m_authentication =
        new AuthenticationParameters(AuthenticationType.USER_PWD, new Credentials());

    @TextMessage(AuthByCredentialsPortMessage.class)
    @Layout(AuthenticationSection.class)
    Void m_authenticationFromInPortInfo;

    static final class AuthByCredentialsPortMessage extends AuthenticationManagedByPortMessage {
        @Override
        protected boolean hasCredentialPort(final NodeParametersInput input) {
            return SAPHANADBConnectorNodeSettings.hasCredentialPort(input);
        }
    }

    static final class HasCredentialPortPredicate extends HasCredentialPort {
        @Override
        protected boolean hasCredentialPort(final NodeParametersInput input) {
            return SAPHANADBConnectorNodeSettings.hasCredentialPort(input);
        }
    }

    private static boolean hasCredentialPort(final NodeParametersInput input) {
        return Arrays.stream(input.getInPortTypes())
            .anyMatch(inPortType -> CredentialPortObjectSpec.class.equals(inPortType.getPortObjectSpecClass()));
    }

    static final class ChangeAvailableAuthenticationModification extends DBAuthenticationTypeModification {
        @Override
        protected Class<? extends EnumChoicesProvider<AuthenticationType>> getAuthenticationTypeChoicesProvider() {
            return AvailableAuthenticationTypesProvider.class;
        }
    }

    static final class AvailableAuthenticationTypesProvider implements EnumChoicesProvider<AuthenticationType> {
        private static final List<AuthenticationType> TYPES = List.of(AuthenticationType.NONE,
            AuthenticationType.USER_PWD, AuthenticationType.KERBEROS);

        @Override
        public List<AuthenticationType> choices(final NodeParametersInput context) {
            return TYPES;
        }
    }

    void validateAuthenticationSettings() throws InvalidSettingsException {
        validateAuthType(m_authentication.getType(), AvailableAuthenticationTypesProvider.TYPES);
    }
}
