package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;
import com.ociweb.schema.MsgField;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static com.ociweb.schema.FieldType.string;
import static com.ociweb.schema.MessageScheme.stationCount;

class DecentMessageState implements java.io.Serializable {
    final List<Integer> installedStationIds;
    final int cfIdx;
    final int pfIdx;
    final int lfIdx;

    final Map<Integer, Integer> installedValves = new HashMap<>();
    final Map<Integer, String> productNumbers = new HashMap<>();
    final Map<Integer, Integer> cycleCounts = new HashMap<>();
    final Map<Integer, Integer> cycleCountLimits = new HashMap<>();
    final Map<Integer, String> inputStatus = new HashMap<>();

    final Map<Integer, Long> fabricationDates = new HashMap<>();
    final Map<Integer, Long> shipmentDates = new HashMap<>();

    final Map<Integer, Integer> pressureFaults = new HashMap<>();
    final Map<Integer, Integer> leakFaults = new HashMap<>();

    DecentMessageState() {
        installedStationIds = new ArrayList<>();
        final int maxStations = stationCount;
        for (int i = 0; i < maxStations; i++) {
            boolean isInstalled = ThreadLocalRandom.current().nextInt(0, 5) == 1;
            if (isInstalled) {
                installedStationIds.add(i);
            }
        }

        this.cfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
        this.pfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
        this.lfIdx = ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
    }
}

public class DecentMessageProducer implements SerialMessageProducer {

    private final static String[] inputEnum = new String[] { "N", "A", "B" };
    private final static String[] pressureFaultEnum = new String[] { "N", "H", "L" };
    private final static String[] leakDetectedEnum = new String[] { "N", "P", "C" };
    private final static String[] sizeEnum = new String[] { "SM" };
    private final static String[] colorEnum = new String[] { "BLU" };
    private final static double minPsi = 0.0;
    private final static double maxPsi = 120.0;

    private final int manifoldNumber;
    private final DecentMessageState s;

    public DecentMessageProducer(int manifoldNumber) {
        this.manifoldNumber = manifoldNumber;

        DecentMessageState state;
        try {
            FileInputStream fileIn = new FileInputStream("Manifold" + manifoldNumber);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            state = (DecentMessageState) in.readObject();
            in.close();
            fileIn.close();
        }
        catch(IOException | ClassNotFoundException i) {
            state = new DecentMessageState();
        }
        this.s = state;
    }

    public int getInstalledCount() {
        return s.installedStationIds.size();
    }

    private void write() {
        try {
            FileOutputStream fileOut = new FileOutputStream("Manifold" + manifoldNumber);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(s);
            out.close();
            fileOut.close();
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    @Override
    public String next(long time, int i) {

        StringBuilder str = new StringBuilder("[");
        final int idx = (i + s.installedStationIds.size()) % s.installedStationIds.size(); ////ThreadLocalRandom.current().nextInt(0, installedStationIds.size());
        int stationId = s.installedStationIds.get(idx);

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
            str.append(value);
        }
        str.append("]");
        write();
        return str.toString();
    }

    private String calcValue(long time, int i, int parseId, int stationId) {
        MsgField msgField = MessageScheme.messages[parseId];

        switch (msgField.key) {
            case "st": { // StationId
                return Integer.toString(stationId);
            }
            case "sn": { // SerialNumber
                return s.installedValves.computeIfAbsent(stationId, k -> (manifoldNumber * 1000) + (stationId * 10)).toString();
            }
            case "pn": { // ProductNumber
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    return s.productNumbers.computeIfAbsent(sn, k ->
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
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    l = s.cycleCountLimits.computeIfAbsent(sn, k ->
                            ThreadLocalRandom.current().nextInt(100000000, 900000000));
                }
                return l.toString();
            }
            case "cc": { // CycleCount
                Integer c = 0;
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    c = s.cycleCounts.get(sn);
                    if (c == null) {
                        if (stationId == s.installedStationIds.get(s.cfIdx)) {
                            Integer ccl = s.cycleCountLimits.get(sn);
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
                    s.cycleCounts.put(sn, c);
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
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    date = s.fabricationDates.computeIfAbsent(sn, k -> System.currentTimeMillis() - 31536000000L);
                }
                return date.toString();
            }
            case "sd": { // Shipment Date
                Long date = 0L;
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    date = s.shipmentDates.computeIfAbsent(sn, k -> System.currentTimeMillis() - 31536000000L + 172800000);
                }
                return date.toString();
            }
            case "pf": { // PressureFault
                int idx = 0;
                if (stationId == s.installedStationIds.get(s.pfIdx)) {
                    boolean flipIt = i % 4 == 0;
                    Integer sn = s.installedValves.get(stationId);
                    if (sn != null) {
                        idx = s.pressureFaults.computeIfAbsent(sn, k -> 0);
                        if (flipIt) {
                            if (idx != 0) {
                                idx = 0;
                            }
                            else {
                                idx = ThreadLocalRandom.current().nextInt(1, pressureFaultEnum.length);
                            }
                            System.out.println(String.format("*) Pressure Fault %d, %d %s", stationId + 1, sn, pressureFaultEnum[idx]));
                            s.pressureFaults.put(sn, idx);
                        }
                        return pressureFaultEnum[idx];
                    }
                }
                return pressureFaultEnum[idx];
            }
            case "ld": { // LeakDetection
                int idx = 0;
                if (stationId == s.installedStationIds.get(s.lfIdx)) {
                    boolean flipIt = i % 4 == 0;
                    Integer sn = s.installedValves.get(stationId);
                    if (sn != null) {
                        idx = s.leakFaults.computeIfAbsent(sn, k -> 0);
                        if (flipIt) {
                            if (idx != 0) {
                                idx = 0;
                            }
                            else {
                                idx = ThreadLocalRandom.current().nextInt(1, leakDetectedEnum.length);
                            }
                            System.out.println(String.format("*) Leak Fault %d %d %s", stationId + 1, sn, leakDetectedEnum[idx]));
                            s.leakFaults.put(sn, idx);
                        }
                    }
                }
                return leakDetectedEnum[idx];
            }
            case "in": { // InputState
                String e = inputEnum[0];
                Integer sn = s.installedValves.get(stationId);
                if (sn != null) {
                    e = s.inputStatus.computeIfAbsent(sn, k -> inputEnum[ThreadLocalRandom.current().nextInt(0, inputEnum.length)]);
                }
                return e;
            }
        }
        return "0";
    }
}
