package com.pixelcare.global.common;

import org.springframework.jdbc.support.KeyHolder;
import java.util.Map;

public final class KeyExtractUtils {

    private KeyExtractUtils() {}

    public static Long extractId(KeyHolder keyHolder) {
        if (keyHolder == null) {
            throw new IllegalArgumentException("KeyHolder must not be null");
        }
        try {
            Number singleKey = keyHolder.getKey();
            if (singleKey != null) {
                return singleKey.longValue();
            }
        } catch (Exception ignored) {
            // PostgreSQL JDBC driver returns all inserted columns in KeyHolder causing keyHolder.getKey() to throw
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && !keys.isEmpty()) {
            Object idVal = keys.get("id");
            if (idVal == null) idVal = keys.get("ID");
            if (idVal instanceof Number num) {
                return num.longValue();
            }
            for (Object val : keys.values()) {
                if (val instanceof Number num) {
                    return num.longValue();
                }
            }
        }
        if (keyHolder.getKeyList() != null && !keyHolder.getKeyList().isEmpty()) {
            Map<String, Object> firstRow = keyHolder.getKeyList().get(0);
            Object idVal = firstRow.get("id");
            if (idVal == null) idVal = firstRow.get("ID");
            if (idVal instanceof Number num) {
                return num.longValue();
            }
            for (Object val : firstRow.values()) {
                if (val instanceof Number num) {
                    return num.longValue();
                }
            }
        }
        throw new IllegalStateException("Generated key could not be extracted from KeyHolder");
    }
}
