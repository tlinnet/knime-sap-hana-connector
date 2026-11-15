package org.knime.database.extension.sap.hana.connector;

import org.knime.database.DBType;
import org.knime.database.DBTypeRegistry;

final class SAPHANAType {

    private SAPHANAType() {
    }

    static DBType get() {
        return DBTypeRegistry.getInstance().getRegisteredDBType("saphana")
            .orElseThrow(() -> new IllegalStateException("The KNIME SAP HANA database extension is not available."));
    }
}
