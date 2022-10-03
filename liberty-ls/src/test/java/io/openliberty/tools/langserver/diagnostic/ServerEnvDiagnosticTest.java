package io.openliberty.tools.langserver.diagnostic;

import java.io.FileNotFoundException;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.junit.Test;

public class ServerEnvDiagnosticTest extends AbstractDiagnosticTest {
    
    @Test
    public void testServerEnv() throws Exception {
        // has case-sensitive, case-insensitive, and negative port integer values.
        testDiagnostic("server", 3);
        List<Diagnostic> diags = lastPublishedDiagnostics.getDiagnostics();
        // Checking: WLP_LOGGING_CONSOLE_FORMAT=asdf
        checkRange(diags.get(0).getRange(), 0, 27, 31);
        // Checking case-sensitive property: WLP_LOGGING_CONSOLE_SOURCE=messagE
        checkRange(diags.get(1).getRange(), 2, 27, 34);
        // Checking valid port: WLP_DEBUG_ADDRESS=-2
        checkRange(diags.get(2).getRange(), 3, 18, 20);
    }
    
    private void testDiagnostic(String file, int expectedNumberOfErrors) throws FileNotFoundException {
        super.testDiagnostic(file, ".env", expectedNumberOfErrors);
    }
}
