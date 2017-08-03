package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;

public class DecentMessageProducer implements SerialMessageProducer {

    private final static int[] installedStationIds = new int[] { 0, 3, 4, 8, 9};
    private final static String[] inputEnum = new String[] { "A", "B", "N" };
    private final static String[] pressureFaultEnum = new String[] { "H", "L", "N" };
    private final static String[] leakDetectedEnum = new String[] { "P", "C", "N" };

    private final static String[] sizeEnum = new String[] { "SM" };
    private final static String[] colorEnum = new String[] { "BLU" };

    private final Map<Integer, Integer> installedValves = new HashMap<>();
    private final Map<Integer, String> productNumbers = new HashMap<>();
    private final Map<Integer, Integer> cycleCounts = new HashMap<>();
    private final Map<Integer, Integer> cycleCountLimits = new HashMap<>();

    private final Map<Integer, Long> fabricationDates = new HashMap<>();
    private final Map<Integer, Long> shipmentDates = new HashMap<>();

    @Override
    public String next(long time, int i) {

        StringBuilder s = new StringBuilder("[");
        int stationId = installedStationIds[ThreadLocalRandom.current().nextInt(0, installedStationIds.length)];

        for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
            String value = MessageScheme.patterns[parseId].substring(0, 2);
            String datum = calcValue(time, i, parseId, stationId);
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

    private String calcValue(long time, int i, int parseId, int stationId) {
        switch (parseId) {
            case 0: { // StationId
                return Integer.toString(stationId);
            }
            case 1: { // SerialNumber
                return installedValves.computeIfAbsent(stationId, k -> i + 100000).toString();
            }
            case 2: { // ProductNumber
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    return productNumbers.computeIfAbsent(sn, k ->
                            String.format("NX-DCV-%s-%s-%d-V%d-L%d-S%d-00",
                                    sizeEnum[ThreadLocalRandom.current().nextInt(0, sizeEnum.length)],
                                    colorEnum[ThreadLocalRandom.current().nextInt(0, colorEnum.length)],
                                    2,
                                    0,
                                    0,
                                    0));
                }
            }
            case 3: { // CycleCount
                Integer c = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    c = cycleCounts.get(sn);
                    if (c == null) {
                        c = 0;
                    }
                    c += 1;
                    cycleCounts.put(sn, c);
                }
                return c.toString();
            }
            case 4: { // CycleCountLimnit
                Integer l = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    l = cycleCountLimits.computeIfAbsent(sn, k ->
                            ThreadLocalRandom.current().nextInt(100000000, 900000000));
                }
                return l.toString();
            }
            case 5: { // PressurePoint // TODO
                return Integer.toString(parseId * i);
            }
            case 6: { // PressureFault // TODO
                return pressureFaultEnum[ThreadLocalRandom.current().nextInt(0, pressureFaultEnum.length)];
            }
            case 7: { // LeakDetection // TODO
                return leakDetectedEnum[ThreadLocalRandom.current().nextInt(0, leakDetectedEnum.length)];
            }
            case 8: { // InputState// TODO
                return inputEnum[ThreadLocalRandom.current().nextInt(0, inputEnum.length)];
            }
            case 9: { // Fabrication Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = fabricationDates.computeIfAbsent(sn, k -> System.currentTimeMillis()); // TODO
                }
                return date.toString();
            }
            case 10: { // Shipment Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = shipmentDates.computeIfAbsent(sn, k -> System.currentTimeMillis()); // TODO
                }
                return date.toString();
            }
        }
        return "0";
    }
}
