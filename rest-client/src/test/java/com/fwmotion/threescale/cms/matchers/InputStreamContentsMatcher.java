package com.fwmotion.threescale.cms.matchers;

import jakarta.annotation.Nonnull;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class InputStreamContentsMatcher extends TypeSafeMatcher<InputStream> {

    private final Matcher<String> contentsMatcher;

    public InputStreamContentsMatcher(@Nonnull Matcher<String> contentsMatcher) {
        this.contentsMatcher = contentsMatcher;
    }

    @Override
    protected boolean matchesSafely(@Nonnull InputStream actual) {
        try {
            actual.reset();
            String contents = IOUtils.toString(actual, Charset.defaultCharset());

            return contentsMatcher.matches(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void describeTo(@Nonnull Description description) {
        description.appendText("input stream's contents ")
                .appendDescriptionOf(contentsMatcher);
    }

    @Override
    protected void describeMismatchSafely(InputStream item, Description mismatchDescription) {
        try {
            item.reset();
            String contents = IOUtils.toString(item, Charset.defaultCharset());

            mismatchDescription.appendText("input stream's contents is \"" + contents + "\"");
        } catch (IOException e) {
            mismatchDescription.appendText("unreadable input stream; " + e.getMessage());
        }
    }

    public static InputStreamContentsMatcher inputStreamContents(Matcher<String> contentsMatcher) {
        return new InputStreamContentsMatcher(contentsMatcher);
    }
}
