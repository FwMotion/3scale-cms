package com.fwmotion.threescale.cms.matchers;

import com.fwmotion.threescale.cms.model.CmsLayout;
import com.fwmotion.threescale.cms.model.CmsObject;
import com.redhat.threescale.rest.cms.model.Layout;
import jakarta.annotation.Nonnull;
import org.hamcrest.Description;

public class CmsLayoutMatcher extends CmsObjectMatcher {

    private final Layout expected;

    public CmsLayoutMatcher(Layout expected) {
        super(
            expected.getId(),
            expected.getCreatedAt(),
            expected.getUpdatedAt()
        );
        this.expected = expected;
    }

    @Override
    public boolean matchesSafely(@Nonnull CmsObject actual) {
        if (!(actual instanceof CmsLayout actualLayout)) {
            return false;
        }

        return super.matchesSafely(actual)
            && actualMatchesExpected(expected.getSystemName(), actualLayout.getSystemName())
            && actualMatchesExpected(expected.getContentType(), actualLayout.getContentType())
            && actualMatchesExpected(expected.getHandler(), actualLayout.getHandler())
            && actualMatchesExpected(expected.getLiquidEnabled(), actualLayout.getLiquidEnabled())
            && actualMatchesExpected(expected.getTitle(), actualLayout.getTitle())
            && actualMatchesExpected(expected.getDraft(), actualLayout.getDraftContent())
            && actualMatchesExpected(expected.getPublished(), actualLayout.getPublishedContent());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("CmsLayout from " + expected);
    }
}
