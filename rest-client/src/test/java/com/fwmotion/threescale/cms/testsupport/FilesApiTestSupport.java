package com.fwmotion.threescale.cms.testsupport;

import com.fwmotion.threescale.cms.matchers.CmsFileMatcher;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.ApiException;
import com.redhat.threescale.rest.cms.api.FilesApi;
import com.redhat.threescale.rest.cms.model.FileList;
import com.redhat.threescale.rest.cms.model.ListPaginationMetadata;
import com.redhat.threescale.rest.cms.model.ModelFile;
import org.hamcrest.Matcher;
import org.mockito.InOrder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

public class FilesApiTestSupport {

    public static final ModelFile FAVICON_FILE = new ModelFile()
        .id(575437L)
        .sectionId(2675715L)
        .path("/favicon.ico")
        .url("http://s3.openshift-storage.svc/bucket--99173774-11c4-4a63-9d1a-2df7b6a0b5bd/provider-name/2022/03/05/favicon-e94f1a378c59231c.ico?X-Amz-Algorithm=AWS4-HMAC-SHA256&amp;X-Amz-Credential=VLXdr0R4TkaRNkgubbLx%2F20221005%2Fus-east-1%2Fs3%2Faws4_request&amp;X-Amz-Date=20221005T214121Z&amp;X-Amz-Expires=900&amp;X-Amz-SignedHeaders=host&amp;X-Amz-Signature=deb0c4a3741a76adf739c3865c978aa0a062410685efa0b16e2827c977ea3143")
        .title("favicon.ico")
        .createdAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 29, 0, ZoneOffset.UTC))
        .updatedAt(OffsetDateTime.of(2022, 3, 5, 4, 48, 29, 0, ZoneOffset.UTC));

    public static final Matcher<CmsObject> FAVICON_FILE_MATCHER = new CmsFileMatcher(FAVICON_FILE);

    private final FilesApi filesApi;

    public FilesApiTestSupport(FilesApi filesApi) {
        this.filesApi = filesApi;
    }

    public void givenListFilesOnlyFavicon() throws ApiException {
        given(filesApi.listFiles(eq(1), anyInt(), isNull()))
            .willReturn(new FileList()
                .metadata(new ListPaginationMetadata()
                    .currentPage(1)
                    .totalPages(1)
                    .totalEntries(1))
                .addCollectionItem(FAVICON_FILE));

        given(filesApi.listFiles(eq(2), anyInt(), isNull()))
            .willReturn(new FileList()
                .metadata(new ListPaginationMetadata()
                    .currentPage(2)
                    .totalPages(1)
                    .totalEntries(1)));
    }

    public void thenOnlyListFilesCalled() throws ApiException {
        InOrder filesOrder = inOrder(filesApi);
        then(filesApi).should(filesOrder).listFiles(eq(1), anyInt(), isNull());
        then(filesApi).should(filesOrder).listFiles(eq(2), anyInt(), isNull());
        then(filesApi).shouldHaveNoMoreInteractions();
    }

}
