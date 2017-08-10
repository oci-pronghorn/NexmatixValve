package com.ociweb.schema;
import com.ociweb.pronghorn.util.TrieParser;

import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(11, 11);

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

    public static TrieParser buildParser() {
        TrieParser tp = new TrieParser(messageSize,1,false,true);
        tp.setMaxNumericLengthCapturable(16);
        tp.setMaxBytesCapturable(36);
        for (int i = 0; i < parseIdLimit; i++) {
            final String pattern = messages[i].getPattern();
            tp.setUTF8Value(pattern, i);
        }
        return tp;
    }

    public static final String manifoldSerialJsonKey = "manifold_sn";
    public static final String timestampJsonKey = "timestamp";
    public static final String stationsJsonKey = "stations";
}
