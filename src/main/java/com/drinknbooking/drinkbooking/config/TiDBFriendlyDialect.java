package com.drinknbooking.drinkbooking.config;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Thêm AUTO_ID_CACHE=1 khi tạo bảng để TiDB cấp ID liên tiếp 1, 2, 3...
 * Đoạn /*T! ... *\/ chỉ TiDB hiểu, MySQL XAMPP coi là chú thích và bỏ qua.
 */
public class TiDBFriendlyDialect extends MySQLDialect {

    public TiDBFriendlyDialect() {
        super();
    }

    public TiDBFriendlyDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public String getTableTypeString() {
        return super.getTableTypeString() + " /*T![auto_id_cache] AUTO_ID_CACHE=1 */";
    }
}