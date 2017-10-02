package com.ociweb.schema;
import Nexmatix.ValveData;
import com.ociweb.NexmatixValve;
import com.ociweb.gl.api.GreenTokenMap;

import java.util.function.BiConsumer;

import static com.ociweb.schema.FieldType.*;

public class MessageScheme {
    public static final int messageSize = 512;
    public static final int jsonMessageSize = 16384;
    public static final int stationCount = 10;
    public static final int parseIdLimit = Math.min(11, 11);

    public static final BiConsumer<ValveData, Object> stationIdConsumer = (valveData, stationId) -> {
        valveData.stationId = (int)stationId;
    };

    public static final BiConsumer<ValveData, Object> pressureFaultConsumer = (valveData, pressureFault) ->  {
        valveData.pressureFault = valveData.pressureFault.from_int((int)pressureFault);
    };

    public static final BiConsumer<ValveData, Object> cyclesConsumer = (valveData, cycleCount) -> {
        valveData.cycles = (int)cycleCount;
    };

    // TODO: inject setValve
    // TODO: add new fields
    /*
        // "st": StationId
        // "sn": SerialNumber
        // "pn": ProductNumber
        // "cl": CycleCountLimit
        // "cc": CycleCount
        // "pp": PressurePoint
        // "fd": Fabrication Date
        // "sd": Shipment Date
        // "pf": PressureFault
        // "ld": LeakDetection
        // "in": InputState

    Nexmatix::ValveData {
        public int manifoldId;                   // nil
        public int stationId;                    // "st" -> stationIdConsumer
        public int valveSerialId;                // nil
        public String partNumber;                // nil
        public boolean leakFault;                // nil
        public PresureFault pressureFault;       // "pf" -> pressureFaultConsumer
        public boolean valveFault;               // nil
        public int cycles;                       // "cc" -> cyclesConsumer
        public int pressure;                     // nil
        public int durationLast12;               // nil
        public int durationLast14;               // nil
        public int equalizationAveragePressure;  // nil
        public int equalizationPressureRate;     // nil
        public int residualOfDynamicAnalysis;    // nil
        public int suppliedPressure;             // nil
    }
    */

    public static final MsgField[] messages = new MsgField[]{
            new MsgField("st", integer, "station_num", true, true, null),
            new MsgField("sn", integer, "valve_sn", true, true, null),
            new MsgField("cl", integer, "ccl", true, true, null),
            new MsgField("cc", integer, "cc", true, false, null),
            new MsgField("pp", floatingPoint, "pp", true, false, null),
            new MsgField("fd", int64, "fab_date", false, true, null),
            new MsgField("sd", int64, "ship_date", false, true, null),
            new MsgField("pf", string, "p_fault", true, false, null),
            new MsgField("ld", string, "leak", true, false, null),
            new MsgField("in", string, "input", true, false, null),
            new MsgField("pn", string, "sku", false, true, null),

            new MsgField("st", integer, "stationId", false, false, stationIdConsumer),
            new MsgField("pf", integer, "pressureFault", false, false, pressureFaultConsumer),
            new MsgField("cc", integer, "cycleCount", false, false, cyclesConsumer),
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
