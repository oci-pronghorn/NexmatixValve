package com.ociweb.behaviors.simulators;

public interface SerialMessageProducer {
    void wantPressureFault();
    void wantLeakFault();
    void wantCycleFault();
    String next(long aLong, int integer);
}
