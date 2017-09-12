package com.ociweb;

import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;

public class NexmatixValve implements FogApp
{
    private MQTTBridge controlBridge;

    @Override
    public void declareConnections(Hardware builder) {

        final int manifoldNumber = Integer.parseInt(builder.getArgumentValue("--manifold", "-m", "1"));

        this.controlBridge = builder.useMQTT(
                "localhost",
                MQTTBridge.defaultPort,
                false,
                "nexmatix" + manifoldNumber)
                .cleanSession(true)
                .keepAliveSeconds(0);

        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.setTimerPulseRate(1000);
        //builder.enableTelemetry();
    }

    @Override
    public void declareBehavior(FogRuntime runtime) {
        final int manifoldNumber = Integer.parseInt(runtime.getArgumentValue("--manifold", "-m", "1"));
        final String project = runtime.getArgumentValue("--project", "-p", "nexmatixmvp-dev");

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
        int installedCount = producer.getInstalledCount();
        SerialSimulatorBehavior serialSim = new SerialSimulatorBehavior(runtime, producer);
        runtime.registerListener(serialSim)
                .addSubscription(fr, serialSim::resetFaults)
                .addSubscription(fp, serialSim::wantPressureFault)
                .addSubscription(fl, serialSim::wantLeakFault)
                .addSubscription(fc, serialSim::wantCycleFault);

        // Register the serial listener that chunks the messages
        runtime.registerListener(new UARTMessageWindowBehavior(runtime, "UART"));

        // Register the json converter
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_STATUS", true, installedCount)).addSubscription("UART");
        runtime.registerListener(new UARTMessageToJsonBehavior(runtime, manifoldNumber, "JSON_CONFIG", false, installedCount)).addSubscription("UART");

        // Register Google Pub Sub
        runtime.registerListener(new GooglePubSubBehavior(project, runtime, "manifold-state", 1)).addSubscription("JSON_STATUS");
        runtime.registerListener(new GooglePubSubBehavior(project, runtime, "manifold-configuration", 60)).addSubscription("JSON_CONFIG");
    }
}
