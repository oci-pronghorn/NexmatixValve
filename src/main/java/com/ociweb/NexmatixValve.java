package com.ociweb;


import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTConfig;
import com.ociweb.iot.maker.*;

import java.util.Objects;

import static com.ociweb.MessageScheme.stationCount;

public class NexmatixValve implements FogApp
{
    private MQTTConfig mqttConfig;
    private final String manifoldTopic = "Manifold1";

    @Override
    public void declareConnections(Hardware builder) {
       builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
       mqttConfig = builder.useMQTT("127.0.0.1", 1883, "NexmatixValve")
                .cleanSession(true)
                .transmissionOoS(1)
                .subscriptionQoS(1)
                .keepAliveSeconds(10);
        //builder.enableTelemetry(true);
        //if (builder.args() != null && builder.args().length > 0 && builder.args()[0].equals("sim")) {
            builder.setTimerPulseRate(1000);
        //}
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {

        //if (runtime.args() != null && runtime.args().length > 0 && runtime.args()[0].equals("sim")) {
            runtime.registerListener(new SerialSimulatorBehavior(runtime));
        //}
        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));
        runtime.subscriptionBridge("foobar", mqttConfig);
        // Register the listener that publishes per field in the message
        final FieldPublisherBehavior fields = new FieldPublisherBehavior(runtime, "VALUE");
        runtime.registerListener(fields).addSubscription("UART");
        // For every station and published field
        for (int stationId = 0; stationId < stationCount; stationId++) {
            for (int valueId = 1; valueId < MessageScheme.topics.length; valueId++) {
                // Create a filter for that field
                final FieldFilterBehavior filter = new FieldFilterBehavior(runtime, "FILTER", stationId, valueId);
                runtime.registerListener(filter).addSubscription(fields.publishTopics[stationId][valueId]);
                // Broadcast the value to MQTT transforming the topic
                final String mqttTopic = String.format("%s/%d/%s", manifoldTopic, stationId, MessageScheme.topics[valueId]);
                runtime.transmissionBridge(filter.publishTopic, mqttTopic, mqttConfig); //optional 2 topics, optional transform lambda
            }
        }
    }
}
