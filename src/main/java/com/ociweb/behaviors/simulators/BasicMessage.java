package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;

import static com.ociweb.schema.FieldType.string;

class BasicMessage implements SerialMessageProducer {
    private String msg = completeMessage();
    private static String completeMessage() {
        StringBuilder s = new StringBuilder("[");
        for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
            String value = MessageScheme.patterns[parseId].substring(0, 2);
            if (MessageScheme.types[parseId] == string) {
                value += "\"" + (parseId * 10) + "\"";
            }
            else {
                value += (parseId * 10);
            }
            s.append(value);
        }
        s.append("]");
        return s.toString();
    }

    @Override
    public String next(long aLong, int integer) {
        return msg;
    }
}
