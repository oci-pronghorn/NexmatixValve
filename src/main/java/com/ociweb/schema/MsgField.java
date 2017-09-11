package com.ociweb.schema;

import java.util.function.BiConsumer;

public class MsgField<T> {
    public final String key;
    public final FieldType type;
    public final String jsonKey;
    public final boolean isStatus;
    public final boolean isConfig;
    public final BiConsumer<ValveStatus, T> setStatus;
    public final BiConsumer<ValveConfig, T> setConfig;

    MsgField(String key, FieldType type, String jsonKey, boolean isStatus, boolean isConfig) {
        this.key = key;
        this.type = type;
        this.jsonKey = jsonKey;
        this.isStatus = isStatus;
        this.isConfig = isConfig;
        this.setStatus = null;
        this.setConfig = null;
    }

    MsgField(String key, FieldType type, String jsonKey, BiConsumer<ValveStatus, T> setStatus, BiConsumer<ValveConfig, T> setConfig) {
        this.key = key;
        this.type = type;
        this.jsonKey = jsonKey;
        this.isStatus = setStatus != null;
        this.isConfig = setConfig != null;
        this.setStatus = setStatus;
        this.setConfig = setConfig;
    }

    String getPattern() {
        return key + type.getPattern();
    }
}
