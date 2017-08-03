package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;

public class DecentMessageProducer implements SerialMessageProducer {

    private final static int[] installedStationIds = new int[] { 0, 3, 4, 8, 9};
    private final static String[] inputEnum = new String[] { "N", "A", "B" };
    private final static String[] pressureFaultEnum = new String[] { "N", "H", "L" };
    private final static String[] leakDetectedEnum = new String[] { "N", "P", "C" };

    private final static String[] sizeEnum = new String[] { "SM" };
    private final static String[] colorEnum = new String[] { "BLU" };

    private final Map<Integer, Integer> installedValves = new HashMap<>();
    private final Map<Integer, String> productNumbers = new HashMap<>();
    private final Map<Integer, Integer> cycleCounts = new HashMap<>();
    private final Map<Integer, Integer> cycleCountLimits = new HashMap<>();
    private final Map<Integer, String> inputStatus = new HashMap<>();

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
                String e = pressureFaultEnum[0];
                return pressureFaultEnum[ThreadLocalRandom.current().nextInt(0, pressureFaultEnum.length)];
            }
            case 7: { // LeakDetection // TODO
                String e = leakDetectedEnum[0];
                return leakDetectedEnum[ThreadLocalRandom.current().nextInt(0, leakDetectedEnum.length)];
            }
            case 8: { // InputState
                String e = inputEnum[0];
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    e = inputStatus.computeIfAbsent(sn, k -> inputEnum[ThreadLocalRandom.current().nextInt(0, inputEnum.length)]);
                }
                return e;
            }
            case 9: { // Fabrication Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = fabricationDates.computeIfAbsent(sn, k -> {
                        Date d = new Date(2016, 5, 13);
                        d.setDate(d.getDate() + ThreadLocalRandom.current().nextInt(-5, 6));
                        return d.getTime();
                    });
                }
                return date.toString();
            }
            case 10: { // Shipment Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = shipmentDates.computeIfAbsent(sn, k -> {
                        Date d = new Date(2016, 6, 13);
                        d.setDate(d.getDate() + ThreadLocalRandom.current().nextInt(-5, 6));
                        return d.getTime();
                    });
                }
                return date.toString();
            }
        }
        return "0";
    }
}
