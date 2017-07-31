package com.ociweb;

import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.schema.MessageScheme;
import com.ociweb.behaviors.*;
import com.ociweb.iot.maker.*;

import static com.ociweb.schema.MessageScheme.parseIdLimit;
import static com.ociweb.schema.MessageScheme.stationCount;

public class NexmatixValve implements FogApp
{
    private MQTTBridge localMqttBridge;
    //private MQTTBridge googleMqttBridge;
    private boolean isSim = false;

    @Override
    public void declareConnections(Hardware builder) {
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.useNetClient();

        localMqttBridge = builder.useMQTT("127.0.0.1", 1883, "NexmatixValve")
                .cleanSession(true)
                .transmissionOoS(1)
                .subscriptionQoS(1)
                .keepAliveSeconds(10);
/*
        googleMqttBridge = builder.useMQTT("127.0.0.1", 1883, "NexmatixValve")
                .cleanSession(true)
                .transmissionOoS(1)
                .subscriptionQoS(1)
                .keepAliveSeconds(10);
*/
        isSim = builder.hasArgument("--sim", "-s");
        if (isSim) {
            builder.setTimerPulseRate(1000);
        }

        builder.enableTelemetry();
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
        if (isSim) {
            runtime.registerListener(new SerialSimulatorBehavior(runtime));
        }

        final String manifoldSerial = runtime.getArgumentValue("--manifold", "-m", "1");
        final String manifoldTopic = "Manifold" + manifoldSerial;

        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));
        // Register the json converter
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldSerial, "JSON")).addSubscription("UART");
        // Register Google Pub Sub
        //runtime.registerListener(new GooglePubSubBehavior(runtime)).addSubscription("JSON");

        // Register the listener that publishes per field in the message
        final FieldPublisherBehavior fields = new FieldPublisherBehavior(runtime, "VALUE");
        runtime.registerListener(fields).addSubscription("UART");
        // For every station and published field
        for (int stationId = 0; stationId < stationCount; stationId++) {
            // Skip Station Id at parseId 0
            for (int parseId = 1; parseId < parseIdLimit; parseId++) {
                // Create a filter for that field
                final FieldFilterBehavior filter = new FieldFilterBehavior(runtime, "FILTER", stationId, parseId);
                final String internalFieldTopic = fields.publishTopic(stationId, parseId);
                runtime.registerListener(filter).addSubscription(internalFieldTopic);
                // Broadcast the value to MQTT transforming the topic
                final String externalTopic = String.format("%s/%d/%s", manifoldTopic, stationId, MessageScheme.topics[parseId]);
                runtime.bridgeTransmission(filter.publishTopic, externalTopic, localMqttBridge); //optional 2 topics, optional transform lambda
            }
        }
    }
}
