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
    private final ValveConfig config;
    private final GreenReader reader = MessageScheme.buildParser().newReader();

    public UARTMessageToStructBehavior(FogRuntime runtime, int manifoldNumber, String publishTopic) {
        this.cmd = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.manifoldNumber = manifoldNumber;
        this.publishTopic = publishTopic;
        this.config = new ValveConfig();
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        final long timeStamp = blobReader.readLong();
        final short messageLength = blobReader.readShort();
        // TODO: populate this.config
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
                        msgField.setValve.accept(config, value);
                        break;
                    }
                    case int64: {
                        long value = reader.extractedLong(0);
                        msgField.setValve.accept(config, value);
                        break;
                    }
                    case string: {
                        StringBuilder builder = new StringBuilder();
                        reader.copyExtractedUTF8ToAppendable(0, builder);
                        msgField.setValve.accept(config, builder.toString());
                        break;
                    }
                    case floatingPoint: {
                        double value = reader.extractedDouble(0);
                        msgField.setValve.accept(config, value);
                        break;
                    }
                }
            }
        }

        cmd.publishTopic(publishTopic, blobWriter -> {blobWriter.writeObject(config);});
        return true;
    }
}
