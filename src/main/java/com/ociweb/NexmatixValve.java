package com.ociweb;


import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTConfig;
import com.ociweb.iot.maker.*;

public class NexmatixValve implements FogApp
{
    private MQTTConfig mqttConfig;
    private final String manifoldTopic = "Manifold1";

    @Override
    public void declareConnections(Hardware builder) {
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        mqttConfig = builder.useMQTT("127.0.0.1", 1883, "NexmatixValve")
                .cleanSession(true)
                .transmissionOoS(2)
                .subscriptionQoS(2)
                .keepAliveSeconds(10);
        builder.limitThreads();
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "uart", 128));
        // Register the listener that publishes per field in the message
        runtime.registerListener(new FieldPublisherBehavior(runtime, "value", 128)).addSubscription("uart");
        // For every station and published field
        for (int station = 0; station < 10; station++) {
            for (int valueTopic = 1; valueTopic < MessageScheme.topics.length; valueTopic++) {
                // Create a filter for that field
                runtime.registerListener(new FieldFilterBehavior(runtime, "filtered", valueTopic)).addSubscription(
                        String.format("value/%d/%d", station, valueTopic));
                // Broadcast the value to MQTT transforming the topic
                runtime.transmissionBridge(
                        String.format("filtered/%d/%d", station, valueTopic),
                        String.format("%s/%d/%s", manifoldTopic, station, MessageScheme.topics[valueTopic]),
                                mqttConfig); //optional 2 topics, optional transform lambda
            }
        }
    }
}
