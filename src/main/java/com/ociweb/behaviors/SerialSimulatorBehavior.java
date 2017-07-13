package com.ociweb.behaviors;

import com.ociweb.MessageScheme;
import com.ociweb.gl.api.TimeListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

import static com.ociweb.iot.maker.FogCommandChannel.SERIAL_WRITER;

public class SerialSimulatorBehavior implements TimeListener {
    private final FogCommandChannel channel;
    private int msgIndex = 0;

    private static final String[] msgs = new String[] {
            "[]", // empty
            "garbage[st", // begin
            "3]garbage", // end
            "[garbage]", // garbage
            "[st10]", // illegal
            completeMessage(),
    };

    private static String completeMessage() {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < MessageScheme.patterns.length; i++) {
            String value = MessageScheme.patterns[i].substring(0, 2);
            if (MessageScheme.types[i] == 1) {
                value += "\"" + i + "\"";
            }
            else {
                value += i;
            }
            s.append(value);
        }
        s.append("]");
        return s.toString();
    }

    public SerialSimulatorBehavior(FogRuntime runtime) {
        this.channel = runtime.newCommandChannel(SERIAL_WRITER);
    }

    @Override
    public void timeEvent(long l, int i) {
        channel.publishSerial(serialWriter -> {
            System.out.println(String.format("A) '%s'", msgs[msgIndex]));
            serialWriter.writeUTF8Text(msgs[msgIndex]);
            msgIndex++;
            if (msgIndex == msgs.length) {
                msgIndex = 0;
            }
        });
    }
}
