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
*     IBM Corporation
*******************************************************************************/
package io.openliberty.tools.langserver.diagnostic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.utils.ParserFileHelperUtil;
import io.openliberty.tools.langserver.utils.PropertiesValidationResult;
import io.openliberty.tools.langserver.utils.ServerPropertyValues;

public class LibertyPropertiesDiagnosticService  {

    private static final Logger LOGGER = Logger.getLogger(LibertyPropertiesDiagnosticService.class.getName());
    private static final ResourceBundle DiagnosticMessages = ResourceBundle.getBundle("DiagnosticMessages", Locale.getDefault());


    public Map<String, PropertiesValidationResult> compute(String text, LibertyTextDocument openedDocument) {
        Map<String, PropertiesValidationResult> errors = new HashMap<>();
        if (ParserFileHelperUtil.isBootstrapPropertiesFile(openedDocument) || ParserFileHelperUtil.isServerEnvFile(openedDocument)) {
            BufferedReader br = new BufferedReader(new StringReader(text));
            String line = null;
            int lineNumber = 0;
            try {
                while ((line=br.readLine()) != null) {
                    PropertiesValidationResult validationResult = PropertiesValidationResult.validateServerProperty(line, openedDocument, lineNumber);
                    if (validationResult.hasErrors()) {
                        errors.put(line, validationResult);
                    }
                    lineNumber++;
                }
            } catch (IOException e) {
                LOGGER.warning("Exception while validating the document " + openedDocument.getUri() + "\n" + e);
            }
        }
        return errors;
    }

    public Collection<Diagnostic> convertToLSPDiagnostics(Map<String, PropertiesValidationResult> propertiesErrors) {
        List<Diagnostic> lspDiagnostics = new ArrayList<>();
        for (Map.Entry<String, PropertiesValidationResult> errorEntry : propertiesErrors.entrySet()) {
            PropertiesValidationResult validationResult = errorEntry.getValue();
            String lineContentInError = errorEntry.getKey();
            List<Diagnostic> invalidValueDiagnostics = computeInvalidValuesDiagnostic(validationResult, lineContentInError);
            lspDiagnostics.addAll(invalidValueDiagnostics);
        }
        return lspDiagnostics;
    }

    private List<Diagnostic> computeInvalidValuesDiagnostic(PropertiesValidationResult validationResult,
            String lineContentInError) {
        List<Diagnostic> lspDiagnostics = new ArrayList<>();
        if (validationResult.hasErrors()) {
            String property = validationResult.getKey();
            String messageTemplate = DiagnosticMessages.getString(validationResult.getDiagnosticType());
            
            // Currently the last arg (getIntegerRange) is only used for the Integer messages which use {2}. Otherwise null is passed and is ignored by the other messages.
            String message = MessageFormat.format(messageTemplate, validationResult.getValue(), property, ServerPropertyValues.getIntegerRange(property));
            lspDiagnostics.add(new Diagnostic(computeRange(validationResult, lineContentInError), message));
        }
        return lspDiagnostics;
    }

    /**
     * Returns a Range containing position information for the diagnostic
     * @param validationResult
     * @param lineContentInError - Line entry from property file.
     * @return
     */
    private Range computeRange(PropertiesValidationResult validationResult, String lineContentInError) {
        int equalIndex = lineContentInError.indexOf("=");
        int lineNumber = validationResult.getLineNumber();
        Integer startChar = validationResult.getStartChar();
        Integer endChar = validationResult.getEndChar();

        Position start = startChar == null ? new Position(lineNumber, equalIndex +1) : new Position(lineNumber, startChar);
        Position end = endChar == null ? new Position(lineNumber, lineContentInError.length()) : new Position(lineNumber, endChar);
        return new Range(start, end);
    }
    
}
