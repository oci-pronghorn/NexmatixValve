package com.ociweb.behaviors.simulators;

import com.ociweb.schema.MessageScheme;
import com.ociweb.schema.MsgField;

import static com.ociweb.schema.FieldType.string;

class BasicMessageProducer implements SerialMessageProducer {
    private String msg = completeMessage();
    private static String completeMessage() {
        StringBuilder s = new StringBuilder("[");
        for (int parseId = 0; parseId < MessageScheme.parseIdLimit; parseId++) {
            MsgField msgField = MessageScheme.messages[parseId];
            String value = msgField.key;
            if (msgField.type == string) {
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
    public void wantPressureFault() {
    }

    @Override
    public void wantLeakFault() {
    }

    @Override
    public void wantCycleFault() {
    }

    @Override
    public String next(long aLong, int integer) {
        return msg;
    }
}
