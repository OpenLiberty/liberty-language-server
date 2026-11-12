/*******************************************************************************
* Copyright (c) 2026 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import io.openliberty.tools.langserver.lemminx.util.DeploymentDescriptorSchemaUtil;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleMappingConstants;
import io.openliberty.tools.langserver.lemminx.util.ResourceBundleUtil;

/**
 * Offers a starter file (stub) as a quick fix when an IBM deployment descriptor,
 * for example ibm-web-bnd.xml, is empty. A stub is available for a file name when a
 * resource with that name exists under {@link #STUB_RESOURCE_DIR}.
 */
public class LibertyDeploymentDescriptorCodeActionParticipant implements ICodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(LibertyDeploymentDescriptorCodeActionParticipant.class.getName());

    private static final String STUB_RESOURCE_DIR = "/stubs/deployment-descriptors/";

    /**
     * Adds a quick fix that replaces an empty deployment descriptor with its stub. The quick fix is added for
     * the diagnostic of the request when the document is empty and a stub exists for the file name.
     */
    @Override
    public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
        String text = request.getDocument().getText();
        if (text != null && text.isEmpty()) {
            addStubCodeAction(request, codeActions, request.getDiagnostic());
        }
    }

    /**
     * Adds the stub quick fix when the document contains only whitespace, because no diagnostic is reported for it.
     */
    @Override
    public void doCodeActionUnconditional(ICodeActionRequest request, List<CodeAction> codeActions,
            CancelChecker cancelChecker) {
        String text = request.getDocument().getText();
        if (text != null && !text.isEmpty() && text.isBlank()) {
            addStubCodeAction(request, codeActions, null);
        }
    }

    /**
     * Adds a quick fix that replaces the whole document with the stub of the deployment descriptor, when a stub
     * exists for the file name.
     */
    private void addStubCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, Diagnostic diagnostic) {
        DOMDocument document = request.getDocument();
        String text = document.getText();
        if (!DeploymentDescriptorSchemaUtil.isDeploymentDescriptor(document.getDocumentURI())) {
            return;
        }
        String stub = getStub(getFileName(document.getDocumentURI()));
        if (stub == null) {
            return;
        }

        Range wholeDocument;
        try {
            wholeDocument = new Range(document.positionAt(0), document.positionAt(text.length()));
        } catch (BadLocationException e) {
            LOGGER.warning("Could not generate the deployment descriptor stub: " + e);
            return;
        }
        CodeAction codeAction = CodeActionFactory.replace(
                ResourceBundleUtil.getMessage(ResourceBundleMappingConstants.TITLE_GENERATE_DEPLOYMENT_DESCRIPTOR),
                wholeDocument, stub, document.getTextDocument(), diagnostic);
        if (diagnostic == null) {
            codeAction.setDiagnostics(null);
        }
        codeActions.add(codeAction);
    }

    private static String getFileName(String uri) {
        return uri == null ? "" : uri.substring(uri.lastIndexOf('/') + 1);
    }

    /**
     * @return the stub content for the given file name, or null if there is no stub for it
     */
    private static String getStub(String fileName) {
        if (fileName.isEmpty()) {
            return null;
        }
        try (InputStream in = LibertyDeploymentDescriptorCodeActionParticipant.class.getResourceAsStream(STUB_RESOURCE_DIR + fileName)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warning("Could not read the stub for " + fileName + ": " + e);
            return null;
        }
    }
}
