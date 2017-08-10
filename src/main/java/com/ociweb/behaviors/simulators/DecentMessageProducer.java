package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;
import com.ociweb.schema.MsgField;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;
import static com.ociweb.schema.MessageScheme.stationCount;

public class DecentMessageProducer implements SerialMessageProducer {

    private final int manifoldNumber;
    private final List<Integer> installedStationIds;
    private final int cfIdx;
    private final int pfIdx;
    private final int lfIdx;

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

    private final Map<Integer, Integer> pressureFaults = new HashMap<>();
    private final Map<Integer, Integer> leakFaults = new HashMap<>();

    private static final double minPsi = 0.0;
    private static final double maxPsi = 120.0;

    public DecentMessageProducer(int manifoldNumber) {
        this.manifoldNumber = manifoldNumber;
        installedStationIds = new ArrayList<>();
        final int maxStations = 1; // stationCount;
        for (int i = 0; i < maxStations; i++) {
            //boolean isInstalled = ThreadLocalRandom.current().nextInt(0, 2) == 1;
            //if (isInstalled) {
                installedStationIds.add(i);
            //}
        }

        this.cfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
        this.pfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
        this.lfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
    }

    public int getInstalledCount() {
        return installedStationIds.size();
    }

    @Override
    public String next(long time, int i) {

        StringBuilder s = new StringBuilder("[");
        int stationId = installedStationIds.get(ThreadLocalRandom.current().nextInt(0, installedStationIds.size()));

        for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
            MsgField msgField = MessageScheme.messages[parseId];
            String value = msgField.key;
            String datum = calcValue(time, i, parseId, stationId);
            if (msgField.type == string) {
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
        MsgField msgField = MessageScheme.messages[parseId];

        switch (msgField.key) {
            case "st": { // StationId
                return Integer.toString(stationId);
            }
            case "sn": { // SerialNumber
                return installedValves.computeIfAbsent(stationId, k -> (manifoldNumber * 1000) + (stationId * 10)).toString();
            }
            case "pn": { // ProductNumber
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
            case "cl": { // CycleCountLimnit
                Integer l = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    l = cycleCountLimits.computeIfAbsent(sn, k ->
                            ThreadLocalRandom.current().nextInt(100000000, 900000000));
                }
                return l.toString();
            }
            case "cc": { // CycleCount
                Integer c = 0;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    c = cycleCounts.get(sn);
                    if (c == null) {
                        if (stationId == installedStationIds.get(cfIdx)) {
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
            case "pp": { // PressurePoint
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
            case "fd": { // Fabrication Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = fabricationDates.computeIfAbsent(sn, k -> {
                        //Date d = new Date(2016, 5, 13);
                        //d.setDate(d.getDate() + ThreadLocalRandom.current().nextInt(-5, 6));
                        //return d.getTime();
                        return System.currentTimeMillis();
                    });
                }
                return date.toString();
            }
            case "sd": { // Shipment Date
                Long date = 0L;
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    date = shipmentDates.computeIfAbsent(sn, k -> {
                        //Date d = new Date(2016, 6, 13);
                        //d.setDate(d.getDate() + ThreadLocalRandom.current().nextInt(-5, 6));
                        //Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
                        //return d.getTime();
                        return System.currentTimeMillis();
                    });
                }
                return date.toString();
            }
            case "pf": { // PressureFault
                int idx = 0;
                if (stationId == installedStationIds.get(pfIdx)) {
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
            case "ld": { // LeakDetection
                int idx = 0;
                if (stationId == installedStationIds.get(lfIdx)) {
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
            case "in": { // InputState
                String e = inputEnum[0];
                Integer sn = installedValves.get(stationId);
                if (sn != null) {
                    e = inputStatus.computeIfAbsent(sn, k -> inputEnum[ThreadLocalRandom.current().nextInt(0, inputEnum.length)]);
                }
                return e;
            }
        }
        return "0";
    }
}
