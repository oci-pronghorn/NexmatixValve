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
        final long timeStamp = messageReader.readLong();
        boolean publish = false;
        if (fieldType == 0) {
            int newValue = messageReader.readInt();
            System.out.println(String.format("D) Path:%s <%d> %d", this.publishTopic, fieldType, newValue));
            if (newValue != intCache) {
                intCache = newValue;
                publish = true;
            }
        }
        else if (fieldType == 1) {
            String newValue = messageReader.readUTF();
            System.out.println(String.format("D) Path:%s <%d> %s", this.publishTopic, fieldType, newValue));
            if (!newValue.equals(stringCache)) {
                stringCache = newValue;
                publish = true;
            }
        }
        if (publish) {
            channel.publishTopic(publishTopic, pubSubWriter -> {
                pubSubWriter.writeLong(timeStamp);
                System.out.println("D) Issued");
                if (fieldType == 0) {
                    pubSubWriter.writeInt(intCache);
                }
                else {
                    pubSubWriter.writeUTF(stringCache);
                }
            });
        }
        else {
            System.out.println("D) Filtered");
        }
        return true;
    }
}
