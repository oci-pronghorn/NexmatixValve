package com.ociweb.behaviors;

import Nexmatix.ValveData;
import com.ociweb.gl.api.GreenReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.schema.*;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class UARTMessageToStructBehavior  implements PubSubListener {
    private final FogCommandChannel cmd;
    private final int manifoldNumber;
    private final String publishTopic;
    private final ValveData valveData;
    private final GreenReader reader = MessageScheme.buildParser().newReader();

    /*
    Nexmatix::ValveData {
        // "st": StationId
        // "sn": SerialNumber
        // "pn": ProductNumber
        // "cl": CycleCountLimit
        // "cc": CycleCount
        // "pp": PressurePoint
        // "fd": Fabrication Date
        // "sd": Shipment Date
        // "pf": PressureFault
        // "ld": LeakDetection
        // "in": InputState

        public int manifoldId;                   // nil
        public int stationId;                    // "st"
        public int valveSerialId;                // nil
        public String partNumber;                // nil
        public boolean leakFault;                // nil
        public PresureFault pressureFault;       // "pf"
        public boolean valveFault;               // nil
        public int cycles;                       // "cc"
        public int pressure;                     // nil
        public int durationLast12;               // nil
        public int durationLast14;               // nil
        public int equalizationAveragePressure;  // nil
        public int equalizationPressureRate;     // nil
        public int residualOfDynamicAnalysis;    // nil
        public int suppliedPressure;             // nil
    }
    */

    public UARTMessageToStructBehavior(FogRuntime runtime, int manifoldNumber, String publishTopic) {
        this.cmd = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.manifoldNumber = manifoldNumber;
        this.publishTopic = publishTopic;
        this.valveData = new ValveData();
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        final long timeStamp = blobReader.readLong();
        final short messageLength = blobReader.readShort();
        while (reader.hasMore()) {
            int parsedId = (int)reader.readToken();
            if (parsedId == -1) {
                reader.skipByte();
            }
            else {
                MsgField msgField = MessageScheme.messages[parsedId];
                if (msgField.setValve == null) continue;

                String key = msgField.jsonKey;
                final FieldType fieldType = msgField.type;
                switch (fieldType) {
                    case integer: {
                        int value = (int) reader.extractedLong(0);
                        msgField.setValve.accept(valveData, value);
                        break;
                    }
                    case int64: {
                        long value = reader.extractedLong(0);
                        msgField.setValve.accept(valveData, value);
                        break;
                    }
                    case string: {
                        StringBuilder builder = new StringBuilder();
                        reader.copyExtractedUTF8ToAppendable(0, builder);
                        msgField.setValve.accept(valveData, builder.toString());
                        break;
                    }
                    case floatingPoint: {
                        double value = reader.extractedDouble(0);
                        msgField.setValve.accept(valveData, value);
                        break;
                    }
                }
            }
        }

        cmd.publishTopic(publishTopic, blobWriter -> {
            blobWriter.writeObject(valveData);
        });

        return true;
    }
}
