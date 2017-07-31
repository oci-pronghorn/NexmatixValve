package com.ociweb.behaviors;


import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.TrieParser;
import com.ociweb.pronghorn.util.TrieParserReader;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;

public class UARTMessageToJsonBehavior implements PubSubListener {
    private FogCommandChannel cmd;
    private final String manifoldSerial;
    private final String publishTopic;
    private final TrieParser parser = MessageScheme.buildParser();
    private final TrieParserReader reader = new TrieParserReader(4, true);

    public UARTMessageToJsonBehavior(FogRuntime runtime, String manifoldSerial, String publishTopic) {
        this.cmd = runtime.newCommandChannel();
        this.cmd.ensureDynamicMessaging(40, 2048);
        this.manifoldSerial = manifoldSerial;
        this.publishTopic = publishTopic;
    }

    // TODO: build up as JSON and publish as UTF8
    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final long timeStamp = messageReader.readLong();
        //StringBuilder a = new StringBuilder();
        //messageReader.readUTF(a);
        //System.out.println(String.format("E) Recieved: %d:'%s'", a.length(), a.toString()));
        final short messageLength = messageReader.readShort();
        //System.out.println("E) Length: " + messageLength);
        reader.parseSetup(messageReader, messageLength);
        int stationId = -1;

        StringBuilder json = new StringBuilder();
        json.append("{\"manifolds\": { \"manifold_sn\" : ");
        json.append(manifoldSerial);
        json.append(", \"stations\" : {\"");

        while (true) {
            // Why return long only to down cast it to int for capture methods?
            int parsedId = (int) TrieParserReader.parseNext(reader, parser);
            //System.out.println("C) Parsed Field: " + parsedId);
            if (parsedId == -1) {
                if (TrieParserReader.parseSkipOne(reader) == -1) {
                    //System.out.println("E) End of Message");
                    break;
                }
            }
            else if (parsedId == 0) {
                stationId = (int)TrieParserReader.capturedLongField(reader, 0);
                json.append(stationId);
                json.append("\" : {");
            }
            else {
                if (stationId != -1) {
                    String key = MessageScheme.jsonKeys[parsedId];
                    final FieldType fieldType = MessageScheme.types[parsedId];
                    json.append("\"");
                    json.append(key);
                    json.append("\":");
                    switch (fieldType) {
                        case integer: {
                            int value = (int) TrieParserReader.capturedLongField(reader, 0);
                            json.append(value);
                            json.append(", ");
                            break;
                        }
                        case string: {
                            String value = "test";//TrieParserReader.writeCapturedUTF8(reader, 0, json);
                            json.append("\"");
                            json.append(value);
                            json.append("\"");
                            json.append(", ");
                            break;
                        }
                        case floatingPoint: {
                            double value = (double) TrieParserReader.capturedLongField(reader, 0);
                            json.append(value);
                            json.append(", ");
                            break;
                        }
                    }
                }
                else {
                    System.out.println("E) Value before Station dropped");
                }
            }
        }
        json.append("\"timeStamp\":");
        json.append(timeStamp);
        json.append("}}}}");
        System.out.println(String.format("E) %s", json.toString()));

        cmd.publishTopic(publishTopic, writer -> {
            writer.writeUTF(json.toString());
        });
        return true;
    }
}