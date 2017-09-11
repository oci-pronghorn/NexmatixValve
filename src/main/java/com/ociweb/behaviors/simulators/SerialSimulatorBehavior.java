package com.ociweb.behaviors.simulators;

import com.ociweb.gl.api.PubSubMethodListener;
import com.ociweb.gl.api.TimeListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.pipe.BlobWriter;

import java.util.Scanner;

import static com.ociweb.iot.maker.FogCommandChannel.SERIAL_WRITER;

public class SerialSimulatorBehavior implements TimeListener, PubSubMethodListener {
    private final FogCommandChannel channel;
    private final SerialMessageProducer producer;

    public SerialSimulatorBehavior(FogRuntime runtime, SerialMessageProducer producer) {
        this.producer = producer;
        this.channel = runtime.newCommandChannel(SERIAL_WRITER);
    }

    private final int limit = Integer.MAX_VALUE;

    public boolean wantPressureFault(CharSequence charSequence, BlobReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        char v = str.substring(1, 2).charAt(0);
        producer.wantPressureFault(stationId, v);
        return true;
    }

    public boolean wantLeakFault(CharSequence charSequence, BlobReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        char v = str.substring(1, 2).charAt(0);
        producer.wantLeakFault(stationId, v);
        return true;

    }

    public boolean wantCycleFault(CharSequence charSequence, BlobReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        int cycleCountLimitIn = Integer.parseInt(str.substring(2));
        producer.wantCycleFault(stationId, cycleCountLimitIn);
        return true;
    }

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
