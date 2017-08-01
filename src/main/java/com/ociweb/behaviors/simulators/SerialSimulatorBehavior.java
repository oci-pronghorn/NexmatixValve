package com.ociweb.behaviors.simulators;

import com.ociweb.gl.api.TimeListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

import static com.ociweb.iot.maker.FogCommandChannel.SERIAL_WRITER;

public class SerialSimulatorBehavior implements TimeListener {
    private final FogCommandChannel channel;
    private final SerialMessageProducer producer = new BasicMessage();

    public SerialSimulatorBehavior(FogRuntime runtime) {
        this.channel = runtime.newCommandChannel(SERIAL_WRITER);
    }

    private final int limit = Integer.MAX_VALUE;

    @Override
    public void timeEvent(long l, int i) {
        if (i >= limit) return;
        channel.publishSerial(serialWriter -> {
            String msg = producer.next(l, i);
            System.out.println(String.format("A.%d) %d:'%s'", i, msg.length(), msg));
            serialWriter.writeUTF8Text(msg);
        });
    }
}
