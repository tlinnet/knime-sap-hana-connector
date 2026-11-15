package org.knime.database.extension.sap.hana.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider.StateProviderInitializer;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters.AuthenticationType;

class SAPHANADBConnectorNodeSettingsTest {

    @Test
    void kerberosIsOfferedAsConfiguredAuthenticationType() {
        final var provider = new SAPHANADBConnectorNodeSettings.AvailableAuthenticationTypesProvider();

        assertEquals(List.of(AuthenticationType.NONE, AuthenticationType.USER_PWD, AuthenticationType.KERBEROS),
            provider.choices(null));
    }

    @Test
    void portProviderIsTriggeredBeforeDialogOpens() {
        final var provider = new SAPHANADBConnectorNodeSettings.CredentialPortDefaultPortProvider();
        final StateProviderInitializer initializer = mock(StateProviderInitializer.class);
        provider.init(initializer);

        verify(initializer).computeBeforeOpenDialog();
        verify(initializer).getValueSupplier(SAPHANADBConnectorNodeSettings.PortValueRef.class);
        verify(initializer).getValueSupplier(SAPHANADBConnectorNodeSettings.PreviouslyHadCredentialPortRef.class);
    }

    @Test
    void providerUsesPreviousConfigurationAndCurrentPort() {
        final NodeParametersInput input = mock(NodeParametersInput.class);
        when(input.getInPortTypes()).thenReturn(new PortType[]{CredentialPortObject.TYPE});
        final StateProviderInitializer initializer = mock(StateProviderInitializer.class);
        when(initializer.getValueSupplier(SAPHANADBConnectorNodeSettings.PortValueRef.class))
            .thenReturn(() -> 30015);
        when(initializer.getValueSupplier(SAPHANADBConnectorNodeSettings.PreviouslyHadCredentialPortRef.class))
            .thenReturn(() -> false);
        final var provider = new SAPHANADBConnectorNodeSettings.CredentialPortDefaultPortProvider();
        provider.init(initializer);

        assertEquals(443, provider.computeState(input));
    }

    @Test
    void credentialPortStateIsUpdatedAfterDialogOpens() {
        final var provider = new SAPHANADBConnectorNodeSettings.CredentialPortStateProvider();
        final StateProviderInitializer initializer = mock(StateProviderInitializer.class);
        provider.init(initializer);
        verify(initializer).computeAfterOpenDialog();

        final NodeParametersInput input = mock(NodeParametersInput.class);
        when(input.getInPortTypes()).thenReturn(new PortType[]{CredentialPortObject.TYPE});
        assertTrue(provider.computeState(input));

        when(input.getInPortTypes()).thenReturn(new PortType[0]);
        assertFalse(provider.computeState(input));
    }

    @Test
    void portChangesOnlyWhenInputConfigurationChangesAtDefaultPort() {
        assertEquals(443, SAPHANADBConnectorNodeSettings.portForInputConfiguration(30015, false, true));
        assertEquals(30015, SAPHANADBConnectorNodeSettings.portForInputConfiguration(443, true, false));
        assertEquals(30015, SAPHANADBConnectorNodeSettings.portForInputConfiguration(30015, true, true));
        assertEquals(443, SAPHANADBConnectorNodeSettings.portForInputConfiguration(443, false, false));
        assertEquals(12345, SAPHANADBConnectorNodeSettings.portForInputConfiguration(12345, false, true));
        assertEquals(12345, SAPHANADBConnectorNodeSettings.portForInputConfiguration(12345, true, false));
    }

    @Test
    void previousPortStateIsBackwardsCompatibleAndPersists() throws Exception {
        final var persistor = new SAPHANADBConnectorNodeSettings.CredentialPortStatePersistor();
        final var saved = new NodeSettings("test");
        assertEquals(false, persistor.load(saved));
        persistor.save(true, saved);
        assertEquals(true, persistor.load(saved));
    }

    @Test
    void savedSettingsPreservePortAndPreviousConfiguration() throws Exception {
        final var settings = new SAPHANADBConnectorNodeSettings();
        settings.m_port = 30015;
        settings.m_previouslyHadCredentialPort = true;
        final var saved = new NodeSettings("test");
        NodeParametersUtil.saveSettings(SAPHANADBConnectorNodeSettings.class, settings, saved);

        final var loaded = NodeParametersUtil.loadSettings(saved, SAPHANADBConnectorNodeSettings.class);
        assertEquals(30015, loaded.m_port);
        assertEquals(true, loaded.m_previouslyHadCredentialPort);
    }
}
