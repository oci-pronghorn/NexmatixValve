package com.ociweb.behaviors;

import com.ociweb.gl.api.*;
import com.ociweb.pronghorn.pipe.ChannelReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldPublisherBehavior implements PubSubListener {
    private final PubSubService service;
    private final GreenReader parser = MessageScheme.buildParser().newReader();

    public FieldPublisherBehavior(FogRuntime runtime) {
        FogCommandChannel channel = runtime.newCommandChannel();
        this.service = channel.newPubSubService();
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader messageReader) {
        //return debugMessage(messageReader);
        final long timeStamp = messageReader.readLong();
        final short messageLength = messageReader.readShort();
        if (messageLength > 0) {
            int stationId = -1;
            parser.beginRead(messageReader);
            while (parser.hasMore()) {
                int parsedId = (int) parser.readToken();
                if (parsedId == -1) {
                    parser.skipByte();
                }
                if (parsedId == 0) {
                    stationId = (int) parser.extractedLong(0);
                } else {
                    if (stationId != -1) {
                        boolean published = publishSingleValue(timeStamp, stationId, parsedId);
                        if (!published) {
                            return false;
                        }
                    } else {
                        System.out.println("C) Dropped: Value before Station");
                    }
                }
            }
        }
        return true;
    }

    private boolean debugMessage(ChannelReader messageReader) {
        final long timeStamp = messageReader.readLong();
        final short messageLength = messageReader.readShort();
        byte[] buffer = new byte[1024];
        final int actualLength = messageReader.read(buffer, 0, messageLength);
        System.out.println(String.format("C) Received [%d.%d]:'%s'", timeStamp, actualLength, new String(buffer, 0, actualLength)));
        return true;
    }

    private boolean publishSingleValue(long timeStamp, int stationId, int parsedId) {
        final FieldType fieldType = MessageScheme.messages[parsedId].type;
        final String topic = MessageScheme.publishTopic(stationId, parsedId);
        return service.publishTopic(topic, pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            switch (fieldType) {
                case integer: {
                    int value = (int)parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing %s: %s) %d", fieldType.name(), topic, value));
                    pubSubWriter.writeInt(value);
                    break;
                }
                case int64: {
                    long value = parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing %s: %s) %d", fieldType.name(), topic, value));
                    pubSubWriter.writeLong(value);
                    break;
                }
                case string: {
                    String value = parser.extractedString(0);
                    System.out.println(String.format("C) Publishing %s: %s) '%s'", fieldType.name(), topic, value));
                    pubSubWriter.writeUTF(value);
                    break;
                }
                case floatingPoint: {
                    double value = (double) parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing %s: %s) %f", fieldType.name(), topic, value));
                    pubSubWriter.writeDouble(value);
                    break;
                }
            }
        });
    }
}
