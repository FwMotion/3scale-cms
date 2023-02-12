package com.fwmotion.threescale.cms.matchers;

import com.fwmotion.threescale.cms.model.CmsFile;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.model.ModelFile;
import org.hamcrest.Description;

import javax.annotation.Nonnull;

public class CmsFileMatcher extends CmsObjectMatcher {

    private final ModelFile expected;

    public CmsFileMatcher(@Nonnull ModelFile expected) {
        super(
            expected.getId(),
            expected.getCreatedAt(),
            expected.getUpdatedAt()
        );
        this.expected = expected;
    }

    @Override
    public boolean matchesSafely(@Nonnull CmsObject actual) {
        if (!(actual instanceof CmsFile)) {
            return false;
        }

        CmsFile actualFile = (CmsFile) actual;

        return super.matchesSafely(actual)
            && actualMatchesExpected(expected.getSectionId(), actualFile.getSectionId())
            && actualMatchesExpected(expected.getPath(), actualFile.getPath())
            && actualMatchesExpected(expected.getTagList(), String.join(",", actualFile.getTags()));
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("CmsFile from " + expected);
    }

}
