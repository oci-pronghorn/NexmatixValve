package com.ociweb.behaviors.simulators;

@FunctionalInterface
public interface SerialMessageProducer {
    String next(long aLong, int integer);
}
