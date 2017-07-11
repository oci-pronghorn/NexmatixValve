package com.ociweb.behaviors;

import com.ociweb.MessageScheme;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;

public class FieldPublisherBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    public final String[][] publishTopics;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    public FieldPublisherBehavior(FogRuntime runtime, String topic) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.publishTopics = new String[10][MessageScheme.topics.length];

        for (int stationId = 0; stationId < 10; stationId++) {
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
        //System.out.println(a.toString());
        final short messageLength = messageReader.readShort();
        reader.parseSetup(messageReader, messageLength);
        int stationId = -1;
        while (true) {
            // Why return long only to down cast it to int for capture methods?
            int parsedId = (int) TrieParserReader.parseNext(reader, parser);
            if (parsedId == -1) {
                if (TrieParserReader.parseSkipOne(reader) == -1) {
                    break;
                }
            } else if (parsedId == 0) {
                stationId = (int)TrieParserReader.capturedLongField(reader, 0);
            } else if (stationId != -1) {
                publishSingleValue(timeStamp, stationId, parsedId);
            }
        }
        return true;
    }

    private void publishSingleValue(long timeStamp, int stationId, int parsedId) {
        final int fieldType = MessageScheme.types[parsedId];
        if (fieldType == -1) {
            return;
        }
        channel.publishTopic(publishTopics[stationId][parsedId], pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            if (fieldType == 0) {
                int value = (int)TrieParserReader.capturedLongField(reader, 0);
                pubSubWriter.writeInt(value);
            }
            else if (fieldType == 1) {
                TrieParserReader.capturedFieldBytesAsUTF8(reader, parsedId, pubSubWriter);
            }
        });
    }
}
