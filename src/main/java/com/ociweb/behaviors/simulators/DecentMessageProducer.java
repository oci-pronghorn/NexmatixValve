package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;

public class DecentMessageProducer implements SerialMessageProducer {

    final static int[] installedStationIds = new int[] { 0, 3, 4, 8, 9};
    final static String[] inputEnum = new String[] { "A", "B", "N" };
    final static String[] pressureFaultEnum = new String[] { "H", "L", "N" };
    final static String[] leakDetectedEnum = new String[] { "P", "C", "N" };

    @Override
    public String next(long time, int i) {

        StringBuilder s = new StringBuilder("[");
        for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
            String value = MessageScheme.patterns[parseId].substring(0, 2);
            String datum = calcValue(time, i, parseId);
            if (MessageScheme.types[parseId] == string) {
                value += "\"" + datum + "\"";
            }
            else {
                value += datum;
            }
            s.append(value);
        }
        s.append("]");
        return s.toString();
    }

    private String calcValue(long time, int i, int parseId) {
        switch (parseId) {
            case 0: { // StationId
                int v = installedStationIds[ThreadLocalRandom.current().nextInt(0, installedStationIds.length)];
                return Integer.toString(v);
            }
            case 1: { // SerialNumber
                return Integer.toString(parseId * i);
            }
            case 2: { // CycleCount
                return Integer.toString(parseId * i);
            }
            case 3: { // CycleCountLimnit
                return Integer.toString(parseId * i);
            }
            case 4: { // PressurePoint
                return Integer.toString(parseId * i);
            }
            case 5: { // PressureFault
                String v = pressureFaultEnum[ThreadLocalRandom.current().nextInt(0, pressureFaultEnum.length)];
                return v;
            }
            case 6: { // LeakDetection
                String v = leakDetectedEnum[ThreadLocalRandom.current().nextInt(0, leakDetectedEnum.length)];
                return v;
            }
            case 7: { // InputState
                String v = inputEnum[ThreadLocalRandom.current().nextInt(0, inputEnum.length)];
                return v;
            }
        }
        return "0";
    }
}
