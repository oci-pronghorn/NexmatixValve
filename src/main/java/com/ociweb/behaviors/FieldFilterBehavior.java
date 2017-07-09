package com.ociweb.behaviors;

import com.ociweb.MessageScheme;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldFilterBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final int fieldType;
    private final String topic;
    private final int valueId;

    private int intCache = Integer.MAX_VALUE;
    private String stringCache = null;

    public FieldFilterBehavior(FogRuntime runtime, String topic, int valueId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.topic = topic;
        this.valueId = valueId;
        this.fieldType = MessageScheme.types[valueId];
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        final long timeStamp = messageReader.readLong();
        final int stationId = messageReader.readInt();
        boolean publish = false;
        if (fieldType == 0) {
            int newValue = messageReader.readInt();
            if (newValue != intCache) {
                intCache = newValue;
                publish = true;
            }
        }
        else if (fieldType == 1) {
            String newValue = messageReader.readUTF();
            if (!newValue.equals(stringCache)) {
                stringCache = newValue;
                publish = true;
            }
        }
        if (publish) {
            channel.publishTopic(String.format("%s/%d/%d", topic, stationId, valueId), pubSubWriter -> {
                pubSubWriter.writeLong(timeStamp);
                if (fieldType == 0) {
                    pubSubWriter.write(intCache);
                }
                else {
                    pubSubWriter.writeUTF(stringCache);
                }
            });
        }
        return true;
    }
}
