package com.ociweb.behaviors.simulators;

public interface SerialMessageProducer {
    void resetFaults();
    void wantPressureFault(int stationId, String v);
    void wantLeakFault(int stationId, String v);
    void wantCycleFault(int stationId, int cycleCountLimitIn);
    String next(long aLong, int integer);
}
