package com.fwmotion.threescale.cms.mappers;

import com.redhat.threescale.rest.cms.model.EnumHandler;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

class CmsTemplateMapperUnitTest {

    CmsTemplateMapper templateMapper = Mappers.getMapper(CmsTemplateMapper.class);

    @Test
    void mapHandlerFromRest_Null() {
        String result = templateMapper.mapHandlerFromRest(null);

        assertNull(result);
    }

    @Test
    void mapHandlerFromRest_Unknown() {
        String result = templateMapper.mapHandlerFromRest(EnumHandler.UNKNOWN_DEFAULT_OPEN_API);

        assertNull(result);
    }

    @Test
    void mapHandlerFromRest_Value() {
        String result = templateMapper.mapHandlerFromRest(EnumHandler.MARKDOWN);

        assertThat(result, is("markdown"));
    }

    @Test
    void mapHandlerToRest_Null() {
        EnumHandler result = templateMapper.mapHandlerToRest(null);

        assertNull(result);
    }

    @Test
    void mapHandlerToRest_Unknown() {
        EnumHandler result = templateMapper.mapHandlerToRest("something never available as a real handler");

        assertNull(result);
    }

    @Test
    void mapHandlerToRest_Value() {
        EnumHandler result = templateMapper.mapHandlerToRest("textile");

        assertThat(result, is(EnumHandler.TEXTILE));
    }

}
