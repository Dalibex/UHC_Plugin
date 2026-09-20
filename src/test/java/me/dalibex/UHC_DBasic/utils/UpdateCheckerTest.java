package me.dalibex.UHC_DBasic.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {
    @Test void ignoresVersionPrefix() {
        assertEquals(0, UpdateChecker.compareVersions("v1.2.3", "V1.2.3"));
    }

    @Test void ignoresSuffix() {
        assertEquals(0, UpdateChecker.compareVersions("1.5.3-SNAPSHOT", "1.5.3"));
    }

    @Test void comparesDifferentLengthsWithImplicitZeros() {
        assertEquals(0, UpdateChecker.compareVersions("1.2", "1.2.0"));
        assertTrue(UpdateChecker.compareVersions("1.2.1", "1.2") > 0);
    }

    @Test void treatsNullAndBlankAsZero() {
        assertEquals(0, UpdateChecker.compareVersions(null, "0"));
        assertEquals(0, UpdateChecker.compareVersions("", null));
    }

    @Test void toleratesMalformedSeparatorsAndSegments() {
        assertEquals(0, UpdateChecker.compareVersions("v1.foo.2", "1.2"));
        assertTrue(UpdateChecker.compareVersions("release2..4", "2.3") > 0);
    }

    @Test void parsesWhitespaceFormattedReleaseJson() {
        String json = "{\n  \"name\": \"Release\",\n  \"tag_name\" : \"v2.4.1\"\n}";
        assertEquals("v2.4.1", UpdateChecker.parseReleaseTag(json));
    }

    @Test void parsesHighestTagFromFormattedTagsJson() {
        String json = "[\n { \"name\" : \"v1.9.0\" },\n {\n\"name\":\"v2.1.0\"\n},\n { \"name\" : \"v2.0.5\" }\n]";
        assertEquals("v2.1.0", UpdateChecker.parseHighestTag(json));
    }
}
