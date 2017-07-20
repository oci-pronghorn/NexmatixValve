package com.ociweb.behaviors.simulators;

@FunctionalInterface
interface SerialMessageProducer {
    String next(long aLong, int integer);
}
