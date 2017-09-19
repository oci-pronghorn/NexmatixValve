package com.ociweb.schema;
import com.ociweb.gl.api.GreenTokenMap;

import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(11, 11);

    // TODO: inject setValve
    // TODO: add new fields
    public static final MsgField[] messages = new MsgField[] {
            new MsgField("st", integer,"station_num", true, true, null),
            new MsgField("sn", integer,"valve_sn", true, true, null),
            new MsgField("cl", integer,"ccl", true, true, null),
            new MsgField("cc", integer,"cc", true, false, null),
            new MsgField("pp", floatingPoint,"pp", true, false, null),
            new MsgField("fd", int64,"fab_date", false, true, null),
            new MsgField("sd", int64,"ship_date", false, true, null),
            new MsgField("pf", string,"p_fault", true, false, null),
            new MsgField("ld", string,"leak", true, false, null),
            new MsgField("in", string,"input", true, false, null),
            new MsgField("pn", string,"sku", false, true, null),
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
}
