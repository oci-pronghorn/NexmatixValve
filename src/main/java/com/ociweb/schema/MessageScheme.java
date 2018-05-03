package com.ociweb.schema;

import com.ociweb.gl.api.GreenTokenMap;
import java.util.function.Consumer;
import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(10, 10);
    public final static double minPsi = 0.0;
    public final static double maxPsi = 120.0;

    public final static String ManifoldPrefix = "Manifold";
    public final static String StationPrefix = "";
    public final static String UARTTopic = "UART";
    public final static String FieldTopic = "Field";
    public final static String FilterTopic = "Filter";

    private static String[][] internalPublishTopics = new String[MessageScheme.stationCount][MessageScheme.parseIdLimit];
    private static String[][] filterPublishTopics = new String[MessageScheme.stationCount][MessageScheme.parseIdLimit];
    private static String[][] externalPublishTopics = new String[MessageScheme.stationCount][MessageScheme.parseIdLimit];

    public static final MsgField[] messages = new MsgField[] {
            new MsgField("st", integer, "StationId"),
            new MsgField("sn", integer, "SerialNumber"),
            new MsgField("cl", integer,"CycleCountLimnit"),
            new MsgField("cc", integer, "CycleCount"),
            new MsgField("pp", floatingPoint, "PressurePoint"),
            new MsgField("fd", int64, "FabricationDate"),
            new MsgField("sd", int64, "ShipmentDate"),
            new MsgField("pf", string, "PressureFault"),
            new MsgField("ld", string, "LeakDetection"),
            new MsgField("in", string, "InputState"),
            new MsgField("pn", string,"ProductNumber"),
    };

    public static void declareTopics(int manifoldNumber, Consumer<String> consume) {
        // For every station and published field
        for (int stationId = 0; stationId < MessageScheme.stationCount; stationId++) {
            // Skip Station Id at parseId 0
            for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
                internalPublishTopics[stationId][parseId] = String.format("%s/%d/%d", FieldTopic, stationId, parseId);
                filterPublishTopics[stationId][parseId] = String.format("%s/%d/%d", FilterTopic, stationId, parseId);
                externalPublishTopics[stationId][parseId] = String.format("%s%d/%s%d/%s", ManifoldPrefix, manifoldNumber, StationPrefix, stationId, MessageScheme.messages[parseId].mqttKey);
                consume.accept(internalPublishTopics[stationId][parseId]);
            }
        }
    }

    public static String internalPublishTopic(int stationId, int parsedId) {
        return internalPublishTopics[stationId][parsedId];
    }

    public static String filterPublishTopic(int stationId, int parsedId) {
        return filterPublishTopics[stationId][parsedId];
    }

    public static String externalPublishTopic(int stationId, int parsedId) {
        return externalPublishTopics[stationId][parsedId];
    }

    public static GreenTokenMap buildParser() {
        GreenTokenMap map = new GreenTokenMap();
        for (int i = 0; i < parseIdLimit; i++) {
            map = map.add(i, messages[i].getPattern());
        }
        return map;
    }
}
