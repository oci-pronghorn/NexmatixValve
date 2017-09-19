package com.ociweb.schema;

import Nexmatix.ValveData;

import java.util.function.BiConsumer;

public class MsgField {
    public final String key;
    public final FieldType type;
    public final String jsonKey;
    public final boolean isStatus;
    public final boolean isConfig;
    public final BiConsumer<ValveData, Object> setValve;

    MsgField(String key, FieldType type, String jsonKey, boolean isStatus, boolean isConfig, BiConsumer<ValveData, Object> setValve) {
        this.key = key;
        this.type = type;
        this.jsonKey = jsonKey;
        this.isStatus = isStatus;
        this.isConfig = isConfig;
        this.setValve = setValve;
    }

    String getPattern() {
        return key + type.getPattern();
    }
}
