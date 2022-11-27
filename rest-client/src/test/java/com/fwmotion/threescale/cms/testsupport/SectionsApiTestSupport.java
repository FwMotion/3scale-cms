package com.fwmotion.threescale.cms.testsupport;

import com.fwmotion.threescale.cms.matchers.CmsSectionMatcher;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.model.Section;
import com.redhat.threescale.rest.cms.model.SectionList;
import org.hamcrest.Matcher;
import org.mockito.InOrder;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

public class SectionsApiTestSupport {

    public static final Section ROOT_SECTION = new Section()
        .id(30)
        .parentId(null)
        .systemName("root")
        .partialPath("/")
        ._public(true)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now());

    public static final Matcher<CmsObject> ROOT_SECTION_MATCHER = new CmsSectionMatcher(ROOT_SECTION);

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
                .addSectionsItem(ROOT_SECTION));

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
