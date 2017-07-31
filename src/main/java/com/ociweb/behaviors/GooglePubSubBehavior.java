package com.ociweb.behaviors;

import com.ociweb.gl.api.HTTPResponseListener;
import com.ociweb.gl.api.HTTPResponseReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.pronghorn.util.Appendables;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener {
    private final FogCommandChannel cmd;
    private final String url;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
        this.cmd.ensureHTTPClientRequesting(10, 2048);

        // https://pubsub.googleapis.com/v1/projects/myproject/topics/mytopic:publish
        this.url = String.format("https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish", "nexmatixmvp", "manifold-state");
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
        try {
            theOldWay(url, builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader reader) {
        return true;
    }

    private void theOldWay(String url, String body) throws IOException {
       // final String USER_AGENT = "Mozilla/5.0";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        //con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        //String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        //wr.writeBytes(urlParameters);
        wr.writeBytes(body);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
    }
}
