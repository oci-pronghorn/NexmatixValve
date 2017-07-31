package com.ociweb.behaviors;

import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.Appendables;

import java.io.IOException;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener {
    private final FogCommandChannel cmd;
    private final String url;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
        this.cmd.ensureHTTPClientRequesting(10, 2048);

        // https://pubsub.googleapis.com/v1/projects/myproject/topics/mytopic:publish
        this.url = String.format("https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish", "nexamatixmvp", "manifold-state");
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final String json = messageReader.readUTF();
        /*{
              "messages": [
                {
                  "attributes": {
                    "key": "iana.org/language_tag",
                    "value": "en"
                  },
                  "data": "SGVsbG8gQ2xvdWQgUHViL1N1YiEgSGVyZSBpcyBteSBtZXNzYWdlIQ=="
                }
              ]
            }
         */

        StringBuilder builder = new StringBuilder();
        builder.append("{\"messages\": [{\"attributes\": {\"key\": \"iana.org/language_tag\", \"value\": \"en\"},\"data\": \"");
        try {
            Appendables.appendBase64(builder, json.getBytes(),0, json.getBytes().length, Integer.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.append("\"}]}");

        System.out.println(String.format("F) %s", builder.toString()));

        //cmd.httpPost(url, 80);
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader reader) {
        return true;
    }
}
