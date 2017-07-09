package com.ociweb;
import com.ociweb.pronghorn.util.TrieParser;

public class MessageScheme {
    private static String[] patterns = new String[] {
            "st%u",
            "sn%i",
            "pn\"%b\"",
            "cc%i",
            "sp%i",
            "pp%i",
            "da%i",
            "db%i",
            "ap%i",
            "ep%i",
            "lr%i",
            "vf%i",
            "pf\"%b\"",
            "lf%i"
    };

    public static String[] topics = new String[] {
            "StationId",
            "SerialNumber",
            "ProductNumber",
            "CycleCount",
            "SupplyPressure",
            "PressurePoint",
            "DurationOfLast1_4Signal",
            "DurationOfLast1_2Signal",
            "EqualizationAveragePressure",
            "EqualizationPressureRate",
            "ResidualOfDynamicAnalysis",
            "ValveFault",
            "PressureFault",
            "LeakFault",
    };

    public static int[] types = new int[] {
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
    };

    public static TrieParser buildParser() {
        TrieParser tp = new TrieParser(256,1,false,true);
        tp.setMaxNumericLengthCapturable(16);
        tp.setMaxBytesCapturable(36);
        for (int i = 0; i < patterns.length; i++) {
            tp.setUTF8Value(patterns[i], i);
        }
        return tp;
    }
}
