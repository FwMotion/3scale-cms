package com.fwmotion.threescale.cms.testsupport;

import com.fwmotion.threescale.cms.matchers.CmsBuiltinSectionMatcher;
import com.fwmotion.threescale.cms.matchers.CmsSectionMatcher;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.model.BuiltinSection;
import com.redhat.threescale.rest.cms.model.Section;
import com.redhat.threescale.rest.cms.model.SectionList;
import org.hamcrest.Matcher;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

public class SectionsApiTestSupport {

    public static final BuiltinSection ROOT_BUILTIN_SECTION = new BuiltinSection()
        .id(30L)
        .parentId(null)
        .systemName("root")
        .title("Root")
        .partialPath("/")
        ._public(true)
        .createdAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 27, 0, ZoneOffset.UTC))
        .updatedAt(OffsetDateTime.of(2022, 3, 18, 6, 31, 57, 0, ZoneOffset.UTC));

    public static final Section CSS_SECTION = new Section()
        .id(32L)
        .parentId(30L)
        .systemName("css")
        .title("css")
        .partialPath("/css")
        ._public(true)
        .createdAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 27, 0, ZoneOffset.UTC))
        .updatedAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 27, 0, ZoneOffset.UTC));

    public static final Matcher<CmsObject> ROOT_BUILTIN_SECTION_MATCHER = new CmsBuiltinSectionMatcher(ROOT_BUILTIN_SECTION);
    public static final Matcher<CmsObject> CSS_SECTION_MATCHER = new CmsSectionMatcher(CSS_SECTION);

    private final SectionsApi sectionsApi;

    public SectionsApiTestSupport(SectionsApi sectionsApi) {
        this.sectionsApi = sectionsApi;
    }

    public void givenListSectionOnlyRoot() throws ApiException {
        given(sectionsApi.listSections(eq(1), anyInt()))
            .willReturn(new SectionList()
                .currentPage(1)
                .totalPages(1)
                .totalEntries(1)
                .addBuiltinSectionsItem(ROOT_BUILTIN_SECTION));

        given(sectionsApi.listSections(eq(2), anyInt()))
            .willReturn(new SectionList()
                .currentPage(2)
                .totalPages(1)
                .totalEntries(1));
    }

    public void givenListSectionRootAndCss() throws ApiException {
        given(sectionsApi.listSections(eq(1), anyInt()))
            .willReturn(new SectionList()
                .currentPage(1)
                .totalPages(1)
                .totalEntries(2)
                .addBuiltinSectionsItem(ROOT_BUILTIN_SECTION)
                .addSectionsItem(CSS_SECTION));

        given(sectionsApi.listSections(eq(2), anyInt()))
            .willReturn(new SectionList()
                .currentPage(2)
                .totalPages(1)
                .totalEntries(1));
    }

    public void thenOnlyListSectionsCalled() throws ApiException {
        InOrder sectionsOrder = inOrder(sectionsApi);
        then(sectionsApi).should(sectionsOrder).listSections(eq(1), anyInt());
        then(sectionsApi).should(sectionsOrder).listSections(eq(2), anyInt());
        then(sectionsApi).shouldHaveNoMoreInteractions();
    }

}
