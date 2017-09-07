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
    private MQTTBridge mqttBridge;
    private String googleProjectId;
    private int manifoldNumber = 0;

    @Override
    public void declareConnections(Hardware builder) {
        builder.useSerial(Baud.B_____9600); //optional device can be set as the second argument
        builder.setTimerPulseRate(1000);

        googleProjectId = builder.getArgumentValue("--project", "-p", "nexmatixmvp-dev");
        manifoldNumber = Integer.parseInt(builder.getArgumentValue("--manifold", "-m", "1"));

        String password = "";
        final String secret =
                "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDObVKljbk+H9v5\n" +
                "/U+XiZ/eNlJA9InAQ9Q++xfsrytFJZ02pp4pvGxD85W6FwkfvTt+CHBooYxwQp4a\n" +
                "ePyHLzDz+2YF5ySX4OB17+N6TNqi5ZvymA/u8Fzj47UdROBlU+L8RiVXgga0RhFv\n" +
                "/ru8+7Uw9XfPhmYjBkUReZxbLSM815iU2ODGTWAtaOlB9JfeoM08co51jk6SF25U\n" +
                "wmwE/3BJH8fSJ9qneM89NLB+TgC0HQclA9ziAcY+GCo94aDxobv0lAbrWjRU4dAy\n" +
                "hSvqhW7+jxDmPGMf7w6Ni9JIpGxMOeziRVma4OHPzPf5+m4Qb6lC84DZsmAvFw3e\n" +
                "10BAYRWBAgMBAAECggEAJo2kcecWQdQRcY8t3k+F3CqpEhiZ4Z7JdTnQLhRJMJDl\n" +
                "298iiwj173r+69KBkbv18IQC+oexgwXuIWOXRmg57Fd3poKVAwis41n6Uk0oSGQ5\n" +
                "zAU6dJXPw4Azw1Op1ULlkdhIAR/3wJOVjiU1SwZ3wL0Xs1qWmNQC8lCUMzMq8aG8\n" +
                "YV4cmAZYkgWEI4vptnWAfJyQT1GyNWx+MJmJCUBYhut8MtCx299cWE8O3dXJsBHZ\n" +
                "vopB2dSNaiGj/daafP3T4kQUK6luBl0Hz69EY/DoAVJRzONZoMoJLw50Kk/K8gcw\n" +
                "CI4zTh22u/0XBz239Km4945jbYIu0brow5LWMjevQQKBgQD6Dfa9X3lPaaotYS31\n" +
                "1Pi4oV08wnMxE3q+ZXTqKZuWeUWyaLtmEJlAU+6YyXOMSouDO38q9XsbVeAZAuyS\n" +
                "b3Z7vRnjJos6ze9r1cEPRBFxtWh7bi+XjvYEpmfKJQj+S3RBdczdHpfpB+0v13Ev\n" +
                "Sgv9c0OndGqn6aCPFniLNoWt/wKBgQDTVc5v5/ApsAqRhhZVsKiawJ/suWwQ9sZx\n" +
                "FUpLQJcDfXJHpLhSMXOh00ieoa51oOCerN1d1/qi5zwfS81lGuz9SWGTi0zufQLT\n" +
                "3coM2uGAaxFvq8/BGxpLTWGJy9k6bietGi+fB88PqwPcH2zRTDXf/QtPnAs6B69/\n" +
                "0X9MMLg8fwKBgF0mooo9bNWWiVzKXPK9WcJ96lveHEdl+E3BQeKRiXJuzvX9agJF\n" +
                "oLGEEtg8A515j6tdmKwKMsgmH4txuWt7tmm2MlSaYTeQy+YiBP+I3e68I6YHkBcn\n" +
                "nKJy0ytMzKLevPo9xgmBghm/aC5wVavGK91I+SUCi1DuCXAEcPd7YiVFAoGBAMP1\n" +
                "v6sSdSYin1oa7GDeoyiDzobx1FvSh2VaKX6n0J+i1bHK8kL8qcz3HlJBd4SI/V8E\n" +
                "yWr4Fuaw5ZXbcwP6OKAQSBNIyrglYNbVxEGxQAIUxaE3vjfACtyiTvw38iB0/gNL\n" +
                "0bZzxjMwDy8wUHWuZhJhD/jsp5hSghBSUOh0EJG/AoGAPLuKcigLkgeAIZfGmfpX\n" +
                "YcmuObryQ5xbxOLBtLfxecIW+qv3B1oM/xjrw5KkG8GHM1RHUI47ByZRPUUJjdzN\n" +
                "3hQdWn1+QthIZa8zvIxu5Fi3MvzudHAFBhP7YmnBD2PKsp29moi5cKlcistrBKbn\n" +
                "GAte+YRDdEBmKLdN9Qkm5rQ=\n" +
                "-----END PRIVATE KEY-----";

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            password = JWT.create()
                    .withIssuer("auth0")
                    .sign(algorithm);
        } catch (Exception exception){
            System.out.print(exception);
        }


        final String deviceId = String.format("manifold-%d", manifoldNumber);
        final String cloudRegion = "us-central1";
        final String registryId = googleProjectId;
        final String broker =  "mqtt.googleapis.com";
        final int port = 8883;
        final String username = "ignored";

        System.out.println(password);

        final String clientId = String.format(
                "projects/%s/locations/%s/registries/%s/devices/%s",
                googleProjectId,
                cloudRegion,
                registryId,
                deviceId);

        this.mqttBridge = builder.useMQTT(broker, port, true, clientId, 64, 32767)
                .cleanSession(true)
                .keepAliveSeconds(10)
                .authentication(username, password);

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
        runtime.registerListener(new UARTMessageToStructBehavior(runtime, manifoldNumber, "DDS_STATUS", true)).addSubscription("UART");
        runtime.registerListener(new UARTMessageToStructBehavior(runtime, manifoldNumber, "DDS_CONFIG", false)).addSubscription("UART");

        // Register Google Pub Sub
        runtime.registerListener(new GooglePubSubBehavior(googleProjectId, runtime, "manifold-state", 1)).addSubscription("JSON_STATUS");
        runtime.registerListener(new GooglePubSubBehavior(googleProjectId, runtime, "manifold-configuration", 60)).addSubscription("JSON_CONFIG");

        runtime.bridgeTransmission("JSON_STATUS", "manifold-state", this.mqttBridge);
        runtime.bridgeTransmission("JSON_CONFIG", "manifold-configuration", this.mqttBridge);
    }
}
