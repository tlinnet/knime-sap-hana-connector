# SAP HANA Connector project

Develop a Java node extension for KNIME Analytics Platform that creates SAP HANA database connections. Follow KNIME's [Java node quickstart](https://docs.knime.com/developers/latest/new_node_quickstart/) and [SDK setup](https://github.com/knime-oss/knime-sdk-setup) guidance, along with the APIs and conventions in the checked-out references.

## References

### Webpages
- Follow KNIME's [guidelines for contributing an extension](https://docs.knime.com/developers/latest/development_contribute_extension/) when developing or packaging the extension.
- SAP documents JWT user authentication for HANA Data Services [here](https://help.sap.com/docs/SAP_DATA_SERVICES/af6d8e979d0f40c49175007e486257f0/8d553b3a0aca45beb9f6dfb68185ebaa.html): leave the User Name field empty and put the encoded JWT in the Password field.
- Consult SAP HANA Platform's [Single Sign-On Using JSON Web Tokens](https://help.sap.com/docs/SAP_HANA_PLATFORM/b3ee5778bc2e4a089d3299b82ec762a7/4b3dc4d4b5514dbd9d45cee7bc167c5a.html) documentation for HANA JWT SSO behavior.

### Local code reference material
- `reference/` is locally ignored and is not part of this repository's deliverable. Use it as read-only reference material; do not modify or add its contents to commits.
- `reference/noding_guidelines.pdf` contains local KNIME node development guidelines. Consult it alongside KNIME's online extension contribution guidance.
- `reference/knime-database-analytics-platform-5.12.0/` is the database implementation reference. For an optional JWT Credentials input port, inspect the current Microsoft SQL Server connector under `org.knime.database.extensions/src/org/knime/database/extension/mssql/node/connector/`.
- `reference/knime-examples-analytics-platform-5.12.0/` contains example node implementations; `org.knime.examples.numberformatter` is a minimal Java node example.
- `reference/knime-sdk-setup-analytics-platform-5.12.0/` contains the SDK setup instructions and Eclipse target platform definitions for KNIME Analytics Platform 5.12.0. For this database connector, select `KNIME-AP-complete.target` in Eclipse; the complete target includes more KNIME extensions and is the safer fit.

## Connector requirements

- Follow KNIME's database connector patterns for the node factory, model, settings, connection controller, ports, and tests. Prefer current APIs and patterns over deprecated examples.
- Support an optional KNIME Credentials input port that accepts a JWT credential, following the Microsoft SQL Server connector's port and credential-resolution flow where applicable.
- For JWT authentication to SAP HANA, obtain the token through KNIME's credential APIs and pass the token as the HANA JDBC driver's **password**. Keep the JDBC username blank. Do not copy SQL Server's `accessToken` JDBC-property behavior; that is specific to its driver.
- Keep credentials out of persisted settings, logs, and error messages. Preserve existing non-JWT connection behavior when changing authentication.
- Test the optional-port and JWT paths, including that the token is supplied as the password and the username is blank. Add or update node documentation when behavior or configuration changes.

## Development and validation

- Inspect the repository's build files and existing conventions before choosing build or test commands. Run the narrowest relevant tests and checks available; use the repository's documented build when changes warrant it.
- Keep changes focused on the SAP HANA connector. Do not copy reference modules wholesale or add unrelated infrastructure.
