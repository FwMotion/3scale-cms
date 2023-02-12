package com.fwmotion.threescale.cms.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class CmsFileMapperUnitTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS, stubOnly = true)
    CmsFileMapper fileMapper;

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
