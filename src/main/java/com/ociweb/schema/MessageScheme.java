package com.ociweb.schema;
import com.ociweb.pronghorn.util.TrieParser;

import static com.ociweb.schema.FieldType.floatingPoint;
import static com.ociweb.schema.FieldType.integer;
import static com.ociweb.schema.FieldType.string;

public class MessageScheme {
    public static final int messageSize = 256;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(8, 8);

    public static String[] patterns = new String[] {
            "st%u",
            "sn%u",
            "inp\"%b\"",
            "cc%u",
            "ccl%u",
            "pp%i",
            "pf\"%b\"",
            "ld\"%b\"",
    };

    public static String[] topics = new String[] {
            "StationId",
            "SerialNumber",
            "InputState",
            "CycleCount",
            "CycleCountLimnit",
            "PressurePoint",
            "PressureFault",
            "LeakDetection",
    };

    public static String[] jsonKeys = new String[] {
            "sn",
            "valve_sn",
            "input",
            "cc",
            "ccl",
            "pp",
            "p_fault",
            "leak",
    };

    public static FieldType[] types = new FieldType[] {
            integer,
            integer,
            string,
            integer,
            integer,
            floatingPoint,
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
