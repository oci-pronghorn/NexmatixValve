package com.ociweb.behaviors;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/*
Cloud Pub/Sub
Cloud Pub/Sub Client Libraries

This page shows how to get started with the new Cloud Client Libraries for the Google Cloud Pub/Sub API. However, we recommend using the older Google APIs Client Libraries if running on Google App Engine standard environment. Read more about the client libraries for Cloud APIs in Client Libraries Explained.

Beta
This is a Beta release of the Cloud Client Libraries for the Google Cloud Pub/Sub API. These libraries might be changed in backward-incompatible ways and are not subject to any SLA or deprecation policy.
Installing the client library

C#GOJAVANODE.JSPHPPYTHONRUBY
If you are using Maven, add this to your pom.xml file:
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-pubsub</artifactId>
    <version>0.20.1-beta</version>
</dependency>
If you are using Gradle, add this to your dependencies:

compile group: 'com.google.cloud', name: 'google-cloud-pubsub', version: '0.20.1-beta'
Using the client library

The following example shows how to use the client library. To run it on your local workstation you must first install the Google Cloud SDK and authenticate by running the following command:

gcloud auth application-default login
For information about authenticating in other environments, see setting up authentication for server to server production applications.
 */

public class GooglePubSubBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final FieldType fieldType;
    private final String projectId;
    private final TopicName topicName;

    public GooglePubSubBehavior(FogRuntime runtime, String publishTopic, int parseId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.fieldType = MessageScheme.types[parseId];
        this.projectId = ServiceOptions.getDefaultProjectId();
        this.topicName = TopicName.create(projectId, publishTopic);

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
            topicAdminClient.createTopic(topicName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        Publisher publisher = null;
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.defaultBuilder(topicName).build();

            List<String> messages = Arrays.asList("first message", "second message");

            // schedule publishing one message at a time : messages get automatically batched
            for (String message : messages) {
                ByteString data = ByteString.copyFromUtf8(message);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
                // Once published, returns a server-assigned message id (unique within the topic)
                ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            }
        } catch (IOException ignored) {
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                try {
                    publisher.shutdown();
                } catch (Exception ignored) {
                }
            }
        }
        return true;
    }
}
