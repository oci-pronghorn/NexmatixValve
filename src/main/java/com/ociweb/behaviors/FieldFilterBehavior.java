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
    private final String fieldName;
    public final String publishTopic;

    private int intCache = Integer.MAX_VALUE;
    private long longCache = Long.MAX_VALUE;
    private String stringCache = "<Unknown>";
    private double floatingPointCache = Double.MAX_VALUE;

    public FieldFilterBehavior(FogRuntime runtime, String filterFieldTopic, int parsedId) {
        FogCommandChannel channel = runtime.newCommandChannel();
        this.service = channel.newPubSubService();
        this.fieldType = MessageScheme.messages[parsedId].type;
        this.publishTopic = filterFieldTopic;
        this.fieldName = MessageScheme.messages[parsedId].mqttKey;
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader messageReader) {
        final long timeStamp = messageReader.readPackedLong();
        boolean publish = false;
        switch (fieldType) {
            case integer: {
                int newValue = messageReader.readInt();
                if (newValue != intCache) {
                    publish = true;
                    System.out.println(String.format("D) %s %s:%s (%d) %d != %d", publishTopic, fieldName, fieldType.name(), timeStamp, intCache, newValue));
                    intCache = newValue;
                }
                break;
            }
            case int64: {
                long newValue = messageReader.readLong();
                if (newValue != longCache) {
                    publish = true;
                    System.out.println(String.format("D) %s %s:%s (%d) %d != %d", publishTopic, fieldName, fieldType.name(), timeStamp, longCache, newValue));
                    longCache = newValue;
                }
                break;
            }
            case string: {
                String newValue = messageReader.readUTF();
                if (newValue.isEmpty()) {
                    break;
                }
                if (!newValue.equals(stringCache)) {
                    publish = true;
                    System.out.println(String.format("D) %s %s:%s (%d) '%s' != '%s'", publishTopic, fieldName, fieldType.name(), timeStamp, stringCache, newValue));
                    stringCache = newValue;
                }
                break;
            }
            case floatingPoint: {
                double newValue = messageReader.readDouble();
                if (newValue != floatingPointCache) {
                    publish = true;
                    System.out.println(String.format("D) %s %s:%s (%d) %f != %f", publishTopic, fieldName, fieldType.name(), timeStamp, floatingPointCache, newValue));
                    floatingPointCache = newValue;
                }
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
