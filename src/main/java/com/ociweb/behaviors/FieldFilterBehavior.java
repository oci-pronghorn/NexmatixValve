package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubService;
import com.ociweb.pronghorn.pipe.ChannelReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldFilterBehavior implements PubSubListener {
    private final PubSubService service;
    private final FieldType fieldType;
    public final String publishTopic;

    private int intCache = Integer.MAX_VALUE;
    private long longCache = Long.MAX_VALUE;
    private String stringCache = "";
    private double floatingPointCache = Double.MAX_VALUE;

    public FieldFilterBehavior(FogRuntime runtime, String publishTopic, int stationId, int parseId) {
        FogCommandChannel channel = runtime.newCommandChannel();
        this.service = channel.newPubSubService();
        this.fieldType = MessageScheme.messages[parseId].type;
        this.publishTopic = String.format("%s/%d/%d", publishTopic, stationId, parseId);
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader messageReader) {
        final long timeStamp = messageReader.readLong();
        boolean publish = false;
        switch (fieldType) {
            case integer: {
                int newValue = messageReader.readInt();
                if (newValue != intCache) {
                    intCache = newValue;
                    publish = true;
                }
                System.out.println(String.format("D) Filtered %s %b: %s: %d -> %d", fieldType.name(), !publish, this.publishTopic, intCache, newValue));
                break;
            }
            case int64: {
                long newValue = messageReader.readLong();
                if (newValue != intCache) {
                    longCache = newValue;
                    publish = true;
                }
                System.out.println(String.format("D) Filtered %s %b: %s: %d -> %d", fieldType.name(), !publish, this.publishTopic, intCache, newValue));
                break;
            }
            case string: {
                String newValue = messageReader.readUTF();
                if (!newValue.equals(stringCache)) {
                    stringCache = newValue;
                    publish = true;
                }
                System.out.println(String.format("D) Filtered %s %b: %s: '%s' -> '%s'", fieldType.name(), !publish, this.publishTopic, stringCache, newValue));
                break;
            }
            case floatingPoint: {
                double newValue = messageReader.readDouble();
                if (newValue != floatingPointCache) {
                    floatingPointCache = newValue;
                    publish = true;
                }
                System.out.println(String.format("D) Filtered %s %b: %s: '%s' -> '%f'", fieldType.name(), !publish, this.publishTopic, stringCache, newValue));
                break;
            }
        }
        if (publish) {
            service.publishTopic(publishTopic, pubSubWriter -> {
                //pubSubWriter.writeLong(timeStamp);
                switch (fieldType) {
                    case integer:
                        pubSubWriter.writeInt(intCache);
                        break;
                    case int64:
                        pubSubWriter.writeLong(longCache);
                        break;
                    case string:
                        pubSubWriter.writeUTF(stringCache);
                        break;
                    case floatingPoint:
                        pubSubWriter.writeDouble(floatingPointCache);
                        break;
                }
            });
        }
        return true;
    }
}
