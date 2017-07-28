package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;

public class GooglePubSubBehavior implements PubSubListener {
    private FogCommandChannel cmd;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        String body = messageReader.readUTF();
        // TODO: send to google - cmd.httpPost("google", 42, )
        return true;
    }
}
