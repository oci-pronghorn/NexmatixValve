package com.ociweb.behaviors;

import com.ociweb.MessageScheme;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldFilterBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final int fieldType;
    public final String publishTopic;

    private int intCache = Integer.MAX_VALUE;
    private String stringCache = null;

    public FieldFilterBehavior(FogRuntime runtime, String topic, int stationId, int valueId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.fieldType = MessageScheme.types[valueId];
        this.publishTopic = String.format("%s/%d/%d", topic, stationId, valueId);
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        System.out.print(charSequence);
        final long timeStamp = messageReader.readLong();
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
            channel.publishTopic(publishTopic, pubSubWriter -> {
                pubSubWriter.writeLong(timeStamp);
                if (fieldType == 0) {
                    pubSubWriter.writeInt(intCache);
                    System.out.println("Out: " + intCache);
                }
                else {
                    pubSubWriter.writeUTF(stringCache);
                    System.out.println("Out: " + stringCache);
                }
            });
        }
        return true;
    }
}
