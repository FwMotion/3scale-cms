package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.redhat.threescale.rest.cms.model.EnumHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

public class EnumHandlerSerializer extends StdSerializer<EnumHandler> {

    public EnumHandlerSerializer() {
        this(EnumHandler.class);
    }

    protected EnumHandlerSerializer(@Nonnull Class<EnumHandler> t) {
        super(t);
    }

    @Override
    public void serialize(@Nullable EnumHandler value,
                          @Nonnull JsonGenerator gen,
                          @Nullable SerializerProvider provider) throws IOException {
        if (value == null
            || value == EnumHandler.UNKNOWN_DEFAULT_OPEN_API) {
            gen.writeNull();
        } else {
            gen.writeString(value.getValue());
        }
    }
}
