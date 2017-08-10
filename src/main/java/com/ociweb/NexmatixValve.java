package com.ociweb;

import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.iot.maker.*;

public class NexmatixValve implements FogApp
{
    @Override
    public void declareConnections(Hardware builder) {
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.setTimerPulseRate(1000);
        //builder.enableTelemetry();
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
        final int manifoldNumber = Integer.parseInt(runtime.getArgumentValue("--manifold", "-m", "1"));

        // Register the serial simulator
        SerialMessageProducer producer = new DecentMessageProducer(manifoldNumber);
        runtime.registerListener(new SerialSimulatorBehavior(runtime, producer));
        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));
        // Register the json converter
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_STATUS", true)).addSubscription("UART");
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_CONFIG", false)).addSubscription("UART");
        // Register Google Pub Sub
        runtime.registerListener(new GooglePubSubBehavior(runtime, "manifold-state", 1)).addSubscription("JSON_STATUS");
        runtime.registerListener(new GooglePubSubBehavior(runtime, "manifold-configuration", 60)).addSubscription("JSON_CONFIG");
    }
}
