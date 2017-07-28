package com.ociweb.behaviors;

import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;

import static com.ociweb.schema.MessageScheme.parseIdLimit;
import static com.ociweb.schema.MessageScheme.stationCount;

public class FieldPublisherBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final String[][] publishTopics;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    public FieldPublisherBehavior(FogRuntime runtime, String topic) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.publishTopics = new String[stationCount][parseIdLimit];

        for (int stationId = 0; stationId < stationCount; stationId++) {
            for (int parseId = 0; parseId < parseIdLimit; parseId++) {
                this.publishTopics[stationId][parseId] = String.format("%s/%d/%d", topic, stationId, parseId);
            }
        }
    }

    public String publishTopic(int stationId, int parsedId) {
        return publishTopics[stationId][parsedId];
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final long timeStamp = messageReader.readLong();
        //StringBuilder a = new StringBuilder();
        //messageReader.readUTF(a);
        //System.out.println(String.format("C) Recieved: %d:'%s'", a.length(), a.toString()));
        final short messageLength = messageReader.readShort();
        //System.out.println("C) Length: " + messageLength);
        reader.parseSetup(messageReader, messageLength);
        int stationId = -1;
        while (true) {
            // Why return long only to down cast it to int for capture methods?
            int parsedId = (int) TrieParserReader.parseNext(reader, parser);
            //System.out.println("C) Parsed Field: " + parsedId);
            if (parsedId == -1) {
                if (TrieParserReader.parseSkipOne(reader) == -1) {
                    //System.out.println("C) End of Message");
                    break;
                }
            }
            else if (parsedId == 0) {
                stationId = (int)TrieParserReader.capturedLongField(reader, 0);
                //System.out.println("C) Station Id: " + stationId);
            }
            else {
                if (stationId != -1) {
                    publishSingleValue(timeStamp, stationId, parsedId);
                }
                else {
                    System.out.println("C) Value before Station dropped");
                }
            }
        }
        return true;
    }

    private void publishSingleValue(long timeStamp, int stationId, int parsedId) {
        final FieldType fieldType = MessageScheme.types[parsedId];
        final String topic = publishTopic(stationId, parsedId);
        channel.publishTopic(topic, pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            switch (fieldType) {
                case integer: {
                    int value = (int) TrieParserReader.capturedLongField(reader, 0);
                    System.out.println(String.format("C) Publishing: %s) %d", topic, value));
                    pubSubWriter.writeInt(value);
                    break;
                }
                case string: {
                    TrieParserReader.writeCapturedUTF8(reader, 0, pubSubWriter);
                    System.out.println(String.format("C) Publishing: %s) '%s'", topic, "some string"));
                    break;
                }
                case floatingPoint: {
                    double value = (double) TrieParserReader.capturedLongField(reader, 0);
                    System.out.println(String.format("C) Publishing: %s) %f", topic, value));
                    pubSubWriter.writeDouble(value);
                    break;
                }
            }
        });
    }
}
