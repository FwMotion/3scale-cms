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
        if (!(actual instanceof CmsLayout)) {
            return false;
        }

        CmsLayout actualLayout = (CmsLayout) actual;

        return super.matchesSafely(actual)
            && actualMatchesExpected(expected.getSystemName(), actualLayout.getSystemName())
            && actualMatchesExpected(expected.getContentType(), actualLayout.getContentType())
            && actualMatchesExpected(expected.getHandler(), actualLayout.getHandler())
            && actualMatchesExpected(expected.getLiquidEnabled(), actualLayout.getLiquidEnabled())
            && actualMatchesExpected(expected.getTitle(), actualLayout.getTitle());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("CmsLayout from " + expected);
    }
}
