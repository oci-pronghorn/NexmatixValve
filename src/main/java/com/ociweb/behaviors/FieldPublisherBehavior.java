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
        final long timeStamp = messageReader.readLong();
        int stationId = -1;
        parser.beginRead(messageReader);
        while (parser.hasMore()) {
            int parsedId = (int)parser.readToken();
            if (parsedId == -1) {
                parser.skipByte();
            }
            if (parsedId == 0) {
                stationId = (int)parser.extractedLong(0);
            }
            else {
                if (stationId != -1) {
                    publishSingleValue(timeStamp, stationId, parsedId);
                }
                else {
                    System.out.println("C) Dropped: Value before Station");
                }
            }
        }
        return true;
    }

    private void publishSingleValue(long timeStamp, int stationId, int parsedId) {
        final FieldType fieldType = MessageScheme.types[parsedId];
        final String topic = MessageScheme.publishTopic(stationId, parsedId);
        service.publishTopic(topic, pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            switch (fieldType) {
                case integer: {
                    int value = (int)parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing: %s) %d", topic, value));
                    pubSubWriter.writeInt(value);
                    break;
                }
                case int64: {
                    long value = parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing: %s) %d", topic, value));
                    pubSubWriter.writeLong(value);
                    break;
                }
                case string: {
                    System.out.println(String.format("C) Publishing: %s) '%s'", topic, "some string"));
                    parser.copyExtractedUTF8ToAppendable(0, pubSubWriter);
                    break;
                }
                case floatingPoint: {
                    double value = (double) parser.extractedLong(0);
                    System.out.println(String.format("C) Publishing: %s) %f", topic, value));
                    pubSubWriter.writeDouble(value);
                    break;
                }
            }
        });
    }
}
