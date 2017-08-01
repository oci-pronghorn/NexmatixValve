package com.ociweb.behaviors;
import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.Appendables;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener {
    private final FogCommandChannel cmd;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
        this.cmd.ensureHTTPClientRequesting(10, 2048);
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final String json = messageReader.readUTF();
        StringBuilder builder = new StringBuilder();
        builder.append("{\"messages\": [{\"attributes\": {\"key\": \"iana.org/language_tag\", \"value\": \"en\"},\"data\": \"");
        Appendables.appendBase64(builder, json.getBytes(),0, json.getBytes().length, Integer.MAX_VALUE);
        builder.append("\"}]}");
        System.out.println(String.format("F) %s", builder.toString()));
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader reader) {
        return true;
    }
}
