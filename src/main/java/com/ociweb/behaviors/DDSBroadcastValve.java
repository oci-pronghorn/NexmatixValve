package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.pronghorn.pipe.BlobReader;
import OpenDDS.DCPS.*;
import DDS.*;
import org.omg.CORBA.StringSeqHolder;
import Nexmatix.*;

import java.util.ArrayList;
import java.util.List;

public class DDSBroadcastValve implements PubSubListener {

    private static final int VALVE_PARTICIPANT = 23;
    private static final String VALVE_TOPIC = "Valve";

    public static boolean checkReliable(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-r")) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkWaitForAcks(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-w")) {
                return true;
            }
        }
        return false;
    }

    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        ValveData valveData = (ValveData)blobReader.readObject();

        // TODO add DDS configuration
        // -DCPSBit 0 -DCPSConfigFile rtps_disc.ini -r -w
        List<String> list = new ArrayList<String>();
        list.add("-DCPSBit");
        list.add("0");
        list.add("-DCPSConfigFile");
        list.add("rtps_disc.ini");
        list.add("-r");
        list.add("-w");
        String[] stringArray = list.toArray(new String[0]);

        boolean reliable = checkReliable(stringArray);
        boolean waitForAcks = checkWaitForAcks(stringArray);

        DomainParticipantFactory domainParticipantFactory = TheParticipantFactory.WithArgs(new StringSeqHolder(stringArray));
        if (domainParticipantFactory == null) {
            System.err.println("ERROR: Domain Participant Factory not found");
            return false;
        }

        DomainParticipant domainParticipant = domainParticipantFactory.create_participant(
                VALVE_PARTICIPANT,
                PARTICIPANT_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );

        if (domainParticipant == null) {
            System.err.println("ERROR: Domain Participant creation failed");
            return false;
        }

        ValveDataTypeSupportImpl ValveDataTypeSupport = new ValveDataTypeSupportImpl();

        if (ValveDataTypeSupport.register_type(domainParticipant, "") != RETCODE_OK.value) {
            System.err.println("ERROR: register_type failed");
            return false;
        }

        Topic topic = domainParticipant.create_topic(
                VALVE_TOPIC,
                ValveDataTypeSupport.get_type_name(),
                TOPIC_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );
        if (topic == null) {
            System.err.println("ERROR: Topic creation failed");
            return false;
        }

        Publisher publisher = domainParticipant.create_publisher(
                PUBLISHER_QOS_DEFAULT.get(),
                null,
                DEFAULT_STATUS_MASK.value
        );
        if (publisher == null) {
            System.err.println("ERROR: Publisher creation failed");
            return false;
        }

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

        DataWriterQosHolder dataWriterQosHolder = new DataWriterQosHolder(dataWriterQos);
        publisher.get_default_datawriter_qos(dataWriterQosHolder);
        dataWriterQosHolder.value.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        if (reliable) {
            dataWriterQosHolder.value.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
        }

        DataWriter dataWriter = publisher.create_datawriter(
                topic,
                dataWriterQosHolder.value,
                null,
                DEFAULT_STATUS_MASK.value
        );
        if (dataWriter == null) {
            System.err.println("ERROR: DataWriter creation failed");
            return false;
        }

        System.out.println("Publisher Created DataWriter");

        //Publish only if there is a subscriber

        StatusCondition statuscondition = dataWriter.get_statuscondition();
        statuscondition.set_enabled_statuses(PUBLICATION_MATCHED_STATUS.value);

        WaitSet waitSet = new WaitSet();
        waitSet.attach_condition(statuscondition);

        PublicationMatchedStatusHolder matched = new PublicationMatchedStatusHolder(new PublicationMatchedStatus());
        //Duration_t timeout = new Duration_t(DURATION_INFINITE_SEC.value, DURATION_INFINITE_NSEC.value);

        while (true) {

            final int result = dataWriter.get_publication_matched_status(matched);

            if (result != RETCODE_OK.value) {
                System.err.println("ERROR: get_publication_matched_status()" + "failed.");
                return false;
            }

            if (matched.value.current_count >= 1) {
                System.out.println("Publisher Matched");
                break;
            }

            return false;

//            ConditionSeqHolder conditionSeqHolder = new ConditionSeqHolder(new Condition[]{});
//            if (waitSet.wait(conditionSeqHolder, timeout) != RETCODE_OK.value) {
//                System.err.println("ERROR: wait() failed.");
//                return false;
//            }
        }

        waitSet.detach_condition(statuscondition);

        ValveDataDataWriter valveDataDataWriter = ValveDataDataWriterHelper.narrow(dataWriter);
        int handle  = valveDataDataWriter.register_instance(valveData); // register key field

        int ret = RETCODE_TIMEOUT.value;
        while ((ret = valveDataDataWriter.write(valveData, handle)) == RETCODE_TIMEOUT.value) { }
        if (ret != RETCODE_OK.value) {
            System.err.println("ERROR write() returned " + ret);
        }

        if (waitForAcks) {
            System.out.println("Publisher waiting for acks");

            // Wait for acknowledgements
            Duration_t forever = new Duration_t(DURATION_INFINITE_SEC.value, DURATION_INFINITE_NSEC.value);
            dataWriter.wait_for_acknowledgments(forever);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
        System.out.println("Stop Publisher");

        // Clean up
        domainParticipant.delete_contained_entities();
        domainParticipantFactory.delete_participant(domainParticipant);
        TheServiceParticipant.shutdown();

        System.out.println("Publisher exiting");

        return true;
    }
}

