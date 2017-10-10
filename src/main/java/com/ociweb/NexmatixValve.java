package com.ociweb;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NexmatixValve implements FogApp
{
    // place native opendds dynamic libs in project current working directory
    static {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String nativeJarName  = null;
            String libExtension = null;
            if (osName.contains("mac")) {
                nativeJarName = new String("OpenDDSDarwin.jar");
                libExtension= new String(".dylib");
            } else if (osName.contains("linux")) {
                nativeJarName = new String("OpenDDSLinux.jar");
                libExtension= new String(".so");
            } else if (osName.contains("Windows")) {
                nativeJarName = new String("OpenDDSWindows.jar");
                libExtension= new String(".dll");
            }

            if (nativeJarName == null) {
                throw new UnsupportedOperationException("No known OpenDDS native jar for OS "+ osName);
            } else {
                System.out.println( nativeJarName + " contains native libaries for " + osName);
            }

            String currentWorkingDirString = Paths.get("").toAbsolutePath().normalize().toString();
            Path jarFilePath = Paths.get(currentWorkingDirString,nativeJarName);
            Files.deleteIfExists(jarFilePath);
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(nativeJarName);
            Files.copy(stream, jarFilePath);
            stream.close();

            if(Files.exists(jarFilePath)) {
                // unpack Jar to cwd
                JarFile jar = new JarFile(jarFilePath.toString());
                for (Enumeration<JarEntry> enumEntries = jar.entries(); enumEntries.hasMoreElements();) {
                    JarEntry entry = enumEntries.nextElement();
                    Path dynamicLibPath = Paths.get(currentWorkingDirString, entry.getName());
                    Files.deleteIfExists(dynamicLibPath);
                    Files.copy(jar.getInputStream(entry), dynamicLibPath);
                }
                jar.close();

                // load dynamic libraries with path to current directory to
                // support rpath location mechanism to resolve to unpacked library path
                String libs[] = {
                        "libACE",
                        "libTAO",
                        "libTAO_AnyTypeCode",
                        "libTAO_PortableServer",
                        "libTAO_CodecFactory",
                        "libTAO_PI",
                        "libTAO_BiDirGIOP",
                        "libidl2jni_runtime",
                        "libtao_java",
                        "libOpenDDS_Dcps",
                        "libOpenDDS_Udp",
                        "libOpenDDS_Tcp",
                        "libOpenDDS_Rtps",
                        "libOpenDDS_Rtps_Udp",
                        "libOpenDDS_DCPS_Java"
                };
                for (String lib: libs) {
                    System.out.println("Loading: " + lib);
                    System.load(Paths.get(currentWorkingDirString, lib+libExtension).toAbsolutePath().normalize().toString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

        //runtime.registerListener(new DDSBroadcastValve()).addSubscription("DDS");
        //runtime.bridgeTransmission("DDS", this.ddsBridge);
    }

}
