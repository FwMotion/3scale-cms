package com.fwmotion.threescale.cms.matchers;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HeaderMatcher extends TypeSafeMatcher<Header> {

    private final String headerName;
    private final Matcher<String> valueMatcher;

    public HeaderMatcher(@Nonnull String headerName,
                         @Nullable Matcher<String> valueMatcher) {
        this.headerName = headerName;
        this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(@Nonnull Header item) {
        if (StringUtils.equalsIgnoreCase(headerName, item.getName())) {
            if (valueMatcher == null) {
                return true;
            }
            return valueMatcher.matches(item.getValue());
        }

        return false;
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("header with name ")
            .appendValue(headerName);

        if (valueMatcher != null) {
            description
                .appendText(" with value of ")
                .appendDescriptionOf(valueMatcher);
        }
    }

    public static Matcher<Header> header(@Nonnull String headerName) {
        return new HeaderMatcher(headerName, null);
    }

    public static Matcher<Header> header(@Nonnull String headerName,
                                       @Nonnull Matcher<String> headerValueMatcher) {
        return new HeaderMatcher(headerName, headerValueMatcher);
    }
}
