package com.ociweb.behaviors;

import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener {
    private FogCommandChannel cmd;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
        this.cmd.ensureHTTPClientRequesting(10, 2048);
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        String body = messageReader.readUTF();
        System.out.println(String.format("F) %s", body));
        // TODO: send to google - cmd.httpPost("google", 42, )
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader reader) {
        return true;
    }
}
