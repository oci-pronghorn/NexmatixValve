package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;

import java.util.concurrent.ThreadLocalRandom;

import static com.ociweb.schema.FieldType.string;

public class DecentMessageProducer implements SerialMessageProducer {

    final static int[] installedStationIds = new int[] { 0, 3, 4, 8, 9};
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
            case 0: {
                int stationId = installedStationIds[ThreadLocalRandom.current().nextInt(0, installedStationIds.length)];
                return Integer.toString(stationId);
            }
            case 1: {
                return Integer.toString(parseId * i);
            }
            case 2: {
                return Integer.toString(parseId * i);
            }
            case 3: {
                return Integer.toString(parseId * i);
            }
            case 4: {
                return Integer.toString(parseId * i);
            }
            case 5: {
                return Integer.toString(parseId * i);
            }
            case 6: {
                return Integer.toString(parseId * i);
            }
            case 7: {
                return Integer.toString(parseId * i);
            }
        }
        return "0";
    }
}
