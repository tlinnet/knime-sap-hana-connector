package org.knime.database.extension.sap.hana.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.node.parameters.widget.credentials.AuthenticationParameters.AuthenticationType;

class SAPHANADBConnectorNodeSettingsTest {

    @Test
    void kerberosIsOfferedAsConfiguredAuthenticationType() {
        final var provider = new SAPHANADBConnectorNodeSettings.AvailableAuthenticationTypesProvider();

        assertEquals(List.of(AuthenticationType.NONE, AuthenticationType.USER_PWD, AuthenticationType.KERBEROS),
            provider.choices(null));
    }
}
