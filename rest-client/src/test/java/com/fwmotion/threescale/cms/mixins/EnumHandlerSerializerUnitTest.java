package com.fwmotion.threescale.cms.mixins;

import com.fasterxml.jackson.core.JsonGenerator;
import com.redhat.threescale.rest.cms.model.EnumHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;

public class EnumHandlerSerializerUnitTest {

    EnumHandlerSerializer serializer;
    JsonGenerator gen;

    @BeforeEach
    void setUp() {
        serializer = new EnumHandlerSerializer();
        gen = mock(JsonGenerator.class);
    }

    @Test
    void serialize_Null() throws Exception {
        serializer.serialize(null, gen, null);

        then(gen).should(only()).writeNull();
    }

    @Test
    void serialize_Unknown() throws Exception {
        serializer.serialize(EnumHandler.UNKNOWN_DEFAULT_OPEN_API, gen, null);

        then(gen).should(only()).writeNull();
    }

    @Test
    void serialize_Value() throws Exception {
        serializer.serialize(EnumHandler.TEXTILE, gen, null);

        then(gen).should(only()).writeString("textile");
    }
}
