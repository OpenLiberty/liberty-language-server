/*******************************************************************************
* Copyright (c) 2022, 2024 IBM Corporation and others.
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

package io.openliberty.tools.langserver.lemminx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openliberty.tools.langserver.lemminx.codeactions.ReplacePlatform;
import io.openliberty.tools.langserver.lemminx.codeactions.ReplaceVariable;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.codeactions.AddAttribute;
import io.openliberty.tools.langserver.lemminx.codeactions.AddFeature;
import io.openliberty.tools.langserver.lemminx.codeactions.AddTrailingSlash;
import io.openliberty.tools.langserver.lemminx.codeactions.CreateFile;
import io.openliberty.tools.langserver.lemminx.codeactions.EditAttribute;
import io.openliberty.tools.langserver.lemminx.codeactions.RemoveTrailingSlash;
import io.openliberty.tools.langserver.lemminx.codeactions.ReplaceFeature;

public class LibertyCodeActionParticipant implements ICodeActionParticipant {
    
    private final Map<String, ICodeActionParticipant> codeActionParticipants;

    public LibertyCodeActionParticipant() {
        codeActionParticipants = new HashMap<>();
    }

    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        Diagnostic diagnostic = request.getDiagnostic();
        if (diagnostic == null || diagnostic.getCode() == null || !diagnostic.getCode().isLeft()) {
            return;
        }
        registerCodeActions();
        ICodeActionParticipant participant = codeActionParticipants.get(diagnostic.getCode().getLeft());
        if (participant != null) {
            participant.doCodeAction(request, codeActions, cancelChecker);
        }
    }

    private void registerCodeActions() {
        if (codeActionParticipants.isEmpty()) {
            codeActionParticipants.put(LibertyDiagnosticParticipant.MISSING_FILE_CODE, new CreateFile());
            codeActionParticipants.put(LibertyDiagnosticParticipant.NOT_OPTIONAL_CODE, new EditAttribute());
            codeActionParticipants.put(LibertyDiagnosticParticipant.IMPLICIT_NOT_OPTIONAL_CODE, new AddAttribute());
            codeActionParticipants.put(LibertyDiagnosticParticipant.INCORRECT_FEATURE_CODE, new ReplaceFeature());
            codeActionParticipants.put(LibertyDiagnosticParticipant.INCORRECT_PLATFORM_CODE, new ReplacePlatform());
            codeActionParticipants.put(LibertyDiagnosticParticipant.INCORRECT_VARIABLE_CODE, new ReplaceVariable());
            codeActionParticipants.put(LibertyDiagnosticParticipant.MISSING_CONFIGURED_FEATURE_CODE, new AddFeature());
            codeActionParticipants.put(LibertyDiagnosticParticipant.IS_FILE_NOT_DIR_CODE, new RemoveTrailingSlash());
            codeActionParticipants.put(LibertyDiagnosticParticipant.Is_DIR_NOT_FILE_CODE, new AddTrailingSlash());
        }
    }
}
