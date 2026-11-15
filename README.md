# SAP HANA Connector for KNIME

This project develops a Java-based connector node for creating database connections to SAP HANA in KNIME Analytics Platform. The goal is to follow KNIME's database connector conventions and provide an optional **Credentials** input port for JWT-based authentication.

## JWT authentication

When a JWT credential is connected to the optional **Credential (JWT)** input port, the node retrieves its access token through KNIME's credential APIs and passes it to the SAP HANA JDBC driver's **password** property with the **user** property left blank. This takes precedence over the node's configured authentication. The node keeps both properties out of the JDBC URL, including when JDBC parameters are configured to be appended to the URL, and does not persist the token. JWT-authenticated connections cannot be restored from saved node internals; re-execute the node to obtain a credential. Without a connected JWT credential, use the configured username/password authentication, Kerberos authentication, or no authentication.

This connector is under development. Validate it against your SAP HANA server and JDBC driver before using it in production workflows.

## Development

- See KNIME's [Java node quickstart](https://docs.knime.com/developers/latest/new_node_quickstart/) for an introduction to Java node development.
- Follow the [KNIME SDK setup](https://github.com/knime-oss/knime-sdk-setup) instructions to configure a development environment.
- Follow KNIME's [guidelines for contributing an extension](https://docs.knime.com/developers/latest/development_contribute_extension/) when developing and preparing the extension.
- In Eclipse, choose **`KNIME-AP-complete.target`** as the target platform. The complete target includes more KNIME extensions and is the safer fit when developing a database connector. The local KNIME 5.12.0 target definitions are in `../knime-sdk-setup/`.
- Choose an Eclipse workspace **outside** this repository; Eclipse cannot import the workspace root as a project. Use **File > Import > General > Existing Projects into Workspace**, select this repository's root, and import `org.knime.sap.hana.connector` without copying it. Import `org.knime.sap.hana.connector.tests/` as the test fragment in the same way. The node also requires the SAP HANA database support from the KNIME database extension (the local reference is `../knime-sap-hana-connector-reference/knime-database-analytics-platform-5.12.0/org.knime.database.extensions.sap/`).
- Register the SAP HANA JDBC driver in KNIME's database preferences and select it in the node configuration.
- Run the focused JWT property tests as a JUnit Plug-in Test from `org.knime.sap.hana.connector.tests/`.
- Inspect the SAP references below for JWT authentication details.

## References

- SAP Data Services: [JWT user authentication](https://help.sap.com/docs/SAP_DATA_SERVICES/af6d8e979d0f40c49175007e486257f0/8d553b3a0aca45beb9f6dfb68185ebaa.html) — for a HANA datastore and an MDS/VDS request, leave **User Name** empty and enter the encoded JWT in **Password**.
- SAP HANA Platform: [Single Sign-On Using JSON Web Tokens](https://help.sap.com/docs/SAP_HANA_PLATFORM/b3ee5778bc2e4a089d3299b82ec762a7/4b3dc4d4b5514dbd9d45cee7bc167c5a.html).

Local development references are in the sibling directory `../knime-sap-hana-connector-reference/`. It contains KNIME's database, example node, and SDK setup repositories, as well as `noding_guidelines.pdf`. These reference materials are not included in this repository.
