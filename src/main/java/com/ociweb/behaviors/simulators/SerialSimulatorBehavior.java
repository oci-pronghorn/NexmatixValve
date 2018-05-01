package com.ociweb.behaviors.simulators;

import com.ociweb.gl.api.PubSubMethodListener;
import com.ociweb.gl.api.TimeListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.iot.maker.SerialService;
import com.ociweb.pronghorn.pipe.ChannelReader;

import java.util.Scanner;

import static com.ociweb.iot.maker.FogCommandChannel.SERIAL_WRITER;

public class SerialSimulatorBehavior implements TimeListener, PubSubMethodListener {
    private final SerialService serialService;
    private final SerialMessageProducer producer;

    public SerialSimulatorBehavior(FogRuntime runtime, SerialMessageProducer producer) {
        this.producer = producer;
        FogCommandChannel channel = runtime.newCommandChannel(SERIAL_WRITER);
        serialService = channel.newSerialService();
    }

    private final int limit = Integer.MAX_VALUE;

    public boolean resetFaults(CharSequence charSequence, ChannelReader messageReader) {
        producer.resetFaults();
        return true;
    }

    public boolean wantPressureFault(CharSequence charSequence, ChannelReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        String v = str.substring(1, 2);
        producer.wantPressureFault(stationId, v);
        return true;
    }

    public boolean wantLeakFault(CharSequence charSequence, ChannelReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        String v = str.substring(1, 2);
        producer.wantLeakFault(stationId, v);
        return true;

    }

    public boolean wantCycleFault(CharSequence charSequence, ChannelReader messageReader) {
        String str = messageReader.readUTFOfLength(messageReader.available());
        int stationId = Integer.parseInt(str.substring(0, 1));
        int cycleCountLimitIn = Integer.parseInt(str.substring(2));
        producer.wantCycleFault(stationId, cycleCountLimitIn);
        return true;
    }

    @Override
    public void timeEvent(long l, int i) {
        if (i >= limit) return;
        serialService.publishSerial(serialWriter -> {
            String msg = producer.next(l, i);
            System.out.println(String.format("A.%d) %d:'%s'", i, msg.length(), msg));
            serialWriter.writeUTF8Text(msg);
        });
    }
}
