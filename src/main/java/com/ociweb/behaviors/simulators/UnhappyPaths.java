package com.ociweb.behaviors.simulators;

class UnhappyPaths implements SerialMessageProducer {
    private int msgIndex = 0;
    private static final String[] msgs = new String[]{
            "[]", // empty
            "garbage[st", // begin
            "3]garbage", // end
            "[garbage]", // garbage
            "[st10]", // illegal
            "[da5st4]", // out of order
    };

    @Override
    public void wantPressureFault() {
    }

    @Override
    public void wantLeakFault() {
    }

    @Override
    public void wantCycleFault() {
    }

    @Override
    public String next(long aLong, int integer) {
        msgIndex++;
        if (msgIndex == msgs.length) {
            msgIndex = 0;
        }
        return msgs[msgIndex];
    }
}
