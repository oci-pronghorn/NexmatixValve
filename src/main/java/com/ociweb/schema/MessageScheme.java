package com.ociweb.schema;
import com.ociweb.gl.api.GreenTokenMap;
import com.ociweb.iot.maker.Hardware;

import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(11, 11);

    private static String[][] publishTopics = new String[MessageScheme.stationCount][MessageScheme.parseIdLimit];

    public static void declareConnections(Hardware builder, String publishTopic) {
        // For every station and published field
        for (int stationId = 0; stationId < MessageScheme.stationCount; stationId++) {
            // Skip Station Id at parseId 0
            for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
                publishTopics[stationId][parseId] = String.format("%s/%d/%d", publishTopic, stationId, parseId);
                //builder.definePrivateTopic(publishTopics[stationId][parseId], "UART", "VALUE");
            }
        }
    }

    public static String publishTopic(int stationId, int parsedId) {
        return publishTopics[stationId][parsedId];
    }

    public static final MsgField[] messages = new MsgField[] {
            new MsgField("st", integer,"station_num", true, true),
            new MsgField("sn", integer,"valve_sn", true, true),
            new MsgField("cl", integer,"ccl", true, true),
            new MsgField("cc", integer,"cc", true, false),
            new MsgField("pp", floatingPoint,"pp", true, false),
            new MsgField("fd", int64,"fab_date", false, true),
            new MsgField("sd", int64,"ship_date", false, true),
            new MsgField("pf", string,"p_fault", true, false),
            new MsgField("ld", string,"leak", true, false),
            new MsgField("in", string,"input", true, false),
            new MsgField("pn", string,"sku", false, true),
    };

    public static GreenTokenMap buildParser() {
        GreenTokenMap map = new GreenTokenMap();
        for (int i = 0; i < parseIdLimit; i++) {
            map = map.add(i, messages[i].getPattern());
        }
        return map;
    }

    public static final String[] topics = new String[] {
            "StationId",
            "SerialNumber",
            "ProductNumber",
            "CycleCountLimnit",
            "CycleCount",
            "PressurePoint",
            "PressureFault",
            "LeakDetection",
            "InputState",
            "FabricationDate",
            "ShipmentDate",
    };

    public static final String manifoldSerialJsonKey = "manifold_sn";
    public static final String timestampJsonKey = "timestamp";
    public static final String stationsJsonKey = "stations";

    public static final FieldType[] types = new FieldType[] {
            integer,
            integer,
            string,
            integer,
            integer,
            floatingPoint,
            int64,
            int64,
            string,
            string,
            string,
    };
}
