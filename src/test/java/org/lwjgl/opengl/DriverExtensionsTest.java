package org.lwjgl.opengl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Driver extension lookup must degrade to an empty set without a context.
 */
class DriverExtensionsTest {

    @Test
    void aDriverThatWillNotAnswerLeavesEveryFlagFalse() {
        Set<String> extensions = DriverExtensions.of(null);
        assertNotNull(extensions, "The query must never return null as the generated constructor calls contains() on it directly");
        assertTrue(extensions.isEmpty(), "With nothing to ask, no extension may be reported present");
    }

}
