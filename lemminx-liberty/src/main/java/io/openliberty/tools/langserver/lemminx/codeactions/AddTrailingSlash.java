package io.openliberty.tools.langserver.lemminx.codeactions;

import java.io.File;
import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class AddTrailingSlash implements ICodeActionParticipant {
    
    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        DOMDocument document = request.getDocument();
        try {
            String fileSeparator = "/";
            String locationText = document.findNodeAt(document.offsetAt(diagnostic.getRange().getEnd())).getAttribute("location");
             // if Windows and path using \, continue using it
            if (File.separator.equals("\\") && locationText.contains("\\")) {
                fileSeparator = "\\";
            }
            String title = "Add trailing slash to specify directory.";
            String replaceText = "location=\"" + locationText + fileSeparator + "\"";
            codeActions.add(CodeActionFactory.replace(title, diagnostic.getRange(), replaceText, document.getTextDocument(), diagnostic));
        } catch (Exception e) {
            // do nothing
        }
    }
}
