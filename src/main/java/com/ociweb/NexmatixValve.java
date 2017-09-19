package com.ociweb;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;

public class NexmatixValve implements FogApp
{
    //private DDSBridge ddsBridge;
    private String googleProjectId;
    private int manifoldNumber = 0;

    @Override
    public void declareConnections(Hardware builder) {
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.setTimerPulseRate(1000);

        manifoldNumber = Integer.parseInt(builder.getArgumentValue("--manifold", "-m", "1"));

        //this.ddsBridge = builder.useDDS();

        builder.enableTelemetry();
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {

        // Register the serial simulator
        DecentMessageProducer producer = new DecentMessageProducer(manifoldNumber);
        int installedCount = producer.getInstalledCount();
        runtime.registerListener(new SerialSimulatorBehavior(runtime, producer));
        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));

        // Register the json converter
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_STATUS", true, installedCount)).addSubscription("UART");
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_CONFIG", false, installedCount)).addSubscription("UART");

        // Register the DDS converter
        runtime.registerListener(new UARTMessageToStructBehavior(runtime, manifoldNumber, "DDS")).addSubscription("UART");

        // Register Google Pub Sub
        runtime.registerListener(new GooglePubSubBehavior(googleProjectId, runtime, "manifold-state", 1)).addSubscription("JSON_STATUS");
        runtime.registerListener(new GooglePubSubBehavior(googleProjectId, runtime, "manifold-configuration", 60)).addSubscription("JSON_CONFIG");

        runtime.registerListener(new DDSBroadcastValve()).addSubscription("DDS");
        //runtime.bridgeTransmission("DDS", this.ddsBridge);
    }
}
