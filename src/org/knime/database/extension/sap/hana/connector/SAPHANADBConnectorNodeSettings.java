package org.knime.database.extension.sap.hana.connector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.database.node.connector.DBConnectorNodeSettingsUtils.DBDialectChoicesProvider;
import org.knime.database.node.connector.DBConnectorNodeSettingsUtils.DBDriverChoicesProvider;
import org.knime.database.node.connector.server.UnauthenticatedServerDBConnectorNodeSettings;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.modification.Modification;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.credentials.AuthenticationFromInPortUtil.AuthenticationManagedByPortMessage;
import org.knime.node.parameters.widget.credentials.AuthenticationFromInPortUtil.HasCredentialPort;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters.AuthenticationType;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.message.TextMessage;

@SuppressWarnings("restriction")
@Modification({SAPHANADBConnectorNodeSettings.ConnectorModification.class,
    SAPHANADBConnectorNodeSettings.DescriptionModification.class,
    SAPHANADBConnectorNodeSettings.ServerLayoutModification.class})
public final class SAPHANADBConnectorNodeSettings extends UnauthenticatedServerDBConnectorNodeSettings {

    private static final int DEFAULT_PORT = 30015;
    static final int JWT_DEFAULT_PORT = 443;

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

    static final class ServerLayoutModification implements Modification.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(DatabaseNameRef.class).removeAnnotation(Widget.class);
            group.find(getPortReference()).removeAnnotation(Advanced.class);
            group.find(getPortReference()).addAnnotation(ValueReference.class).withValue(PortValueRef.class).modify();
            group.find(getPortReference()).addAnnotation(ValueProvider.class)
                .withValue(CredentialPortDefaultPortProvider.class).modify();
        }

        @SuppressWarnings("unchecked")
        private static Class<? extends Modification.Reference> getPortReference() {
            try {
                return (Class<? extends Modification.Reference>)Class
                    .forName("org.knime.database.node.connector.server.UnauthenticatedServerDBConnectorNodeSettings$PortRef");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Unable to locate SAP HANA port widget reference.", ex);
            }
        }
    }

    interface PortValueRef extends ParameterReference<Integer> {
    }

    interface PreviouslyHadCredentialPortRef extends ParameterReference<Boolean> {
    }

    @Widget(title = "Previous Credentials port state", description = "Tracks the previous input-port configuration.")
    @Layout(AuthenticationSection.class)
    @Effect(predicate = AlwaysHidden.class, type = EffectType.HIDE)
    @ValueReference(PreviouslyHadCredentialPortRef.class)
    @ValueProvider(CredentialPortStateProvider.class)
    @Persistor(CredentialPortStatePersistor.class)
    boolean m_previouslyHadCredentialPort;

    static final class AlwaysHidden implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer initializer) {
            return initializer.always();
        }
    }

    static final class CredentialPortStatePersistor implements NodeParametersPersistor<Boolean> {
        private static final String KEY = "previouslyHadCredentialPort";

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.containsKey(KEY) && settings.getBoolean(KEY);
        }

        @Override
        public void save(final Boolean value, final NodeSettingsWO settings) {
            settings.addBoolean(KEY, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{KEY}};
        }
    }

    static final class CredentialPortStateProvider implements StateProvider<Boolean> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        @Override
        public Boolean computeState(final NodeParametersInput context) {
            return hasCredentialPort(context);
        }
    }

    static final class CredentialPortDefaultPortProvider implements StateProvider<Integer> {
        private Supplier<Integer> m_port;
        private Supplier<Boolean> m_previouslyHadCredentialPort;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_port = initializer.getValueSupplier(PortValueRef.class);
            m_previouslyHadCredentialPort = initializer.getValueSupplier(PreviouslyHadCredentialPortRef.class);
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Integer computeState(final NodeParametersInput context) {
            return portForInputConfiguration(m_port.get(), m_previouslyHadCredentialPort.get(),
                hasCredentialPort(context));
        }
    }

    static int portForInputConfiguration(final int port, final boolean previouslyHadCredentialPort,
        final boolean hasCredentialPort) {
        if (hasCredentialPort && !previouslyHadCredentialPort && port == DEFAULT_PORT) {
            return JWT_DEFAULT_PORT;
        }
        if (!hasCredentialPort && previouslyHadCredentialPort && port == JWT_DEFAULT_PORT) {
            return DEFAULT_PORT;
        }
        return port;
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
            .anyMatch(inPortType -> CredentialPortObjectSpec.class
                .isAssignableFrom(inPortType.getPortObjectSpecClass()));
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
