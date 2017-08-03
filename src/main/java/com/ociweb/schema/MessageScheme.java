package com.ociweb.schema;
import com.ociweb.pronghorn.util.TrieParser;

import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(11, 11);

    public static final String[] patterns = new String[] {
            "st%u",
            "sn%u",
            "pn\"%b\"",
            "cl%u",
            "cc%u",
            "pp%i",
            "pf\"%b\"",
            "ld\"%b\"",
            "in\"%b\"",
            "fd%u",
            "sd%u",
    };

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

    public static final String[] jsonKeys = new String[] {
            "station_num",
            "valve_sn",
            "sku",
            "ccl",
            "cc",
            "pp",
            "p_fault",
            "leak",
            "input",
            "fab_date",
            "ship_date",
    };

    public static final String manifoldSerialJsonKey = "manifold_sn";
    public static final String timestampJsonKey = "update_time";
    public static final String stationsJsonKey = "stations";

    public static final boolean[] statusField = new boolean[] {
            true,
            true,
            false,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
    };

    public static final boolean[] configField = new boolean[] {
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
    };

    public static final FieldType[] types = new FieldType[] {
            integer,
            integer,
            string,
            integer,
            integer,
            floatingPoint,
            string,
            string,
            string,
            integer,
            integer,
    };

    public static TrieParser buildParser() {
        TrieParser tp = new TrieParser(messageSize,1,false,true);
        tp.setMaxNumericLengthCapturable(16);
        tp.setMaxBytesCapturable(36);
        for (int i = 0; i < parseIdLimit; i++) {
            tp.setUTF8Value(patterns[i], i);
        }
        return tp;
    }
}
