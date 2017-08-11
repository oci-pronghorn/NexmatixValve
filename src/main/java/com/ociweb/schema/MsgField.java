package com.ociweb.schema;

public class MsgField {
    public final String key;
    public final FieldType type;
    public final String jsonKey;
    public final boolean isStatus;
    public final boolean isConfig;

    MsgField(String key, FieldType type, String jsonKey, boolean isStatus, boolean isConfig) {
        this.key = key;
        this.type = type;
        this.jsonKey = jsonKey;
        this.isStatus = isStatus;
        this.isConfig = isConfig;
    }

    String getPattern() {
        return key + type.getPattern();
    }
}
