package com.orthodoxprayers.privateapp.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ExternalLinkPolicyTest {
    @Test
    public void allowsOnlyWellFormedHttpsWithoutEmbeddedCredentials() {
        assertTrue(ExternalLinkPolicy.isAllowedHttps("https://orthodoxjordan.org/prayers"));
        assertTrue(ExternalLinkPolicy.isAllowedHttps("  https://example.org/path?q=1  "));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("http://example.org"));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("intent://example.org/#Intent;end"));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("file:///data/local/tmp/item"));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("https://user:password@example.org/private"));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("https://example.org\nintent://bad"));
        assertFalse(ExternalLinkPolicy.isAllowedHttps("https:///missing-host"));
    }
}
