package com.ociweb;

import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.schema.MessageScheme;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTConfig;
import com.ociweb.iot.maker.*;

import static com.ociweb.schema.MessageScheme.parseIdLimit;
import static com.ociweb.schema.MessageScheme.stationCount;

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
        builder.enableTelemetry();
        if (builder.args() != null && builder.args().length > 0 && builder.args()[0].equals("sim")) {
            builder.setTimerPulseRate(1000);
        }
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {

        if (runtime.args() != null && runtime.args().length > 0 && runtime.args()[0].equals("sim")) {
            runtime.registerListener(new SerialSimulatorBehavior(runtime));
        }
        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));
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
                runtime.transmissionBridge(filter.publishTopic, externalTopic, mqttConfig); //optional 2 topics, optional transform lambda
            }
        }
    }
}
