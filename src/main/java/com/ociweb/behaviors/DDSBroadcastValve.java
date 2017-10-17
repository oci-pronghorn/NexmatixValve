package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.gl.api.ShutdownListener;
import com.ociweb.gl.api.StartupListener;
import com.ociweb.pronghorn.pipe.ChannelReader;
import OpenDDS.DCPS.*;
import DDS.*;
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

    public boolean message(CharSequence charSequence, ChannelReader channelReader) {
        if (this.dataWriter != null) {
            ValveData valveData = (ValveData)channelReader.readObject();
            ValveDataDataWriter valveDataDataWriter = ValveDataDataWriterHelper.narrow(this.dataWriter);
            if (valveDataDataWriter != null) {
                // TODO: fix this
                //int handle = valveDataDataWriter.register_instance(valveData);
                //int rc = valveDataDataWriter.write(valveData, 0);
                int rc = RETCODE_OK.value;
                if (RETCODE_OK.value == rc) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean acceptShutdown() {
        System.out.println("Publisher exiting");
        // Clean up
        if (domainParticipant != null) {
            domainParticipant.delete_contained_entities();
            domainParticipantFactory.delete_participant(domainParticipant);
        }
        TheServiceParticipant.shutdown();
        return true;
    }
}

