package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;

public class DecentMessageProducer implements SerialMessageProducer {

    private final static int[] installedStationIds = new int[] { 0, 3, 4, 8, 9 };
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

    private final Map<Integer, Integer> fabricationDates = new HashMap<>();
    private final Map<Integer, Integer> shipmentDates = new HashMap<>();

    private final Map<Integer, Integer> pressureFaults = new HashMap<>();
    private final Map<Integer, Integer> leakFaults = new HashMap<>();

    private static final double minPsi = 0.0;
    private static final double maxPsi = 120.0;

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
            case 3: { // CycleCountLimnit
                Integer l = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    l = cycleCountLimits.computeIfAbsent(sn, k ->
                            ThreadLocalRandom.current().nextInt(100000000, 900000000));
                }
                return l.toString();
            }
            case 4: { // CycleCount
                Integer c = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    c = cycleCounts.get(sn);
                    if (c == null) {
                        if (stationId == installedStationIds[1]) {
                            Integer ccl = cycleCountLimits.get(sn);
                            if (ccl != null) {
                                c = ccl - 10;
                            }
                            else {
                                c = 0;
                            }
                        }
                        else {
                            c = 0;
                        }
                    }
                    c += 1;
                    cycleCounts.put(sn, c);
                }
                return c.toString();
            }
            case 5: { // PressurePoint
                /* TODO:
                Pretty much a square wave. As the valve cycles, the pressure reading goes from a minimum value very close
                to 0 to a maximum value of whatever the customer's compressor is set to (typically 70 to 110 psig is what
                most manufacturers use to operate their machines). Because we are reading data every 5 seconds and the
                valves move so much faster than that, it is unlikely that we capture a point that is not in one of those
                extremes.
                So if you want to create convincing fake data, you could generate a sequence of values that are either in
                the minimum, close to zero, or maximum, close to whatever you chose to use as line pressure.
                After MVP we will explore the possibility of presenting pressure profiles with a much finer detail.
                 */
                double v = time % (Math.PI * 2.0);
                double s = Math.sin(v);
                double c = (s + 1.0) * 0.5;
                double r = minPsi + ((maxPsi - minPsi) * c);
                return Double.toString(r);
            }
            case 6: { // PressureFault
                int idx = 0;
                if (stationId == installedStationIds[2]) {
                    boolean flipIt = i % 4 == 0;
                    Integer sn = installedValves.get(stationId);
                    if (sn != null) {
                        idx = pressureFaults.computeIfAbsent(sn, k -> 0);
                        if (flipIt) {
                            if (idx != 0) {
                                idx = 0;
                            }
                            else {
                                idx = ThreadLocalRandom.current().nextInt(1, pressureFaultEnum.length);
                            }
                            pressureFaults.put(sn, idx);
                        }
                        return pressureFaultEnum[idx];
                    }
                }
                return pressureFaultEnum[idx];
            }
            case 7: { // LeakDetection
                int idx = 0;
                if (stationId == installedStationIds[3]) {
                    boolean flipIt = i % 4 == 0;
                    Integer sn = installedValves.get(stationId);
                    if (sn != null) {
                        idx = leakFaults.computeIfAbsent(sn, k -> 0);
                        if (flipIt) {
                            if (idx != 0) {
                                idx = 0;
                            }
                            else {
                                idx = ThreadLocalRandom.current().nextInt(1, leakDetectedEnum.length);
                            }
                            leakFaults.put(sn, idx);
                        }
                    }
                }
                return leakDetectedEnum[idx];
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
                Integer daysPastY2K = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    daysPastY2K = fabricationDates.computeIfAbsent(sn, k -> {
                        return 6424 - 30 + ThreadLocalRandom.current().nextInt(-5, 6);
                    });
                }
                return daysPastY2K.toString();
            }
            case 10: { // Shipment Date
                Integer daysPastY2K = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    daysPastY2K = shipmentDates.computeIfAbsent(sn, k -> {
                        return 6424 - 10 + ThreadLocalRandom.current().nextInt(-5, 6);
                    });
                }
                return daysPastY2K.toString();
            }
        }
        return "0";
    }
}
