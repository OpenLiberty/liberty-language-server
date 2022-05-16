package io.openliberty.tools.langserver.lemminx.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

public class EditAttribute implements ICodeActionParticipant {

    @Override
    public void doCodeAction(Diagnostic diagnostic, Range range, DOMDocument document, List<CodeAction> codeActions,
            SharedSettings sharedSettings, IComponentProvider componentProvider) {
        try {
            String title = "Change the optional attribute from false to true.";
            String replaceText = "optional=\"true\"";
            codeActions.add(CodeActionFactory.replace(title, diagnostic.getRange(), replaceText, document.getTextDocument(), diagnostic));

            // also build option to create file
            new CreateFile().doCodeAction(diagnostic, range, document, codeActions, sharedSettings, componentProvider);
        } catch (Exception e) {
            // do nothing
        }
    }
}
