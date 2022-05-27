/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.lemminx.codeactions.AddAttribute;
import io.openliberty.tools.langserver.lemminx.codeactions.CreateFile;
import io.openliberty.tools.langserver.lemminx.codeactions.EditAttribute;

public class LibertyCodeActionParticipant implements ICodeActionParticipant {
    
    private final Map<String, ICodeActionParticipant> codeActionParticipants;

    public LibertyCodeActionParticipant() {
        codeActionParticipants = new HashMap<>();
    }

    @Override
    public void doCodeAction(Diagnostic diagnostic, Range range, DOMDocument document, List<CodeAction> codeActions,
            SharedSettings sharedSettings, IComponentProvider componentProvider) {
        if (diagnostic == null || diagnostic.getCode() == null || !diagnostic.getCode().isLeft()) {
            return;
        }
        registerCodeActions(sharedSettings);
        ICodeActionParticipant participant = codeActionParticipants.get(diagnostic.getCode().getLeft());
        if (participant != null) {
            participant.doCodeAction(diagnostic, range, document, codeActions, sharedSettings, componentProvider);
        }
    }

    private void registerCodeActions(SharedSettings sharedSettings) {
        if (codeActionParticipants.isEmpty()) {
            codeActionParticipants.put(LibertyDiagnosticParticipant.MISSING_FILE_CODE, new CreateFile());
            codeActionParticipants.put(LibertyDiagnosticParticipant.NOT_OPTIONAL_CODE, new EditAttribute());
            codeActionParticipants.put(LibertyDiagnosticParticipant.IMPLICIT_NOT_OPTIONAL_CODE, new AddAttribute());
        }
    }
}
