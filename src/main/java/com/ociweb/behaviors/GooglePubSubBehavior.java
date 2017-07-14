package com.ociweb.behaviors;

import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;
import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;

public class GooglePubSubBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final FieldType fieldType;
    public final String publishTopic;

    public GooglePubSubBehavior(FogRuntime runtime, String publishTopic, int parseId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.fieldType = MessageScheme.types[parseId];
        this.publishTopic = publishTopic;
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        return true;
    }
}
