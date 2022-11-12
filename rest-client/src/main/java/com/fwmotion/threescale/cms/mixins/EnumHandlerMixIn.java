package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.redhat.threescale.rest.cms.model.EnumHandler;

import java.io.IOException;

/**
 * @see EnumHandler
 */
@JsonSerialize(using = EnumHandlerMixIn.EnumHandlerSerializer.class)
public interface EnumHandlerMixIn {

    class EnumHandlerSerializer extends StdSerializer<EnumHandler> {

        public EnumHandlerSerializer() {
            this(EnumHandler.class);
        }

        protected EnumHandlerSerializer(Class<EnumHandler> t) {
            super(t);
        }

        @Override
        public void serialize(EnumHandler value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null
                || value == EnumHandler.UNKNOWN_DEFAULT_OPEN_API) {
                gen.writeNull();
            } else {
                gen.writeString(value.getValue());
            }
        }
    }
}
