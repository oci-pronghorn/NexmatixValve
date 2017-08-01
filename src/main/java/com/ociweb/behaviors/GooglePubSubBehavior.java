package com.ociweb.behaviors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.pubsub.v1.TopicName;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener {
    private final FogCommandChannel cmd;
    private final String url;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel(NET_REQUESTER);
        this.cmd.ensureHTTPClientRequesting(10, 2048);
        this.url = String.format("https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish", "nexmatixmvp", "manifold-state");
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final String json = messageReader.readUTF();

        StringBuilder builder = new StringBuilder();
        builder.append("{\"messages\": [{\"attributes\": {\"key\": \"iana.org/language_tag\", \"value\": \"en\"},\"data\": \"");
        byte[] bytes = json.getBytes();
        Appendables.appendBase64(builder, bytes, 0, bytes.length, Integer.MAX_VALUE);
        builder.append("\"}]}");
        String body = builder.toString();

        System.out.println(String.format("F) %s", body));

        //theFoglightWay(body);
        //theOldWay(body);
        theGoogleWay(json);
        return true;
    }

    @Override
    public boolean responseHTTP(HTTPResponseReader reader) {
        return true;
    }

    private void theFoglightWay(String body) {
        cmd.httpPost(url, 80, "", blobWriter -> {
            blobWriter.write(body.getBytes());
        });
    }

    // Must execute "gcloud auth application-default login" on command line
    // And the user you select have permissions
    private void theGoogleWay(String body) {

        Publisher publisher = null;
        List<ApiFuture<String>> messageIdFutures = new ArrayList<>();
        try {
            final TopicName topicName;
            topicName = TopicName.create("nexmatixmvp", "manifold-state");

            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.defaultBuilder(topicName).build();

            List<String> messages = Collections.singletonList(body);

            // schedule publishing one message at a time : messages get automatically batched
            for (String message : messages) {
                ByteString data = ByteString.copyFromUtf8(message);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

                // Once published, returns a server-assigned message id (unique within the topic)
                ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
                messageIdFutures.add(messageIdFuture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // wait on any pending publish requests.
            List<String> messageIds = null;
            try {
                messageIds = ApiFutures.allAsList(messageIdFutures).get();

                for (String messageId : messageIds) {
                    System.out.println("published with message ID: " + messageId);
                }

                if (publisher != null) {
                    // When finished with the publisher, shutdown to free up resources.
                    publisher.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void theOldWay(String body) {
        // final String USER_AGENT = "Mozilla/5.0";
        try {
            URL obj = null;
            obj = new URL(url);
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
