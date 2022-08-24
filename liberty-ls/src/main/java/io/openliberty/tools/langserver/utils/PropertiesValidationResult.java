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
package io.openliberty.tools.langserver.utils;

import java.util.List;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;

public class PropertiesValidationResult {
    Integer lineNumber;
    boolean hasErrors;
    PropertiesEntryInstance entry;
    LibertyTextDocument textDocumentItem;
    
    private static final Logger LOGGER = Logger.getLogger(PropertiesValidationResult.class.getName());

    public PropertiesValidationResult(PropertiesEntryInstance entry) {
        this.entry = entry;
        this.textDocumentItem = entry.getTextDocument();
        this.hasErrors = false;
    }

    public void validateServerProperty() {
        if (entry.isComment() || !ServerPropertyValues.canValidateKeyValues(textDocumentItem, entry.getKey())) {
            return;
        }
        String property = entry.getKey();
        List<String> validValues = ServerPropertyValues.getValidValues(textDocumentItem, property);
        if(ServerPropertyValues.isCaseSensitive(property)) {
            // if case-sensitive, check if value is valid
            hasErrors = !validValues.contains(entry.getValue());
        } else {
            // ignoring case, check if value is valid
            hasErrors = !validValues.stream().anyMatch(entry.getValue()::equalsIgnoreCase);
        }
        return;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public String getKey() {
        return entry.getKey();
    }

    public String getValue() {
        return entry.getValue();
    }

    public String getInvalidValueMessageTemplate() {
        if (ParserFileHelperUtil.isBootstrapPropertiesFile(textDocumentItem)) {
            return "INVALID_PROPERTY_VALUE";
        } else {
            return "INVALID_VARIABLE_VALUE";
        }
    }
}
