package com.ociweb.behaviors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import com.ociweb.gl.api.*;
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
import java.util.Collections;
import java.util.List;

import com.google.pubsub.v1.TopicName;

import static com.ociweb.schema.MessageScheme.jsonMessageSize;

public class GooglePubSubBehavior implements PubSubListener, HTTPResponseListener, StartupListener, ShutdownListener {
    private final FogCommandChannel cmd;
    private final String url;

    public GooglePubSubBehavior(FogRuntime runtime) {
        this.cmd = runtime.newCommandChannel();
        this.cmd.ensureDynamicMessaging(64, jsonMessageSize);
        //this.cmd.ensureHTTPClientRequesting(10, jsonMessageSize);
        this.url = String.format("https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish", "nexmatixmvp", "manifold-state");
    }

    @Override
    public boolean message(CharSequence charSequence, BlobReader messageReader) {
        final String json = messageReader.readUTF();

       // StringBuilder builder = new StringBuilder();
        //builder.append("{\"messages\": [{\"attributes\": {\"key\": \"iana.org/language_tag\", \"value\": \"en\"},\"data\": \"");
       // byte[] bytes = json.getBytes();
       // Appendables.appendBase64(builder, bytes, 0, bytes.length, Integer.MAX_VALUE);
       // builder.append("\"}]}");
       // String body = builder.toString();
      //  System.out.println(String.format("F) %s", body));

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

    private TopicName topicName = null;
    private Publisher publisher = null;

    @Override
    public void startup() {
        // creating publisher in constructor freezes app
        // creating publisher here blocks for too long
    }

    @Override
    public boolean acceptShutdown() {
        // When finished with the publisher, shutdown to free up resources.
        try {
            if (publisher != null) {
                publisher.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    // Must execute "gcloud auth application-default login" on command line
    // And the user you select have permissions
    private void theGoogleWay(String body) {
        List<ApiFuture<String>> messageIdFutures = new ArrayList<>();
        try {
            // Create a publisher instance with default settings bound to the topic
            // This takes a long time!!!
            if (topicName == null) {
                topicName = TopicName.create("nexmatixmvp", "manifold-state");
                publisher = Publisher.defaultBuilder(topicName).build();
            }

            // schedule publishing one message at a time : messages get automatically batched
            List<String> messages = Collections.singletonList(body);
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
            try {
                List<String> messageIds = ApiFutures.allAsList(messageIdFutures).get();
                for (String messageId : messageIds) {
                    System.out.println("F) published ID: " + messageId);
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
