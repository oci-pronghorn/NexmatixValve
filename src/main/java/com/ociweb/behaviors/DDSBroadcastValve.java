package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.pronghorn.pipe.BlobReader;
import Nexmatix.*;

public class DDSBroadcastValve implements PubSubListener {

    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        ValveData valveData = (ValveData)blobReader.readObject();
        // TODO send out to DDS
        return true;
    }
}

