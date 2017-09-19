package com.ociweb.behaviors;

import com.ociweb.gl.api.PubSubListener;
import com.ociweb.pronghorn.pipe.BlobReader;
import com.ociweb.schema.ValveConfig;

// TODO: rename and use the correct class
public class DDSBroadcastValve implements PubSubListener {

    public boolean message(CharSequence charSequence, BlobReader blobReader) {
        ValveConfig config = (ValveConfig)blobReader.readObject();
        // TODO send out to DDS
        return true;
    }
}

