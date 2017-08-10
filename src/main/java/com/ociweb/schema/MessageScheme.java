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
            "fd%u",
            "sd%u",
            "pf\"%b\"",
            "ld\"%b\"",
            "in\"%b\"",
    };

    public static final String[] jsonKeys = new String[] {
            "station_num",
            "valve_sn",
            "sku",
            "ccl",
            "cc",
            "pp",
            "fab_date",
            "ship_date",
            "p_fault",
            "leak",
            "input",
    };

    public static final String manifoldSerialJsonKey = "manifold_sn";
    public static final String timestampJsonKey = "timestamp";
    public static final String stationsJsonKey = "stations";

    public static final boolean[] statusField = new boolean[] {
            true,
            true,
            false,
            true,
            true,
            true,
            false,
            false,
            true,
            true,
            true,
    };

    public static final boolean[] configField = new boolean[] {
            true,
            true,
            true,
            true,
            false,
            false,
            true,
            true,
            false,
            false,
            false,
    };

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
