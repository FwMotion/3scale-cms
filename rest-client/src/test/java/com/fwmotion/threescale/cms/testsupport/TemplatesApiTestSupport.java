package com.fwmotion.threescale.cms.testsupport;

import com.fwmotion.threescale.cms.matchers.CmsLayoutMatcher;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.Layout;
import com.redhat.threescale.rest.cms.model.Template;
import com.redhat.threescale.rest.cms.model.TemplateList;
import org.hamcrest.Matcher;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

public class TemplatesApiTestSupport {

    public static final Layout MAIN_LAYOUT = new Layout()
        .id(119)
        .systemName("main_layout")
        .contentType("text/html")
        .handler(null)
        .liquidEnabled(true)
        .title("Main layout")
        .createdAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 27, 0, ZoneOffset.UTC))
        .updatedAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 27, 0, ZoneOffset.UTC));
    public static final Matcher<CmsObject> MAIN_LAYOUT_MATCHER = new CmsLayoutMatcher(MAIN_LAYOUT);

    private static final Template TEMPLATE_WITH_DRAFT = new Template()
        .id(MAIN_LAYOUT.getId())
        .systemName(MAIN_LAYOUT.getSystemName())
        .contentType(MAIN_LAYOUT.getContentType())
        .handler(MAIN_LAYOUT.getHandler())
        .liquidEnabled(MAIN_LAYOUT.getLiquidEnabled())
        .title(MAIN_LAYOUT.getTitle())
        .draft("Main layout draft content")
        .published("Main layout published content")
        .createdAt(MAIN_LAYOUT.getCreatedAt())
        .updatedAt(MAIN_LAYOUT.getUpdatedAt());

    private static final Template TEMPLATE_WITHOUT_DRAFT = new Template()
        .id(MAIN_LAYOUT.getId())
        .systemName(MAIN_LAYOUT.getSystemName())
        .contentType(MAIN_LAYOUT.getContentType())
        .handler(MAIN_LAYOUT.getHandler())
        .liquidEnabled(MAIN_LAYOUT.getLiquidEnabled())
        .title(MAIN_LAYOUT.getTitle())
        .draft("")
        .published("Main layout published content")
        .createdAt(MAIN_LAYOUT.getCreatedAt())
        .updatedAt(MAIN_LAYOUT.getUpdatedAt());

    private final TemplatesApi templatesApi;

    public TemplatesApiTestSupport(TemplatesApi templatesApi) {
        this.templatesApi = templatesApi;
    }

    public void givenListTemplatesOnlyMainLayout() throws ApiException {
        given(templatesApi.listTemplates(eq(1), anyInt()))
            .willReturn(new TemplateList()
                .currentPage(1)
                .totalPages(1)
                .totalEntries(1)
                .addLayoutsItem(MAIN_LAYOUT));

        given(templatesApi.listTemplates(eq(2), anyInt()))
            .willReturn(new TemplateList()
                .currentPage(2)
                .totalPages(1)
                .totalEntries(1));
    }

    public void givenGetTemplateWithoutDraft(int expectedId) throws ApiException {
        given(templatesApi.getTemplate(expectedId))
            .willReturn(TEMPLATE_WITHOUT_DRAFT);
    }

    public void givenGetTemplateWithDraft(int expectedId) throws ApiException {
        given(templatesApi.getTemplate(expectedId))
            .willReturn(TEMPLATE_WITH_DRAFT);
    }


    public void thenOnlyListTemplatesCalled() throws ApiException {
        InOrder templatesOrder = inOrder(templatesApi);
        then(templatesApi).should(templatesOrder).listTemplates(eq(1), anyInt());
        then(templatesApi).should(templatesOrder).listTemplates(eq(2), anyInt());
        then(templatesApi).shouldHaveNoMoreInteractions();
    }

}
