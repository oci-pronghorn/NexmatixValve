package com.ociweb;

import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;
import com.ociweb.schema.MessageScheme;

public class NexmatixValve implements FogApp  {
    private MQTTBridge controlBridge;
    private int manifoldNumber;

    @Override
    public void declareConnections(Hardware builder) {

        // Read command line args
        manifoldNumber = builder.getArgumentValue("--manifold", "-m", 1);
        final String broker = builder.getArgumentValue("--broker", "-b", "localhost");
        final long rateInMS = builder.getArgumentValue("--rate", "-r", 1000);

        // Create MQTT Client
        this.controlBridge = builder.useMQTT(
                broker,
                MQTTBridge.defaultPort,
                "nexmatix" + manifoldNumber)
                .cleanSession(true)
                .keepAliveSeconds(0);

        // Setup serial port
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument

        // Set message broadcast rate
        builder.setTimerPulseRate(rateInMS);

        builder.enableTelemetry();

        // Setup private topics
        //builder.definePrivateTopic("UART", "UART", "VALUE");
        MessageScheme.declareTopics(manifoldNumber, s -> {
            //builder.definePrivateTopic(s, "UART", "VALUE");
        });
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
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
        UARTMessageWindowBehavior UART = new UARTMessageWindowBehavior(runtime, MessageScheme.UARTTopic);
        runtime.registerListener(MessageScheme.UARTTopic, UART);

        // Register the listener that publishes per field in the message
        final FieldPublisherBehavior fields = new FieldPublisherBehavior(runtime);
        runtime.registerListener(MessageScheme.FieldTopic, fields).addSubscription(MessageScheme.UARTTopic);

        // For every station and published field
        for (int stationId = 0; stationId < MessageScheme.stationCount; stationId++) {
            // Skip Station Id at parseId 0
            for (int parseId = 1; parseId < MessageScheme.parseIdLimit; parseId++) {
                // Create a filter for that field
                final String internalFieldTopic = MessageScheme.internalPublishTopic(stationId, parseId);
                final String filterFieldTopic = MessageScheme.filterPublishTopic(stationId, parseId);
                final String externallFieldTopic =  MessageScheme.externalPublishTopic(stationId, parseId);

                final FieldFilterBehavior filter = new FieldFilterBehavior(runtime, filterFieldTopic, parseId);
                final String name = String.format("%d.%s", stationId, MessageScheme.messages[parseId].mqttKey);
                runtime.registerListener(name, filter).addSubscription(internalFieldTopic);
                // Broadcast the value to MQTT transforming the topic
                runtime.bridgeTransmission(filter.publishTopic, externallFieldTopic, controlBridge); //optional 2 topics, optional transform lambda
            }
        }
    }
}
