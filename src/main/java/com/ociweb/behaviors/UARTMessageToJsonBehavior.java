package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.schema.MsgField;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.ociweb.schema.MessageScheme.*;

public class UARTMessageToJsonBehavior implements PubSubListener {
    private final FogCommandChannel cmd;
    private final boolean isStatus;
    private final int manifoldNumber;
    private final String publishTopic;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    private final int batchCountLimit;
    private int batchCount;

    private Map<Integer, StringBuilder> stations = new HashMap<>();

    public UARTMessageToJsonBehavior(FogRuntime runtime, int manifoldNumber, String publishTopic, boolean isStatus, int batchCountLimit) {
        this.cmd = runtime.newCommandChannel();
        this.isStatus = isStatus;
        this.cmd.ensureDynamicMessaging(64, jsonMessageSize);
        this.manifoldNumber = manifoldNumber;
        this.publishTopic = publishTopic;
        this.batchCountLimit = batchCountLimit;
        this.batchCount = batchCountLimit;
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        NumberFormat formatter = new DecimalFormat("#0.0000");
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
                MsgField msgField = MessageScheme.messages[parsedId];
                if (isStatus && !msgField.isStatus) continue;
                if (!isStatus && !msgField.isConfig) continue;

                String key = msgField.jsonKey;
                final FieldType fieldType = msgField.type;
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
                        // Coming out bad
                        long value = TrieParserReader.capturedLongField(reader, 0);
                        json.append(0/*value*/);
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
                        // Not a decimal - this truncates
                        double value = (double) TrieParserReader.capturedLongField(reader, 0);
                        json.append(formatter.format(value));
                        json.append(",");
                        break;
                    }
                }
            }
        }
        json.delete(json.length()-1, json.length());
        json.append("},");

        stations.put(stationId, json);

        if (stations.size() >= batchCount) {
            StringBuilder all = new StringBuilder();
            all.append("{\""+ manifoldSerialJsonKey + "\":");
            all.append(manifoldNumber);
            all.append(",\"" + timestampJsonKey + "\":");
            all.append(timeStamp);
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
