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
                System.out.println(String.format("D) Detected int: %s: %d -> %d", this.publishTopic, intCache, newValue));
                if (newValue != intCache) {
                    intCache = newValue;
                    publish = true;
                }
                break;
            }
            case int64: {
                long newValue = messageReader.readLong();
                System.out.println(String.format("D) Detected long: %s: %d -> %d", this.publishTopic, intCache, newValue));
                if (newValue != intCache) {
                    longCache = newValue;
                    publish = true;
                }
                break;
            }
            case string: {
                String newValue = messageReader.readUTF();
                System.out.println(String.format("D) Detected string: %s: '%s' -> '%s'", this.publishTopic, stringCache, newValue));
                if (!newValue.equals(stringCache)) {
                    stringCache = newValue;
                    publish = true;
                }
                break;
            }
            case floatingPoint: {
                double newValue = messageReader.readDouble();
                System.out.println(String.format("D) Detected decimal: %s: '%s' -> '%f'", this.publishTopic, stringCache, newValue));
                if (newValue != floatingPointCache) {
                    floatingPointCache = newValue;
                    publish = true;
                }
                break;
            }
        }
        if (publish) {
            service.publishTopic(publishTopic, pubSubWriter -> {
                pubSubWriter.writeLong(timeStamp);
                System.out.println(String.format("D) Issued: %s", this.publishTopic));
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
        else {
            System.out.println(String.format("D) Dropped: %s", this.publishTopic));
        }
        return true;
    }
}
