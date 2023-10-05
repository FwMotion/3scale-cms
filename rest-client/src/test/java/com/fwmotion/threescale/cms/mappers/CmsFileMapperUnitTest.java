package com.fwmotion.threescale.cms.mappers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CmsFileMapperUnitTest {

    CmsFileMapper fileMapper = Mappers.getMapper(CmsFileMapper.class);

    @Test
    void tagsFromRest_Null() {
        Set<String> result = fileMapper.tagsFromRest(null);

        assertThat(result, hasSize(0));
    }

    @Test
    void tagsFromRest_NotNull() {
        Set<String> result = fileMapper.tagsFromRest("1,2 ,  3,4");

        assertThat(result, contains("1", "2", "3", "4"));
    }

    @Test
    void tagsToRest_Null() {
        String result = fileMapper.tagsToRest(null);

        assertThat(result, is(""));
    }

    @Test
    void tagsToRest_NotNull() {
        String result = fileMapper.tagsToRest(Set.of("1", "2", "a", "b"));

        assertThat(result, is("1,2,a,b"));
    }

}
