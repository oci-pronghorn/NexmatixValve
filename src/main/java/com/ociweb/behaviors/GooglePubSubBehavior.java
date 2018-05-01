package com.ociweb.behaviors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.ociweb.gl.api.*;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.pubsub.v1.TopicName;
import com.ociweb.pronghorn.pipe.ChannelReader;

import static com.ociweb.schema.MessageScheme.jsonMessageSize;

/*
Must execute uber jar

apt-get install unzip autoconf m4 libtool

#! /bin/bash
wget https://github.com/google/protobuf/releases/download/v2.6.1/protobuf-2.6.1.tar.gz
tar xzf protobuf-2.6.1.tar.gz
cd protobuf-2.6.1
sudo apt-get update
sudo apt-get install build-essential
sudo ./configure
sudo make
sudo make check
sudo make install
sudo ldconfig
protoc --version
 */

public class GooglePubSubBehavior implements PubSubListener, StartupListener, ShutdownListener {
    private final PubSubService service;
    private final String publishTopic;
    private final int interval;
    private final String project;
    private int counter = 0;
    private long lastTime = System.currentTimeMillis();

    private final boolean checkFuture = false;

    public GooglePubSubBehavior(String project, FogRuntime runtime, String publishTopic, int interval) {
        FogCommandChannel channel = runtime.newCommandChannel();
        this.service = channel.newPubSubService(64, jsonMessageSize);
        this.publishTopic = publishTopic;
        this.interval = interval;
        this.project = project;
    }

    @Override
    public boolean message(CharSequence charSequence, ChannelReader messageReader) {
        final String json = messageReader.readUTF();
        if (counter % interval == 0) {
            long thisTime = System.currentTimeMillis();
            long duration = (thisTime - lastTime);
            lastTime = thisTime;
            theGoogleWay(json);
            System.out.println(String.format("D.%s.%d) sent %d", publishTopic, counter, duration));
        }
        else {
            System.out.println(String.format("D.%s.%d) skipped", publishTopic, counter));
        }
        counter++;
        return true;
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
                topicName = TopicName.create(project, publishTopic);
                publisher = Publisher.defaultBuilder(topicName).build();
            }

            // schedule publishing one message at a time : messages get automatically batched
            List<String> messages = Collections.singletonList(body);
            for (String message : messages) {
                ByteString data = ByteString.copyFromUtf8(message);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

                // Once published, returns a server-assigned message id (unique within the topic)
                ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
                if (checkFuture) {
                    messageIdFutures.add(messageIdFuture);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }  finally {
            // wait on any pending publish requests.
            if (checkFuture) {
                try {
                    List<String> messageIds = ApiFutures.allAsList(messageIdFutures).get();
                    for (String messageId : messageIds) {
                        System.out.println(String.format("D.%s.%d) published ID: %s", publishTopic, counter, messageId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
