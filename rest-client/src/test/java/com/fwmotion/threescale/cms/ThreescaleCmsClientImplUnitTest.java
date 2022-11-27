package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.model.*;
import com.fwmotion.threescale.cms.testsupport.FilesApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.SectionsApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.TemplatesApiTestSupport;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.ModelFile;
import com.redhat.threescale.rest.cms.model.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fwmotion.threescale.cms.matchers.InputStreamContentsMatcher.inputStreamContents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.only;

@ExtendWith(MockitoExtension.class)
class ThreescaleCmsClientImplUnitTest {

    @InjectMocks
    ThreescaleCmsClientImpl threescaleCmsClient;

    @Mock
    FilesApi filesApi;

    @Mock
    SectionsApi sectionsApi;

    @Mock
    TemplatesApi templatesApi;

    SectionsApiTestSupport sectionsApiTestSupport;

    FilesApiTestSupport filesApiTestSupport;

    TemplatesApiTestSupport templatesApiTestSupport;

    @BeforeEach
    void setUp() {
        sectionsApiTestSupport = new SectionsApiTestSupport(sectionsApi);
        filesApiTestSupport = new FilesApiTestSupport(filesApi);
        templatesApiTestSupport = new TemplatesApiTestSupport(templatesApi);
    }

    @Test
    void testStreamAllCmsObjects() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();
        filesApiTestSupport.givenListFilesOnlyFavicon();
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsObject> result = threescaleCmsClient.streamAllCmsObjects()
            .collect(Collectors.toList());

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        filesApiTestSupport.thenOnlyListFilesCalled();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(
            SectionsApiTestSupport.ROOT_SECTION_MATCHER,
            FilesApiTestSupport.FAVICON_FILE_MATCHER,
            TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void listAllCmsObjects() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();
        filesApiTestSupport.givenListFilesOnlyFavicon();
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsObject> result = threescaleCmsClient.listAllCmsObjects();

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        filesApiTestSupport.thenOnlyListFilesCalled();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(
            SectionsApiTestSupport.ROOT_SECTION_MATCHER,
            FilesApiTestSupport.FAVICON_FILE_MATCHER,
            TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void streamSections() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();

        List<CmsSection> result = threescaleCmsClient.streamSections()
            .collect(Collectors.toList());

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(SectionsApiTestSupport.ROOT_SECTION_MATCHER));
    }

    @Test
    void listSections() throws Exception {
        sectionsApiTestSupport.givenListSectionOnlyRoot();

        List<CmsSection> result = threescaleCmsClient.listSections();

        sectionsApiTestSupport.thenOnlyListSectionsCalled();
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(SectionsApiTestSupport.ROOT_SECTION_MATCHER));
    }

    @Test
    void streamFiles() throws Exception {
        filesApiTestSupport.givenListFilesOnlyFavicon();

        List<CmsFile> result = threescaleCmsClient.streamFiles()
            .collect(Collectors.toList());

        filesApiTestSupport.thenOnlyListFilesCalled();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(FilesApiTestSupport.FAVICON_FILE_MATCHER));
    }

    @Test
    void listFiles() throws Exception {
        filesApiTestSupport.givenListFilesOnlyFavicon();

        List<CmsFile> result = threescaleCmsClient.listFiles();

        filesApiTestSupport.thenOnlyListFilesCalled();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        assertThat(result, contains(FilesApiTestSupport.FAVICON_FILE_MATCHER));
    }

    @Test
    void streamTemplates() throws Exception {
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsTemplate> result = threescaleCmsClient.streamTemplates()
            .collect(Collectors.toList());

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void listTemplates() throws Exception {
        templatesApiTestSupport.givenListTemplatesOnlyMainLayout();

        List<CmsTemplate> result = threescaleCmsClient.listTemplates();

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        templatesApiTestSupport.thenOnlyListTemplatesCalled();

        assertThat(result, contains(TemplatesApiTestSupport.MAIN_LAYOUT_MATCHER));
    }

    @Test
    void getTemplateDraft_ByIdNoDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithoutDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByIdWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplateDraft_ByObjectNoDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithoutDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByObjectWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplatePublished_ById() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplatePublished_ByObject() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void save_NewSectionNoTitle() throws Exception {
        // Given a new CmsSection object to create with no title set
        CmsSection newSection = new CmsSection();
        newSection.setParentId(30);
        newSection.setId(null);
        newSection.setSystemName("new");
        newSection.setPath("/new");
        newSection.setTitle(null);
        newSection.setPublic(true);

        // And the generated API will respond with an object with an ID
        given(sectionsApi.createSection(
            eq(newSection.getPublic()),
            eq(newSection.getSystemName()),
            eq(newSection.getParentId()),
            eq(newSection.getPath()),
            eq(newSection.getSystemName())))
            .willReturn(new Section()
                .parentId(newSection.getParentId())
                .id(31)
                .systemName(newSection.getSystemName())
                .partialPath(newSection.getPath())
                .title(newSection.getSystemName())
                ._public(newSection.getPublic())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        threescaleCmsClient.save(newSection);

        // Then only the Sections API should have been called to create a section
        then(sectionsApi).should(only()).createSection(
            eq(newSection.getPublic()),
            eq(newSection.getSystemName()),
            eq(newSection.getParentId()),
            eq(newSection.getPath()),
            eq(newSection.getSystemName()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the new section object should have an ID
        assertThat(newSection.getId(), is(31));
    }

    @Test
    void save_NewSectionWithTitle() throws Exception {
        // Given a new CmsSection object to create with a title
        CmsSection newSection = new CmsSection();
        newSection.setParentId(30);
        newSection.setId(null);
        newSection.setSystemName("new");
        newSection.setPath("/new");
        newSection.setTitle("new_section");
        newSection.setPublic(true);

        // And the generated API will respond with an object with an ID
        given(sectionsApi.createSection(
            eq(newSection.getPublic()),
            eq(newSection.getTitle()),
            eq(newSection.getParentId()),
            eq(newSection.getPath()),
            eq(newSection.getSystemName())))
            .willReturn(new Section()
                .parentId(newSection.getParentId())
                .id(32)
                .systemName(newSection.getSystemName())
                .partialPath(newSection.getPath())
                .title(newSection.getTitle())
                ._public(newSection.getPublic())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        threescaleCmsClient.save(newSection);

        // Then only the sections API should have been called to create a section
        then(sectionsApi).should(only()).createSection(
            eq(newSection.getPublic()),
            eq(newSection.getTitle()),
            eq(newSection.getParentId()),
            eq(newSection.getPath()),
            eq(newSection.getSystemName()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the new section should have an ID
        assertThat(newSection.getId(), is(32));
    }

    @Test
    void save_UpdatedSection() throws Exception {
        // Given a CmsSection object with an ID already
        CmsSection updatedSection = new CmsSection();
        updatedSection.setParentId(30);
        updatedSection.setId(31);
        updatedSection.setSystemName("new");
        updatedSection.setPath("/new");
        updatedSection.setTitle("new_section");
        updatedSection.setPublic(true);
        updatedSection.setCreatedAt(OffsetDateTime.now());
        updatedSection.setUpdatedAt(OffsetDateTime.now());

        // And the generated API will respond with an object with an ID
        given(sectionsApi.updateSection(
            eq(updatedSection.getId()),
            eq(updatedSection.getPublic()),
            eq(updatedSection.getTitle()),
            eq(updatedSection.getParentId())))
            .willReturn(new Section()
                .parentId(updatedSection.getParentId())
                .id(updatedSection.getId())
                .systemName(updatedSection.getSystemName())
                .partialPath(updatedSection.getPath())
                .title(updatedSection.getTitle())
                ._public(updatedSection.getPublic())
                .createdAt(updatedSection.getCreatedAt())
                .updatedAt(OffsetDateTime.now()));

        // When the new section is saved
        threescaleCmsClient.save(updatedSection);

        // Then only the sections API should have been called to create a section
        then(sectionsApi).should(only()).updateSection(
            eq(updatedSection.getId()),
            eq(updatedSection.getPublic()),
            eq(updatedSection.getTitle()),
            eq(updatedSection.getParentId()));
        then(filesApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void save_NewFile() throws Exception {
        // Given a CmsFile object with no ID yet
        CmsFile newFile = new CmsFile();
        newFile.setId(null);
        newFile.setPath("/file.jpg");
        newFile.setSectionId(30);
        newFile.setTags(Set.of("a", "b", "c"));
        newFile.setDownloadable(true);

        // And a File
        File newFileContent = new File("/tmp/file.jpg");

        // And the generated API will respond with an object with an ID
        given(filesApi.createFile(
            eq(newFile.getSectionId()),
            eq(newFile.getPath()),
            same(newFileContent),
            eq(String.join(",", newFile.getTags())),
            eq(newFile.getDownloadable())))
            .willReturn(new ModelFile()
                // TODO
                .id(17));

        // When the interface code is called
        threescaleCmsClient.save(newFile, newFileContent);

        // Then only the file content should have been saved
        then(filesApi).should(only()).createFile(
            eq(newFile.getSectionId()),
            eq(newFile.getPath()),
            same(newFileContent),
            eq(String.join(",", newFile.getTags())),
            eq(newFile.getDownloadable()));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the file should have had its ID updated
        assertThat(newFile.getId(), is(17));
    }

    @Test
    void getFileContent() {
    }

    @Test
    void testGetFileContent() {
    }

    @Test
    void testSave() {
    }

    @Test
    void testSave1() {
    }

    @Test
    void publish() {
    }

    @Test
    void delete() {
    }

    @Test
    void testDelete() {
    }
}
