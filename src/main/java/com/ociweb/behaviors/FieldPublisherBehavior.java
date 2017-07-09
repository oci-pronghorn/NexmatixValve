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
    private final String topic;
    private final byte[] buffer;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(1);

    public FieldPublisherBehavior(FogRuntime runtime, String topic, int maxMessageLen) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.topic = topic;
        this.buffer = new byte[maxMessageLen];
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        final long timeStamp = messageReader.readLong();
        final int messageLen = messageReader.readShort();
        final int read = messageReader.read(buffer, 0, messageLen);
        if (read == messageLen) {
            long stationId = -1;
            while (true) {
                // Why return long only to down cast it for capture methods?
                int parsedId = (int) TrieParserReader.parseNext(reader, parser);
                if (parsedId == -1) {
                    break;
                } else if (parsedId == 0) {
                    stationId = TrieParserReader.capturedLongField(reader, parsedId);
                } else if (stationId != -1) {
                    publishSingleValue(timeStamp, stationId, parsedId);
                }
            }
        }
        return true;
    }

    private void publishSingleValue(long timeStamp, long stationId, int valueId) {
        final int fieldType = MessageScheme.types[valueId];
        if (fieldType == -1) {
            return;
        }
        channel.publishTopic(String.format("%s/%d/%d", topic, stationId, valueId), pubSubWriter -> {
            pubSubWriter.writeLong(timeStamp);
            pubSubWriter.writeInt(fieldType);
            if (fieldType == 0) {
                int value = (int)TrieParserReader.capturedLongField(reader, valueId);
                pubSubWriter.writeInt(value);
            }
            else if (fieldType == 1) {
                StringBuilder str = new StringBuilder();
                TrieParserReader.capturedFieldBytesAsUTF8(reader, valueId, str);
                pubSubWriter.writeUTF(str);
            }
        });
    }
}
