package com.ociweb;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ociweb.behaviors.simulators.DecentMessageProducer;
import com.ociweb.behaviors.simulators.SerialSimulatorBehavior;
import com.ociweb.behaviors.*;
import com.ociweb.gl.api.MQTTBridge;
import com.ociweb.iot.maker.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NexmatixValve implements FogApp
{
    // place native opendds dynamic libs in project current working directory
    static {
        try {
            final String osName = System.getProperty("os.name").toLowerCase();
            String nativeJarName  = null;
            if (osName.contains("mac")) {
                nativeJarName = "OpenDDSDarwin.jar";
            } else if (osName.contains("linux")) {
                nativeJarName = "OpenDDSLinux.jar";
            } else if (osName.contains("Windows")) {
                nativeJarName = "OpenDDSWindows.jar";
            }

            if (nativeJarName == null) {
                throw new UnsupportedOperationException("No known OpenDDS native jar for OS "+ osName);
            } else {
                System.out.println( nativeJarName + " contains native libaries for " + osName);
            }

            final String currentWorkingDirString = Paths.get("").toAbsolutePath().normalize().toString();
            final Path jarFilePath = Paths.get(currentWorkingDirString, nativeJarName);
            Files.deleteIfExists(jarFilePath);
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(nativeJarName);
            Files.copy(stream, jarFilePath);
            stream.close();

            if(Files.exists(jarFilePath)) {
                // unpack Jar to cwd
                Map<String, String> libFileNameMap = new HashMap<>();
                JarFile jar = new JarFile(jarFilePath.toString());
                for (Enumeration<JarEntry> enumEntries = jar.entries(); enumEntries.hasMoreElements();) {
                    final JarEntry entry = enumEntries.nextElement();
                    final int startIndex = 0;
                    final int endIndex = entry.getName().indexOf(".");
                    final String key = entry.getName().substring(startIndex, endIndex).toLowerCase();
                    final Path dynamicLibPath = Paths.get(currentWorkingDirString, entry.getName());
                    libFileNameMap.put(key, entry.getName());
                    Files.deleteIfExists(dynamicLibPath);
                    Files.copy(jar.getInputStream(entry), dynamicLibPath);
                }
                jar.close();

                //libFileNameMap.forEach((id, val) -> System.out.println(id + ":" + val));

                // load dynamic libraries with path to current directory to
                // support rpath location mechanism to resolve to unpacked library path
                final String libs[] = {
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
                    final String key = lib.toLowerCase();
                    if (libFileNameMap.containsKey(key)) {
                        final String libFileName = libFileNameMap.get(key);
                        System.out.println("Loading: " + libFileName);
                        System.load(Paths.get(currentWorkingDirString, libFileName).toAbsolutePath().normalize().toString());
                    } else {
                        System.out.println("Skipping: " + lib);
                    }
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
