package com.ociweb.behaviors.simulators;

public interface SerialMessageProducer {
    void wantPressureFault(int stationId, char v);
    void wantLeakFault(int stationId, char v);
    void wantCycleFault(int stationId, int cycleCountLimitIn);
    String next(long aLong, int integer);
}
