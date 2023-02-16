package com.fwmotion.threescale.cms.matchers;

import com.fwmotion.threescale.cms.model.CmsObject;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;

public abstract class CmsObjectMatcher extends TypeSafeMatcher<CmsObject> {

    private final Long expectedId;
    private final OffsetDateTime expectedCreatedAt;
    private final OffsetDateTime expectedUpdatedAt;

    protected CmsObjectMatcher(@Nullable Long expectedId,
                               @Nullable OffsetDateTime expectedCreatedAt,
                               @Nullable OffsetDateTime expectedUpdatedAt) {
        this.expectedId = expectedId;
        this.expectedCreatedAt = expectedCreatedAt;
        this.expectedUpdatedAt = expectedUpdatedAt;
    }

    @Override
    protected boolean matchesSafely(@Nonnull CmsObject actual) {
        return actualMatchesExpected(expectedId, actual.getId())
            && actualMatchesExpected(expectedCreatedAt, actual.getCreatedAt())
            && actualMatchesExpected(expectedUpdatedAt, actual.getUpdatedAt());
    }

    protected <T> boolean actualMatchesExpected(@Nullable T expected,
                                                @Nullable T actual) {
        if (expected == null) {
            return actual == null;
        } else {
            return expected.equals(actual);
        }
    }
}
