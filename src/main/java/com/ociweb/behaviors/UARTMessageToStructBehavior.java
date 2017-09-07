package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;

public class UARTMessageToStructBehavior  implements PubSubListener {
    private final FogCommandChannel cmd;

    public UARTMessageToStructBehavior(FogRuntime runtime, int manifoldNumber, String publishTopic, boolean isStatus) {
        this.cmd = runtime.newCommandChannel();
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        return true;
    }
}
