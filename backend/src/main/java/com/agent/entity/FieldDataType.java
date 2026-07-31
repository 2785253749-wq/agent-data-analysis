package com.agent.entity;

/**
 * Valid data types for dataset fields.
 * Used for SQL generation type-awareness and frontend dropdown options.
 */
public enum FieldDataType {
    VARCHAR,
    INT,
    BIGINT,
    DECIMAL,
    DATETIME,
    DATE,
    TEXT,
    BOOLEAN;

    /**
     * Case-insensitive parse with a friendly error message.
     */
    public static FieldDataType fromString(String value) {
        for (FieldDataType dt : values()) {
            if (dt.name().equalsIgnoreCase(value)) {
                return dt;
            }
        }
        throw new IllegalArgumentException(
                "Invalid data type: " + value + ". Valid values: " + validValues());
    }

    public static String validValues() {
        StringBuilder sb = new StringBuilder();
        for (FieldDataType dt : values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(dt.name().toLowerCase());
        }
        return sb.toString();
    }
}
