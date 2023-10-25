package com.fwmotion.threescale.cms.matchers;

import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.CmsSection;
import com.redhat.threescale.rest.cms.model.BuiltinSection;
import jakarta.annotation.Nonnull;
import org.hamcrest.Description;

public class CmsBuiltinSectionMatcher extends CmsObjectMatcher {

    private final BuiltinSection expected;

    public CmsBuiltinSectionMatcher(@Nonnull BuiltinSection expected) {
        super(
            expected.getId(),
            expected.getCreatedAt(),
            expected.getUpdatedAt()
        );
        this.expected = expected;
    }

    @Override
    public boolean matchesSafely(@Nonnull CmsObject actual) {
        if (!(actual instanceof CmsSection actualSection)) {
            return false;
        }

        return super.matchesSafely(actual)
            && actualMatchesExpected(expected.getParentId(), actualSection.getParentId())
            && actualMatchesExpected(expected.getSystemName(), actualSection.getSystemName())
            && actualMatchesExpected(expected.getPartialPath(), actualSection.getPath())
            && actualMatchesExpected(expected.getPublic(), actualSection.getPublic());
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("CmsBuiltinSection from " + expected);
    }
}
