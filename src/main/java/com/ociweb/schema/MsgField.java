package com.ociweb.schema;

import java.util.function.BiConsumer;

public class MsgField {
    public final String key;
    public final FieldType type;
    public final String jsonKey;
    public final boolean isStatus;
    public final boolean isConfig;
    // TODO: rename and use type to match third class form IDL
    public final BiConsumer<ValveConfig, Object> setValve;

    MsgField(String key, FieldType type, String jsonKey, boolean isStatus, boolean isConfig, BiConsumer<ValveConfig, Object> setValve) {
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
