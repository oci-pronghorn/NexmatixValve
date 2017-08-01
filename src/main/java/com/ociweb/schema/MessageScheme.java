package com.ociweb.schema;
import com.ociweb.pronghorn.util.TrieParser;

import static com.ociweb.schema.FieldType.floatingPoint;
import static com.ociweb.schema.FieldType.integer;
import static com.ociweb.schema.FieldType.string;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(9, 9);

    public static final String[] patterns = new String[] {
            "st%u",
            "sn%u",
            "pn\"%b\"",
            "cc%u",
            "cl%u",
            "pp%i",
            "pf\"%b\"",
            "ld\"%b\"",
            "in\"%b\"",
    };

    public static final String[] topics = new String[] {
            "StationId",
            "SerialNumber",
            "ProductNumber",
            "CycleCount",
            "CycleCountLimnit",
            "PressurePoint",
            "PressureFault",
            "LeakDetection",
            "InputState",
    };

    public static final String[] jsonKeys = new String[] {
            "sn",
            "valve_sn",
            "valve_pn",
            "cc",
            "ccl",
            "pp",
            "p_fault",
            "leak",
            "input",
    };

    public static final String timestampJsonKey = "timestamp";

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
