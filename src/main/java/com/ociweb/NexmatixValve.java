package com.ociweb;

import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;
import com.ociweb.schema.MessageScheme;

public class NexmatixValve implements FogApp
{
    private MQTTBridge controlBridge;

    @Override
    public void declareConnections(Hardware builder) {

        final int manifoldNumber = builder.getArgumentValue("--manifold", "-m", 1);
        final String broker = builder.getArgumentValue("--broker", "-b", "localhost");

        this.controlBridge = builder.useMQTT(
                broker,
                MQTTBridge.defaultPort,
                "nexmatix" + manifoldNumber)
                .cleanSession(true)
                .keepAliveSeconds(0);

        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.setTimerPulseRate(1000);
        builder.enableTelemetry();

        //builder.definePrivateTopic("UART", "UART", "VALUE");
        MessageScheme.declareConnections(builder, "VALUE");
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
        final int manifoldNumber = Integer.parseInt(runtime.getArgumentValue("--manifold", "-m", "1"));
        //final String project = runtime.getArgumentValue("--project", "-p", "nexmatixmvp-dev");
        final String manifoldTopic = "Manifold" + manifoldNumber;

        final String prefix = "m" + manifoldNumber + "/";
        final String fr = "fault/reset";
        final String fp = "fault/pressure";
        final String fl = "fault/leak";
        final String fc = "fault/cycle";

        runtime.bridgeSubscription(fr, prefix + fr, controlBridge);
        runtime.bridgeSubscription(fp, prefix + fp, controlBridge);
        runtime.bridgeSubscription(fl, prefix + fl, controlBridge);
        runtime.bridgeSubscription(fc, prefix + fc, controlBridge);

        // Register the serial simulator
        DecentMessageProducer producer = new DecentMessageProducer(manifoldNumber, false);

        SerialSimulatorBehavior serialSim = new SerialSimulatorBehavior(runtime, producer);
        runtime.registerListener(serialSim)
                .addSubscription(fr, serialSim::resetFaults)
                .addSubscription(fp, serialSim::wantPressureFault)
                .addSubscription(fl, serialSim::wantLeakFault)
                .addSubscription(fc, serialSim::wantCycleFault);

        // Register the serial listener that chunks the messages
        runtime.registerListener("UART", new UARTMessageWindowBehavior(runtime, "UART"));

        // Register the listener that publishes per field in the message
        final FieldPublisherBehavior fields = new FieldPublisherBehavior(runtime);
        runtime.registerListener("VALUE", fields).addSubscription("UART");

        // For every station and published field
        for (int stationId = 0; stationId < MessageScheme.stationCount; stationId++) {
            // Skip Station Id at parseId 0
            for (int parseId = 1; parseId < MessageScheme.parseIdLimit; parseId++) {
                // Create a filter for that field
                final FieldFilterBehavior filter = new FieldFilterBehavior(runtime, "FILTER", stationId, parseId);
                final String internalFieldTopic = MessageScheme.publishTopic(stationId, parseId);
                runtime.registerListener(filter).addSubscription(internalFieldTopic);

                // Broadcast the value to MQTT transforming the topic
                final String externalTopic = String.format("%s/%d/%s", manifoldTopic, stationId, MessageScheme.topics[parseId]);
                runtime.bridgeTransmission(filter.publishTopic, externalTopic, controlBridge); //optional 2 topics, optional transform lambda
            }
        }
    }
}
