package com.ociweb.schema;
import com.ociweb.pronghorn.util.TrieParser;

import static com.ociweb.schema.FieldType.integer;
import static com.ociweb.schema.FieldType.string;

public class MessageScheme {
    public static final int messageSize = 256;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(14, 14);

    public static String[] patterns = new String[] {
            "st%u",
            "sn%i",
            "pn\"%b\"",
            "pf\"%b\"",
            "cc%i",
            "sp%i",
            "pp%i",
            "da%i",
            "db%i",
            "ap%i",
            "ep%i",
            "lr%i",
            "vf%i",
            "lf%i"
    };

    public static String[] topics = new String[] {
            "StationId",
            "SerialNumber",
            "ProductNumber",
            "PressureFault",
            "CycleCount",
            "SupplyPressure",
            "PressurePoint",
            "DurationOfLast1_4Signal",
            "DurationOfLast1_2Signal",
            "EqualizationAveragePressure",
            "EqualizationPressureRate",
            "ResidualOfDynamicAnalysis",
            "ValveFault",
            "LeakFault",
    };

    public static FieldType[] types = new FieldType[] {
            integer,
            integer,
            string,
            string,
            integer,
            integer,
            integer,
            integer,
            integer,
            integer,
            integer,
            integer,
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
