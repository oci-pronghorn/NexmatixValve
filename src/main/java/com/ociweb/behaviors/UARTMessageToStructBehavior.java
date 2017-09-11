package com.ociweb.behaviors;

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
    private final boolean isStatus;
    private final ValveStatus recycleStatus;
    private final ValveConfig recycleConfig;
    private final Object recycleMe;
    private final GreenReader reader = MessageScheme.buildParser().newReader();

    public UARTMessageToStructBehavior(FogRuntime runtime, int manifoldNumber, String publishTopic, boolean isStatus) {
        this.cmd = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.manifoldNumber = manifoldNumber;
        this.publishTopic = publishTopic;
        this.isStatus = isStatus;
        this.recycleStatus = isStatus ? new ValveStatus() : null;
        this.recycleConfig = !isStatus ? new ValveConfig() : null;
        this.recycleMe = isStatus ? recycleStatus : recycleConfig;
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
                if (isStatus && !msgField.isStatus) continue;
                if (!isStatus && !msgField.isConfig) continue;

                String key = msgField.jsonKey;
                final FieldType fieldType = msgField.type;
                switch (fieldType) {
                    case integer: {
                        int value = (int) reader.extractedLong(0);
                        break;
                    }
                    case int64: {
                        long value = reader.extractedLong(0);
                        break;
                    }
                    case string: {
                        StringBuilder builder = new StringBuilder();
                        reader.copyExtractedUTF8ToAppendable(0, builder);
                        break;
                    }
                    case floatingPoint: {
                        double value = reader.extractedDouble(0);
                        break;
                    }
                }
            }
        }

        cmd.publishTopic(publishTopic, blobWriter -> {blobWriter.writeObject(recycleMe);});
        return true;
    }
}
