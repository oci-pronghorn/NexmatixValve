package com.ociweb.schema;

public class MsgField {
    public final String key;
    public final FieldType type;
    public final String mqttKey;

    MsgField(String key, FieldType type, String mqttKey) {
        this.key = key;
        this.type = type;
        this.mqttKey = mqttKey;
    }

    String getPattern() {
        return key + type.getPattern();
    }
}
