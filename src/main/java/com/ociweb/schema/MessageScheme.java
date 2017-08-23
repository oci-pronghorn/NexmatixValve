package com.ociweb.schema;
import com.ociweb.gl.api.GreenTokenMap;

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

    public static GreenTokenMap buildParser() {
        GreenTokenMap map = new GreenTokenMap();
        for (int i = 0; i < parseIdLimit; i++) {
            map = map.add(i, messages[i].getPattern());
        }
        return map;
    }

    public static final String manifoldSerialJsonKey = "manifold_sn";
    public static final String timestampJsonKey = "timestamp";
    public static final String stationsJsonKey = "stations";
    public static final String googleProjectName = "nexmatixmvp-dev";
}
