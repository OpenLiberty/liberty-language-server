/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.langserver.codeactions;

import com.google.gson.JsonPrimitive;
import io.openliberty.tools.langserver.LibertyLanguageServer;
import io.openliberty.tools.langserver.LibertyTextDocumentService;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.openliberty.tools.langserver.utils.LanguageServerConstants.SYMBOL_EQUALS;
import static io.openliberty.tools.langserver.utils.LanguageServerConstants.INSERT_VALUE;
import static io.openliberty.tools.langserver.utils.LanguageServerConstants.REPLACE_VALUE;

public abstract class CodeActionQuickfixFactory {

    protected LibertyTextDocumentService libertyTextDocumentService;
    protected LibertyLanguageServer libertyLanguageServer;
    private static final ResourceBundle codeActionMessages = ResourceBundle.getBundle("CodeActionMessages", Locale.US);

    protected CodeActionQuickfixFactory(LibertyTextDocumentService libertyTextDocumentService, LibertyLanguageServer libertyLanguageServer) {
        this.libertyTextDocumentService = libertyTextDocumentService;
        this.libertyLanguageServer = libertyLanguageServer;
    }

    /**
     * returns list of code actions or commands.
     * called from CodeActionParticipant
     *      1. In case of quick fix for single value property
     *          a. show all allowed values in code action
     *      2. Multi value property,
     *          a. If field has multiple values specified
     *          b. If any of the value is valid, show code action
     *              "Replace with {validValues}"
     *              "Replace with {validValues},${nextAllowedValue1}"
     *              ...
     *              "Replace with {validValues},${nextAllowedValueN}"
     *       example, user entered,WLP_LOGGING_CONSOLE_SOURCE=abc,audit,message,kyc
     *          quickfix should contain something like
     *              Replace with "audit,message
     *              Replace with "audit,message,trace"
     *              Replace with "audit,message,ffdc"
     *              Replace with "audit,message,auditLog"
     *
     * @param params code action params
     * @return codeaction
     */
    public List<Either<Command, CodeAction>> apply(CodeActionParams params) {
        TextDocumentItem openedDocument = libertyTextDocumentService.getOpenedDocument(params.getTextDocument().getUri());
        List<Diagnostic> diagnostics = params.getContext().getDiagnostics();
        List<Either<Command, CodeAction>> res = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getCode() != null && getErrorCode().equals(diagnostic.getCode().getLeft())) {
                String line = new ParserFileHelperUtil().getLine(new LibertyTextDocument(openedDocument), diagnostic.getRange().getStart().getLine());
                if (line != null) {
                    String prefix = getUserEnteredValidValues(diagnostic);
                    if (!Objects.equals(prefix, "")) {
                        // add a code action to just replace with valid values
                        // present in current string
                        res.add(Either.forRight(createCodeAction(params, diagnostic, prefix, line)));
                        // append a comma so that completion will show all values
                        // for multi value
                        prefix += ",";
                    }
                    // fetch all completion values and shows them as quick fix
                    // prefix will contain all valid values in current entered string, or else ""
                    // completion returns empty list if no completion item is present
                    List<String> possibleProperties = retrieveCompletionValues(openedDocument, diagnostic.getRange().getStart(), prefix);
                    for (String mostProbableProperty : possibleProperties) {
                        // expected format for a code action is <Command,CodeAction>
                        res.add(Either.forRight(createCodeAction(params, diagnostic, mostProbableProperty, line)));
                    }
                }
            }
        }
        return res;
    }

    /**
     * get valid values entered by user in the current line
     * in case of multi value property, user may have entered some valid and some invalid values
     * @param diagnostic
     * @return
     */
    private static String getUserEnteredValidValues(Diagnostic diagnostic) {
        String prefix = "";
        // user entered valid values are passed in diagnostic.setData()
        if (diagnostic.getData() != null) {
            if (diagnostic.getData() instanceof JsonPrimitive) {
                prefix = ((JsonPrimitive) diagnostic.getData()).getAsString();
            }
            if (diagnostic.getData() instanceof String) {
                prefix = (String) diagnostic.getData();
            }
        }
        return prefix;
    }

    /**
     * used to create code action object for quickfix
     *
     * @param params           codeaction params
     * @param diagnostic       diagnostic for which code action to be shown
     * @param possibleProperty completion value
     * @param currentLine      current diagnostic line
     * @return codeaction
     */
    protected CodeAction createCodeAction(CodeActionParams params, Diagnostic diagnostic, String possibleProperty, String currentLine) {
        CodeAction codeAction;
        if (!currentLine.contains(SYMBOL_EQUALS)) {
            // append equals symbol before inserting property
            codeAction = new CodeAction(MessageFormat.format(codeActionMessages.getString(INSERT_VALUE), possibleProperty));
            possibleProperty = SYMBOL_EQUALS + possibleProperty;
        } else {
            codeAction = new CodeAction(MessageFormat.format(codeActionMessages.getString(REPLACE_VALUE), possibleProperty));
        }
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setKind(CodeActionKind.QuickFix);
        Map<String, List<TextEdit>> changes = new HashMap<>();
        TextEdit textEdit = new TextEdit(diagnostic.getRange(), possibleProperty);
        changes.put(params.getTextDocument().getUri(), List.of(textEdit));
        codeAction.setEdit(new WorkspaceEdit(changes));
        return codeAction;
    }

    protected abstract List<String> retrieveCompletionValues(TextDocumentItem textDocumentItem, Position position, String prefix);

    protected abstract String getErrorCode();
}
