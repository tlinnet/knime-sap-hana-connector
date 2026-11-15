package org.knime.database.extension.sap.hana.connector;

import java.io.IOException;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.database.port.DBSessionPortObject;
import org.knime.node.parameters.legacy.nodeimpl.WebUINodeConfiguration;
import org.knime.node.parameters.legacy.nodeimpl.WebUINodeFactory;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public final class SAPHANADBConnectorNodeFactory extends ConfigurableNodeFactory<SAPHANADBConnectorNodeModel>
    implements NodeDialogFactory {

    private static final String CREDENTIAL_INPUT_PORT = "Credential (JWT)";

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder()
        .name("SAP HANA Connector")
        .icon("sap_hana_connector.png")
        .shortDescription("Create a database connection to SAP HANA.")
        .fullDescription("""
                Creates a connection to a SAP HANA database through its JDBC driver. Configure the server hostname, \
                port, and authentication (none, username/password, or Kerberos). SAP HANA connections use the JDBC \
                format jdbc:sap://HOST:PORT. Register and select a SAP HANA JDBC driver in KNIME's \
                database preferences. A JWT credential can be supplied through the optional input port.
                """)
        .modelSettingsClass(SAPHANADBConnectorNodeSettings.class)
        .addExternalResource("https://docs.knime.com/ap/latest/db_extension_guide/", "KNIME Database documentation")
        .nodeType(NodeType.Source)
        .addInputPort(CREDENTIAL_INPUT_PORT, CredentialPortObject.TYPE, """
                Optionally connect a JWT credential. When connected, the token is passed as the SAP HANA JDBC \
                password and the username is left blank; this takes precedence over the configured authentication.
                """, true)
        .addOutputPort("DB Connection", DBSessionPortObject.TYPE, "SAP HANA DB Connection.")
        .keywords("database", "db", "connection", "session", "SAP", "HANA", "JWT", "Kerberos")
        .build();

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIGURATION);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var builder = new PortsConfigurationBuilder();
        builder.addFixedOutputPortGroup("DB Connection", DBSessionPortObject.TYPE);
        builder.addOptionalInputPortGroup(CREDENTIAL_INPUT_PORT, CredentialPortObject.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected SAPHANADBConnectorNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new SAPHANADBConnectorNodeModel(creationConfig.getPortConfig().orElseThrow());
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<SAPHANADBConnectorNodeModel> createNodeView(final int viewIndex,
        final SAPHANADBConnectorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, SAPHANADBConnectorNodeSettings.class);
    }
}
