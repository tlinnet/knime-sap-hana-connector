# SAP HANA Connector project

Develop a Java node extension for KNIME Analytics Platform that creates SAP HANA database connections. Follow KNIME's [Java node quickstart](https://docs.knime.com/developers/latest/new_node_quickstart/) and [SDK setup](https://github.com/knime-oss/knime-sdk-setup) guidance, along with the APIs and conventions in the checked-out references.

## References

- `reference/knime-database-analytics-platform-5.12.0/` is the database implementation reference. For an optional JWT Credentials input port, inspect the current Microsoft SQL Server connector under `org.knime.database.extensions/src/org/knime/database/extension/mssql/node/connector/`.
- `reference/knime-examples-analytics-platform-5.12.0/` contains example node implementations; `org.knime.examples.numberformatter` is a minimal Java node example.
- `reference/` is locally ignored and is not part of this repository's deliverable. Use it as read-only reference material; do not modify or add its contents to commits.

## Connector requirements

- Follow KNIME's database connector patterns for the node factory, model, settings, connection controller, ports, and tests. Prefer current APIs and patterns over deprecated examples.
- Support an optional KNIME Credentials input port that accepts a JWT credential, following the Microsoft SQL Server connector's port and credential-resolution flow where applicable.
- For JWT authentication to SAP HANA, obtain the token through KNIME's credential APIs and pass the token as the HANA JDBC driver's **password**. Keep the JDBC username blank. Do not copy SQL Server's `accessToken` JDBC-property behavior; that is specific to its driver.
- Keep credentials out of persisted settings, logs, and error messages. Preserve existing non-JWT connection behavior when changing authentication.
- Test the optional-port and JWT paths, including that the token is supplied as the password and the username is blank. Add or update node documentation when behavior or configuration changes.

## Development and validation

- Inspect the repository's build files and existing conventions before choosing build or test commands. Run the narrowest relevant tests and checks available; use the repository's documented build when changes warrant it.
- Keep changes focused on the SAP HANA connector. Do not copy reference modules wholesale or add unrelated infrastructure.
