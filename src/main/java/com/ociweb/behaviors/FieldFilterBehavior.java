package com.ociweb.behaviors;

import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldFilterBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final FieldType fieldType;
    public final String publishTopic;

    private int intCache = Integer.MAX_VALUE;
    private long longCache = Long.MAX_VALUE;
    private String stringCache = "";
    private double floatingPointCache = Double.MAX_VALUE;

    public FieldFilterBehavior(FogRuntime runtime, String publishTopic, int stationId, int parseId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.fieldType = MessageScheme.types[parseId];
        this.publishTopic = String.format("%s/%d/%d", publishTopic, stationId, parseId);
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final long timeStamp = messageReader.readLong();
        boolean publish = false;
        switch (fieldType) {
            case integer: {
                int newValue = messageReader.readInt();
                System.out.println(String.format("D) Detected: %s: %d -> %d", this.publishTopic, intCache, newValue));
                if (newValue != intCache) {
                    intCache = newValue;
                    publish = true;
                }
                break;
            }
            case int64: {
                long newValue = messageReader.readLong();
                System.out.println(String.format("D) Detected: %s: %d -> %d", this.publishTopic, intCache, newValue));
                if (newValue != intCache) {
                    longCache = newValue;
                    publish = true;
                }
                break;
            }
            case string: {
                String newValue = messageReader.readUTF();
                System.out.println(String.format("D) Detected: %s: '%s' -> '%s'", this.publishTopic, stringCache, newValue));
                if (!newValue.equals(stringCache)) {
                    stringCache = newValue;
                    publish = true;
                }
                break;
            }
            case floatingPoint: {
                double newValue = messageReader.readDouble();
                System.out.println(String.format("D) Detected: %s: '%s' -> '%f'", this.publishTopic, stringCache, newValue));
                if (newValue != floatingPointCache) {
                    floatingPointCache = newValue;
                    publish = true;
                }
                break;
            }
        }
        if (publish) {
            channel.publishTopic(publishTopic, pubSubWriter -> {
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
