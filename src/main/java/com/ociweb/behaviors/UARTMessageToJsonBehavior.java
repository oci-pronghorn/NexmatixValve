package com.ociweb.behaviors;


import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.ociweb.schema.MessageScheme.*;

public class UARTMessageToJsonBehavior implements PubSubListener {
    private final FogCommandChannel cmd;
    private final boolean isStatus;
    private final String manifoldSerial;
    private final String publishTopic;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    private final int batchCountLimit = 5;
    private int batchCount = batchCountLimit;

    private Map<Integer, StringBuilder> stations = new HashMap<>();

    public UARTMessageToJsonBehavior(FogRuntime runtime, String manifoldSerial, String publishTopic, boolean isStatus) {
        this.cmd = runtime.newCommandChannel();
        this.isStatus = isStatus;
        this.cmd.ensureDynamicMessaging(64, jsonMessageSize);
        this.manifoldSerial = manifoldSerial;
        this.publishTopic = publishTopic;
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

        StringBuilder json = new StringBuilder();

        json.append("{");

        long workAround = System.currentTimeMillis();

        while (true) {
            // Why return long only to down cast it to int for capture methods?
            int parsedId = (int) TrieParserReader.parseNext(reader, parser);
            //System.out.println("E) Parsed Field: " + parsedId);
            if (parsedId == -1) {
                if (TrieParserReader.parseSkipOne(reader) == -1) {
                    //System.out.println("C) End of Message");
                    break;
                }
            }
            else {
                if (isStatus && !MessageScheme.statusField[parsedId]) continue;
                if (!isStatus && !MessageScheme.configField[parsedId]) continue;

                String key = MessageScheme.jsonKeys[parsedId];
                final FieldType fieldType = MessageScheme.types[parsedId];
                json.append("\"");
                json.append(key);
                json.append("\":");
                switch (fieldType) {
                    case integer: {
                        int value = (int) TrieParserReader.capturedLongField(reader, 0);
                        if (parsedId == 0) {
                            value = value + 1;
                            stationId = value;
                        }
                        json.append(value);
                        json.append(",");
                        break;
                    }
                    case int64: {
                        long value = TrieParserReader.capturedLongField(reader, 0);
                        if (value < -1) {
                            if (parsedId == 10) {
                                value = workAround;
                            }
                        }
                        else {
                            if (parsedId == 9) {
                                workAround = value;
                            }
                        }
                        json.append(value);
                        json.append(",");
                        break;
                    }
                    case string: {
                        json.append("\"");
                        TrieParserReader.capturedFieldBytesAsUTF8(reader, 0, json);
                        json.append("\"");
                        json.append(",");
                        break;
                    }
                    case floatingPoint: {
                        double value = (double) TrieParserReader.capturedLongField(reader, 0);
                        json.append(value);
                        json.append(",");
                        break;
                    }
                }
            }
        }
        json.append("\"" + timestampJsonKey + "\":");
        json.append(timeStamp);
        json.append("},");

        stations.put(stationId, json);

        if (stations.size() >= batchCount) {
            StringBuilder all = new StringBuilder();
            all.append("{\""+ manifoldSerialJsonKey + "\":");
            all.append(manifoldSerial);
            all.append(",\"" + stationsJsonKey + "\":[");

            for (StringBuilder station: stations.values()) {
                all.append(station);
            }

            all.delete(all.length()-1, all.length());
            all.append("]}");

            String body = all.toString();
            stations.clear();
            batchCount = ThreadLocalRandom.current().nextInt(1, batchCountLimit + 1);
            System.out.println(String.format("C.%s) %s", publishTopic, body));
            cmd.publishTopic(publishTopic, writer -> {
                writer.writeUTF(body);
            });
        }
        return true;
    }
}
