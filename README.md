# SAP HANA Connector for KNIME

This project develops a Java-based connector node for creating database connections to SAP HANA in KNIME Analytics Platform. The goal is to follow KNIME's database connector conventions and provide an optional **Credentials** input port for JWT-based authentication.

## JWT authentication

When a JWT credential is connected, the connector is intended to pass the encoded token to the SAP HANA JDBC driver's **password** field and leave the **username** blank. This follows SAP's guidance for JWT user authentication. The token should be supplied through KNIME's credential APIs, not entered into or stored in the node's settings.

This behavior is under development; check the current implementation before relying on JWT support in a KNIME workflow.

## Development

- See KNIME's [Java node quickstart](https://docs.knime.com/developers/latest/new_node_quickstart/) for an introduction to Java node development.
- Follow the [KNIME SDK setup](https://github.com/knime-oss/knime-sdk-setup) instructions to configure a development environment.
- Inspect the SAP references below for JWT authentication details.

## References

- SAP Data Services: [JWT user authentication](https://help.sap.com/docs/SAP_DATA_SERVICES/af6d8e979d0f40c49175007e486257f0/8d553b3a0aca45beb9f6dfb68185ebaa.html) — for a HANA datastore and an MDS/VDS request, leave **User Name** empty and enter the encoded JWT in **Password**.
- SAP HANA Platform: [Single Sign-On Using JSON Web Tokens](https://help.sap.com/docs/SAP_HANA_PLATFORM/b3ee5778bc2e4a089d3299b82ec762a7/4b3dc4d4b5514dbd9d45cee7bc167c5a.html).

The `reference/` directory may contain local checkouts of KNIME's database and example node repositories for development. It is local reference material and is not included in this repository.
