package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.model.*;
import com.fwmotion.threescale.cms.testsupport.FilesApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.SectionsApiTestSupport;
import com.fwmotion.threescale.cms.testsupport.TemplatesApiTestSupport;
import com.redhat.threescale.rest.cms.ApiClient;
import com.redhat.threescale.rest.cms.XmlEnabledApiClient;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.api.SectionsApi;
import com.redhat.threescale.rest.cms.api.TemplatesApi;
import com.redhat.threescale.rest.cms.model.ModelFile;
import com.redhat.threescale.rest.cms.model.ProviderAccount;
import com.redhat.threescale.rest.cms.model.Section;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fwmotion.threescale.cms.matchers.HeaderMatcher.header;
import static com.fwmotion.threescale.cms.matchers.InputStreamContentsMatcher.inputStreamContents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
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

    ApiClient apiClient;

    @Mock
    CloseableHttpClient httpClientMock;

    @Mock
    CloseableHttpResponse httpResponseMock;

    @Captor
    ArgumentCaptor<HttpClientResponseHandler<?>> responseHandlerMatcher;

    @BeforeEach
    void setUp() {
        sectionsApiTestSupport = new SectionsApiTestSupport(sectionsApi);
        filesApiTestSupport = new FilesApiTestSupport(filesApi);
        templatesApiTestSupport = new TemplatesApiTestSupport(templatesApi);

        apiClient = new XmlEnabledApiClient(httpClientMock);
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
    void getFileContent_ByIdNoAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new ProviderAccount()
                .baseUrl("https://3scale.example.com")
                .siteAccessCode(""));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>)invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(16);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And no access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, is("*/*")))
            ).and(
                not(hasItemInArray(header("Cookie", Matchers.startsWith("access_code="))))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByIdWithAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new ProviderAccount()
                .baseUrl("https://3scale.example.com")
                .siteAccessCode("this is my access code"));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>)invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(16);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And the correct access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, equalTo("*/*")))
            ).and(
                hasItemInArray(header("Cookie", equalTo("access_code=this is my access code")))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByCmsFileNoAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new ProviderAccount()
                .baseUrl("https://3scale.example.com")
                .siteAccessCode(""));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>)invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        CmsFile cmsFile = new CmsFile();
        cmsFile.setId(16L);

        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(cmsFile);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And no access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, is("*/*")))
            ).and(
                not(hasItemInArray(header("Cookie", Matchers.startsWith("access_code="))))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
    }

    @Test
    void getFileContent_ByCmsFileWithAccessCode() throws Exception {
        // Given the HTTP client is accessible
        given(filesApi.getApiClient()).willReturn(apiClient);

        // And the Files API will return information about the file
        given(filesApi.getFile(eq(16L)))
            .willReturn(FilesApiTestSupport.FAVICON_FILE);

        // And the Files API will return information about the tenant account
        // (including no access code)
        given(filesApi.readProviderSettings())
            .willReturn(new ProviderAccount()
                .baseUrl("https://3scale.example.com")
                .siteAccessCode("this is my access code"));

        // And any direct HTTP request will return a result
        given(httpClientMock.execute(ArgumentMatchers.any(HttpUriRequest.class), ArgumentMatchers.<HttpClientResponseHandler<?>>any()))
            .willAnswer(invocation -> ((HttpClientResponseHandler<?>)invocation.getArgument(1))
                .handleResponse(httpResponseMock));

        BasicHttpEntity responseEntity = new BasicHttpEntity(
            IOUtils.toInputStream("response data", Charset.defaultCharset()),
            ContentType.APPLICATION_JSON);
        given(httpResponseMock.getEntity()).willReturn(responseEntity);

        // When file content is requested
        CmsFile cmsFile = new CmsFile();
        cmsFile.setId(16L);

        Optional<InputStream> resultOptional = threescaleCmsClient.getFileContent(cmsFile);

        // Then only the Files API should have interactions
        then(filesApi).should().getFile(eq(16L));
        then(filesApi).should().readProviderSettings();
        //noinspection ResultOfMethodCallIgnored
        then(filesApi).should(atLeastOnce()).getApiClient();
        then(filesApi).shouldHaveNoMoreInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the HTTP client should have a valid request to pull file content
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        //noinspection resource
        then(httpClientMock).should(only()).execute(requestCaptor.capture(), responseHandlerMatcher.capture());
        HttpUriRequest actualRequest = requestCaptor.getValue();

        // And the request should have been GET
        assertThat(actualRequest.getMethod(), is("GET"));

        // And the URI should match expected result
        assertThat(actualRequest.getUri(), is(URI.create("https://3scale.example.com" + FilesApiTestSupport.FAVICON_FILE.getPath())));

        // And the correct access code header should have been included in the request
        assertThat(actualRequest.getHeaders(),
            both(
                hasItemInArray(header(HttpHeaders.ACCEPT, equalTo("*/*")))
            ).and(
                hasItemInArray(header("Cookie", equalTo("access_code=this is my access code")))));

        // And the actual response data should match what was returned by the stubbed http request
        assertTrue(resultOptional.isPresent());
        assertThat(resultOptional.get(), inputStreamContents(equalTo("response data")));
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
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByIdWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplateDraft_ByObjectNoDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithoutDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119L);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplateDraft_ByObjectWithDraftContent() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119L);

        Optional<InputStream> result = threescaleCmsClient.getTemplateDraft(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout draft content")));
    }

    @Test
    void getTemplatePublished_ById() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(119);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void getTemplatePublished_ByObject() throws Exception {
        templatesApiTestSupport.givenGetTemplateWithDraft(119);

        CmsLayout layout = new CmsLayout();
        layout.setId(119L);

        Optional<InputStream> result = threescaleCmsClient.getTemplatePublished(layout);

        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).getTemplate(119L);

        assertTrue(result.isPresent());
        assertThat(result.get(), inputStreamContents(is("Main layout published content")));
    }

    @Test
    void save_NewSectionNoTitle() throws Exception {
        // Given a new CmsSection object to create with no title set
        CmsSection newSection = new CmsSection();
        newSection.setParentId(30L);
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
                .id(31L)
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
        assertThat(newSection.getId(), is(31L));
    }

    @Test
    void save_NewSectionWithTitle() throws Exception {
        // Given a new CmsSection object to create with a title
        CmsSection newSection = new CmsSection();
        newSection.setParentId(30L);
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
                .id(32L)
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
        assertThat(newSection.getId(), is(32L));
    }

    @Test
    void save_UpdatedSection() throws Exception {
        // Given a CmsSection object with an ID already
        CmsSection updatedSection = new CmsSection();
        updatedSection.setParentId(30L);
        updatedSection.setId(31L);
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
        newFile.setSectionId(30L);
        newFile.setTags(Set.of("a", "b", "c"));
        newFile.setDownloadable(true);

        String expectedTagString = newFile.getTags()
            .stream()
            .sorted()
            .collect(Collectors.joining(","));

        // And a File
        File newFileContent = new File("/tmp/file.jpg");

        // And the generated API will respond with an object with an ID
        given(filesApi.createFile(
            eq(newFile.getSectionId()),
            eq(newFile.getPath()),
            same(newFileContent),
            eq(expectedTagString),
            eq(newFile.getDownloadable())))
            .willReturn(new ModelFile()
                // TODO
                .id(17L));

        // When the interface code is called
        threescaleCmsClient.save(newFile, newFileContent);

        // Then only the file content should have been saved
        then(filesApi).should(only()).createFile(
            eq(newFile.getSectionId()),
            eq(newFile.getPath()),
            same(newFileContent),
            eq(expectedTagString),
            eq(newFile.getDownloadable()));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();

        // And the file should have had its ID updated
        assertThat(newFile.getId(), is(17L));
    }

    @Test
    void save_UpdatedFile() throws Exception {
        // Given a CmsFile object with an ID already
        CmsFile updateFile = new CmsFile();
        updateFile.setId(16L);
        updateFile.setPath("/file.jpg");
        updateFile.setSectionId(30L);
        updateFile.setTags(Set.of("a", "b", "c"));
        updateFile.setDownloadable(true);

        String expectedTagString = updateFile.getTags()
            .stream()
            .sorted()
            .collect(Collectors.joining(","));


        // And a File
        File newFileContent = new File("/tmp/file.jpg");

        // And the generated API will respond with an object with an ID
        given(filesApi.updateFile(
            eq(updateFile.getId()),
            eq(updateFile.getSectionId()),
            eq(updateFile.getPath()),
            eq(expectedTagString),
            eq(updateFile.getDownloadable()),
            same(newFileContent)))
            .willReturn(new ModelFile()
                // TODO
                .id(17L));

        // When the interface code is called
        threescaleCmsClient.save(updateFile, newFileContent);

        // Then only the file content should have been saved
        then(filesApi).should(only()).updateFile(
            eq(updateFile.getId()),
            eq(updateFile.getSectionId()),
            eq(updateFile.getPath()),
            eq(expectedTagString),
            eq(updateFile.getDownloadable()),
            same(newFileContent));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
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
    void delete_File() throws Exception {
        // Given a CmsFile object with an ID already
        CmsFile newFile = new CmsFile();
        newFile.setId(16L);
        newFile.setPath("/file.jpg");
        newFile.setSectionId(30L);
        newFile.setTags(Set.of("a", "b", "c"));
        newFile.setDownloadable(true);

        // When the interface code is called
        threescaleCmsClient.delete(newFile);

        // Then only the file should have been deleted
        then(filesApi).should(only()).deleteFile(
            eq(newFile.getId()));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_FileById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.FILE, 16);

        // Then only the file should have been deleted
        then(filesApi).should(only()).deleteFile(
            eq(16L));
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_Section() throws Exception {
        // Given a CmsSection object with an ID already
        CmsSection cmsSection = new CmsSection();
        cmsSection.setParentId(30L);
        cmsSection.setId(31L);
        cmsSection.setSystemName("new");
        cmsSection.setPath("/new");
        cmsSection.setTitle("new_section");
        cmsSection.setPublic(true);

        // When the interface code is called
        threescaleCmsClient.delete(cmsSection);

        // Then only the section should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).should(only()).deleteSection(
            eq(cmsSection.getId()));
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_SectionById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.SECTION, 31);

        // Then only the section should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).should(only()).deleteSection(
            eq(31L));
        then(templatesApi).shouldHaveNoInteractions();
    }

    @Test
    void delete_Template() throws Exception {
        // Given a CmsLayout (Template) object with an ID already
        CmsLayout layout = new CmsLayout();
        layout.setId(119L);

        // When the interface code is called
        threescaleCmsClient.delete(layout);

        // Then only the template should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).deleteTemplate(
            eq(layout.getId()));
    }

    @Test
    void delete_TemplateById() throws Exception {
        // When the interface code is called
        threescaleCmsClient.delete(ThreescaleObjectType.TEMPLATE, 119);

        // Then only the template should have been deleted
        then(filesApi).shouldHaveNoInteractions();
        then(sectionsApi).shouldHaveNoInteractions();
        then(templatesApi).should(only()).deleteTemplate(
            eq(119L));
    }
}
