package com.drinknbooking.drinkbooking.config;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class TiDBFriendlyDialect extends MySQLDialect {

    private final boolean tidb;

    public TiDBFriendlyDialect() {
        super();
        this.tidb = false;
    }

    public TiDBFriendlyDialect(DialectResolutionInfo info) {
        super(info);
        String version = info.getDatabaseVersion();
        this.tidb = version != null && version.toLowerCase().contains("tidb");
        System.out.println(">>> TiDBFriendlyDialect: version=" + version + " | tidb=" + tidb);
    }

    @Override
    public String getTableTypeString() {
        String base = super.getTableTypeString();
        return tidb ? base + " AUTO_ID_CACHE=1" : base;
    }
}