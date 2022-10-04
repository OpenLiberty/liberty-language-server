package io.openliberty.tools.langserver.diagnostic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

public class ServerEnvDiagnosticTest extends AbstractDiagnosticTest {
    
    @Test
    public void testServerEnv() throws Exception {
        // has invalid, case-sensitive, case-insensitive, and negative port integer values.
        testDiagnostic("server.env", 3);
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        List<Range> expectedDiagnosticRanges = new ArrayList<Range>();
        // Checking invalid value: WLP_LOGGING_CONSOLE_FORMAT=asdf
        expectedDiagnosticRanges.add(createRange(0, 27, 31));
        // Checking invalid case-sensitive property: WLP_LOGGING_CONSOLE_SOURCE=messagE
        expectedDiagnosticRanges.add(createRange(2, 27, 34));
        // Checking invalid port: WLP_DEBUG_ADDRESS=-2
        expectedDiagnosticRanges.add(createRange(3, 18, 20));
        
        for (Diagnostic diag: diags) {
            boolean found = expectedDiagnosticRanges.remove(diag.getRange());
            assertTrue("Found diagnostic which the test did not account for: " + diag, found);
        }
        assertEquals("Did not find all the expected diagnostics. These expected ranges were not found: " + expectedDiagnosticRanges.toString(), 0, expectedDiagnosticRanges.size());
    }
}
