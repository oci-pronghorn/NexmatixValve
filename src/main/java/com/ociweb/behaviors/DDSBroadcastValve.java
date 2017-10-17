package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.gl.api.ShutdownListener;
import com.ociweb.gl.api.StartupListener;
import com.ociweb.pronghorn.pipe.ChannelReader;
import OpenDDS.DCPS.*;
import DDS.*;
import com.sun.org.apache.bcel.internal.generic.RET;
import org.omg.CORBA.StringSeqHolder;
import Nexmatix.*;
import java.util.ArrayList;
import java.util.List;

public class DDSBroadcastValve implements PubSubListener, StartupListener, ShutdownListener {

    private static final int VAlVE_DOMAIN_ID = 23;
    private static final String VALVE_TOPIC = "Valve";

    private DomainParticipantFactory domainParticipantFactory = null;
    private DomainParticipant domainParticipant = null;
    private DataWriter dataWriter = null;
    private DataWriterQosHolder dataWriterQosHolder = null;
    private Publisher publisher = null;
    private Topic topic = null;
    private ValveDataTypeSupportImpl valveDataTypeSupport = null;

    @Override
    public void startup() {
        final long startTime = System.currentTimeMillis();

        // -DCPSBit 0 -DCPSConfigFile rtps_disc.ini
        List<String> list = new ArrayList<String>();
        list.add("-DCPSBit");
        list.add("0");
        list.add("-DCPSConfigFile");
        list.add("rtps_disc.ini");
        String[] stringArray = list.toArray(new String[0]);

        domainParticipantFactory = TheParticipantFactory.WithArgs(new StringSeqHolder(stringArray));
        if (domainParticipantFactory == null) {
            System.err.println("ERROR: Domain Participant Factory not found: " + (System.currentTimeMillis() - startTime));
            return;
        }

        domainParticipant = domainParticipantFactory.create_participant(
                VAlVE_DOMAIN_ID,
                PARTICIPANT_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );

        if (domainParticipant == null) {
            System.err.println("ERROR: Domain Participant creation failed: " + (System.currentTimeMillis() - startTime));
            return;
        }

        valveDataTypeSupport = new ValveDataTypeSupportImpl();
        if (valveDataTypeSupport == null) {
            System.err.println("ERROR: new ValveDataTypeSupportImpl: " + (System.currentTimeMillis() - startTime));
            return;
        }

        if (valveDataTypeSupport.register_type(domainParticipant, "") != RETCODE_OK.value) {
            System.err.println("ERROR: register_type failed: " + (System.currentTimeMillis() - startTime));
            return;
        }

        topic = domainParticipant.create_topic(
                VALVE_TOPIC,
                valveDataTypeSupport.get_type_name(),
                TOPIC_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );
        if (topic == null) {
            System.err.println("ERROR: Topic creation failed: " + (System.currentTimeMillis() - startTime));
            return;
        }

        publisher = domainParticipant.create_publisher(
                PUBLISHER_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );
        if (publisher == null) {
            System.err.println("ERROR: Publisher creation failed: " + (System.currentTimeMillis() - startTime));
            return;
        }

        setPublisherQOS(publisher);

        //if (true == setPublisherQOS(publisher)){ }
        dataWriter = publisher.create_datawriter(
            topic,
            DATAWRITER_QOS_DEFAULT.get(),
            null,
            DEFAULT_STATUS_MASK.value
        );
        if (dataWriter != null) {
            System.out.println("Created dataWriter: " + (System.currentTimeMillis() - startTime));
        } else {
            System.err.println("ERROR: DataWriter creation failed");
        }
        System.out.println("Total execution time: " + (System.currentTimeMillis() - startTime) );
    }

    private boolean setPublisherQOS(Publisher publisher) {
        if (publisher != null) {
            // Use the default transport configuration (do nothing)

            DataWriterQos dataWriterQos = new DataWriterQos();

            dataWriterQos.durability = new DurabilityQosPolicy();
            dataWriterQos.durability.kind = DurabilityQosPolicyKind.from_int(0);

            dataWriterQos.durability_service = new DurabilityServiceQosPolicy();
            dataWriterQos.durability_service.history_kind = HistoryQosPolicyKind.from_int(0);
            dataWriterQos.durability_service.service_cleanup_delay = new Duration_t();

            dataWriterQos.deadline = new DeadlineQosPolicy();
            dataWriterQos.deadline.period = new Duration_t();

            dataWriterQos.latency_budget = new LatencyBudgetQosPolicy();
            dataWriterQos.latency_budget.duration = new Duration_t();

            dataWriterQos.liveliness = new LivelinessQosPolicy();
            dataWriterQos.liveliness.kind = LivelinessQosPolicyKind.from_int(0);
            dataWriterQos.liveliness.lease_duration = new Duration_t();

            dataWriterQos.reliability = new ReliabilityQosPolicy();
            dataWriterQos.reliability.kind = ReliabilityQosPolicyKind.from_int(0);
            dataWriterQos.reliability.max_blocking_time = new Duration_t();

            dataWriterQos.destination_order = new DestinationOrderQosPolicy();
            dataWriterQos.destination_order.kind = DestinationOrderQosPolicyKind.from_int(0);

            dataWriterQos.history = new HistoryQosPolicy();
            dataWriterQos.history.kind = HistoryQosPolicyKind.from_int(0);

            dataWriterQos.resource_limits = new ResourceLimitsQosPolicy();
            dataWriterQos.transport_priority = new TransportPriorityQosPolicy();

            dataWriterQos.lifespan = new LifespanQosPolicy();
            dataWriterQos.lifespan.duration = new Duration_t();

            dataWriterQos.user_data = new UserDataQosPolicy();
            dataWriterQos.user_data.value = new byte[0];

            dataWriterQos.ownership = new OwnershipQosPolicy();
            dataWriterQos.ownership.kind = OwnershipQosPolicyKind.from_int(0);

            dataWriterQos.ownership_strength = new OwnershipStrengthQosPolicy();
            dataWriterQos.writer_data_lifecycle = new WriterDataLifecycleQosPolicy();

            dataWriterQosHolder = new DataWriterQosHolder(dataWriterQos);
            if (dataWriterQosHolder != null) {
                //Need to pass dataWriterQosHolder will non null members due to in/out semantics of IDL call
                publisher.get_default_datawriter_qos(dataWriterQosHolder);
                dataWriterQosHolder.value.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
                dataWriterQosHolder.value.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
                return true;
            }
        }
        return false;
    }
    private void initValveData(ValveData valveData){
//            public int manifoldId;                   // nil
//            public int stationId;                    // "st"
//            public int valveSerialId;                // nil
//            public String partNumber;                // nil
//            public boolean leakFault;                // nil
//            public PresureFault pressureFault;       // "pf"
//            public boolean valveFault;               // nil
//            public int cycles;                       // "cc"
//            public int pressure;                     // nil
//            public int durationLast12;               // nil
//            public int durationLast14;               // nil
//            public int equalizationAveragePressure;  // nil
//            public int equalizationPressureRate;     // nil
//            public int residualOfDynamicAnalysis;    // nil
//            public int suppliedPressure;             // nil

        valveData.manifoldId = 1;
        valveData.stationId  = 2;
        valveData.valveSerialId = 3;
        valveData.partNumber = "1234";
        valveData.leakFault = false;
        valveData.pressureFault = PresureFault.NO_FAULT;
        valveData.cycles = 4;
        valveData.pressure = 5;
        valveData.durationLast12 =6;
        valveData.durationLast14 =7;
        valveData.equalizationAveragePressure =8;
        valveData.residualOfDynamicAnalysis =9;
        valveData.suppliedPressure =10;
    }
    private void printValveData(ValveData valveData) {
        System.out.println("manifoldId:" + valveData.manifoldId);
        System.out.println("stationId:" + valveData.stationId);
        System.out.println("valveSerialId:" + valveData.valveSerialId);
        System.out.println("partNumber:" + valveData.partNumber);
        System.out.println("leakFault:" + valveData.leakFault);
        System.out.println("pressureFault:" + valveData.pressureFault.toString());
        System.out.println("cycles:" + valveData.cycles);
        System.out.println("pressure:" + valveData.pressure);
        System.out.println("durationLast12:" + valveData.durationLast12);
        System.out.println("durationLast14:" + valveData.durationLast14);
        System.out.println("equalizationAveragePressure:" + valveData.equalizationAveragePressure);
        System.out.println("residualOfDynamicAnalysis:" + valveData.residualOfDynamicAnalysis);
        System.out.println("suppliedPressure:" + valveData.suppliedPressure);
    }

    public boolean message(CharSequence charSequence, ChannelReader channelReader) {
        boolean rc = false;
        if (this.dataWriter != null) {
            ValveData valveData = (ValveData)channelReader.readObject();
            initValveData(valveData);
            //printValveData(valveData);
            ValveDataDataWriter valveDataDataWriter = ValveDataDataWriterHelper.narrow(this.dataWriter);

            if (valveDataDataWriter != null) {

                int handle = valveDataDataWriter.register_instance(valveData);
                System.out.println("register_instance:" + handle);

                int write_rc = valveDataDataWriter.write(valveData, handle);
                switch(write_rc){
                    case RETCODE_OK.value:
                        System.out.println("valveDataDataWriter.write returned:" + "RETCODE_OK");
                        rc = true;
                        break;
                    case RETCODE_TIMEOUT.value:
                        System.out.println("valveDataDataWriter.write returned:" + "RETCODE_TIMEOUT");
                        break;
                    default:
                        System.out.println("valveDataDataWriter.write returned:" + write_rc);
                        break;
                }
            }
        }
        return rc;
    }

    @Override
    public boolean acceptShutdown() {
        boolean rc = false;
        System.out.println("Publisher exiting");
        // Clean up
        if (domainParticipant != null) {
            domainParticipant.delete_contained_entities();
            domainParticipantFactory.delete_participant(domainParticipant);
            rc = true;
        }
        TheServiceParticipant.shutdown();
        return rc;
    }
}

