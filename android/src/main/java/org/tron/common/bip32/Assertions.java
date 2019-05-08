package org.tron.common.bip32;

/**
 * Assertion utility functions.
 */
public class Assertions {

    /**
     * Verify that the provided precondition holds true.
     *
     * @param assertionResult assertion value
     * @param errorMessage error message if precondition failure
     */
    public static void verifyPrecondition(boolean assertionResult, String errorMessage) {
        if (!assertionResult) {
            throw new RuntimeException(errorMessage);
        }
    }
}
