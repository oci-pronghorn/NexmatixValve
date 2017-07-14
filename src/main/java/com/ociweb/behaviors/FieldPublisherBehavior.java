package com.ociweb.behaviors;

import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;

import static com.ociweb.schema.MessageScheme.stationCount;

public class FieldPublisherBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    public final String[][] publishTopics;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    public FieldPublisherBehavior(FogRuntime runtime, String topic) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.publishTopics = new String[stationCount][MessageScheme.topics.length];

        for (int stationId = 0; stationId < stationCount; stationId++) {
            for (int valueId = 1; valueId < MessageScheme.topics.length; valueId++) {
                this.publishTopics[stationId][valueId] = String.format("%s/%d/%d", topic, stationId, valueId);
            }
        }
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        final long timeStamp = messageReader.readLong();
        //StringBuilder a = new StringBuilder();
        //messageReader.readUTF(a);
        //System.out.println("C) Recieved: " + a.toString());
        final short messageLength = messageReader.readShort();
        System.out.println("C) Length: " + messageLength);
        reader.parseSetup(messageReader, messageLength);
        int stationId = -1;
        while (true) {
            // Why return long only to down cast it to int for capture methods?
            int parsedId = (int) TrieParserReader.parseNext(reader, parser);
            System.out.println("C) Parsed Field: " + parsedId);
            if (parsedId == -1) {
                if (TrieParserReader.parseSkipOne(reader) == -1) {
                    System.out.println("C) End of Message");
                    break;
                }
            }
            else if (parsedId == 0) {
                stationId = (int)TrieParserReader.capturedLongField(reader, 0);
                System.out.println("C) Station Id: " + stationId);
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
        System.out.println("C) Publishing: " + parsedId + " <" + fieldType + ">");
        channel.publishTopic(publishTopics[stationId][parsedId], pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            switch (fieldType) {
                case integer: {
                    int value = (int) TrieParserReader.capturedLongField(reader, 0);
                    pubSubWriter.writeInt(value);
                    break;
                }
                case string: {
                    TrieParserReader.writeCapturedUTF8(reader, 0, pubSubWriter);
                    break;
                }
            }
        });
    }
}
