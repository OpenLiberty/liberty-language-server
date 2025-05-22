/*******************************************************************************
* Copyright (c) 2022, 2025 IBM Corporation and others.
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Range;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.model.propertiesfile.PropertiesEntryInstance;

public class PropertiesValidationResult {
    Integer lineNumber, startChar = null, endChar = null;
    boolean hasErrors;
    boolean hasWarnings;
    String diagnosticType;
    PropertiesEntryInstance entry;
    LibertyTextDocument textDocumentItem;
    // being used for passing correct values for quickfix
    // in case the current value has multiple values, and some of them is correct
    String multiValuePrefix;
    // used in case we need to send a custom diagnostic message
    String customValue;
    
    private static final Logger LOGGER = Logger.getLogger(PropertiesValidationResult.class.getName());

    private PropertiesValidationResult(PropertiesEntryInstance entry) {
        this.entry = entry;
        this.textDocumentItem = entry.getTextDocument();
        this.hasErrors = false;
        this.multiValuePrefix = "";
    }

    /**
     * Validates if line has valid value for the provided key. Returns a PropertiesValidationResult object containing validation information.
     * @param line - String of line content
     * @param textDocumentItem
     * @return PropertiesValidationResult with information about any errors
     */
    public static PropertiesValidationResult validateServerProperty(String line, LibertyTextDocument textDocumentItem, Integer lineNumber) {
        PropertiesEntryInstance entry = new PropertiesEntryInstance(line, textDocumentItem);
        PropertiesValidationResult result = new PropertiesValidationResult(entry);
        result.setLineNumber(lineNumber);
        result.validateServerProperty();
        return result;
    }

    /**
     * Validates property, raises hasErrors flag and sets the diagnostic type if error is detected. 
     * Sets the start and/or end character for the error.
     */
    // Note: If setting hasErrors to true, MUST set diagnosticType as well for diagnostic message retrieval.
    public void validateServerProperty() {
        if (entry.isComment()) {
            return;
        }
        String property = entry.getKey();
        String value = entry.getValue();
        
        // check whitespace around equal sign (=)
        if (LibertyConfigFileManager.isServerEnvFile(textDocumentItem)) {
            if ((property != null && property.endsWith(" ")) || (value != null && value.startsWith(" "))) {
                startChar = property.trim().length();
                endChar = entry.getLineContent().length() - value.trim().length();
                hasErrors = true;
                diagnosticType = "SERVER_ENV_INVALID_WHITESPACE";
                return;
            }
        }

        

        if (ServerPropertyValues.usesPredefinedValues(textDocumentItem, property)) {
            if (value == null || value.isEmpty()) {
                // if a value is not specified, issue a warning not an error
                hasWarnings = true;
                diagnosticType = LibertyConfigFileManager.isBootstrapPropertiesFile(textDocumentItem) ? 
                                            "EMPTY_PROPERTY_VALUE" : "EMPTY_VARIABLE_VALUE";
            } else {
                List<String> validValues = ServerPropertyValues.getValidValues(textDocumentItem, property);
                if(ServerPropertyValues.isCaseSensitive(property)) {
                    // currently all comma separated values properties are case-sensitive
                    if (ServerPropertyValues.multipleCommaSeparatedValuesAllowed(getKey()) && value != null) {
                        validateMultiValueProperty(value, validValues);
                    } else {
                        // case-sensitive single value field, check if value is valid
                        hasErrors = !validValues.contains(value);
                    }
                } else {
                    // ignoring case, check if value is valid
                    hasErrors = !validValues.stream().anyMatch(value::equalsIgnoreCase);
                }
                diagnosticType = LibertyConfigFileManager.isBootstrapPropertiesFile(textDocumentItem) ? 
                                                "INVALID_PROPERTY_VALUE" : "INVALID_VARIABLE_VALUE";
            }
        } else if (ServerPropertyValues.usesIntegerRangeValue(property)) {
            Range<Integer> range = ServerPropertyValues.getIntegerRange(property);
            if (ServerPropertyValues.usesTimeUnit(property)) {
                // for purgeMinTime - with or without unit (h or H) is acceptable.
                try { // try parsing Integer if no units specified
                    hasErrors = !range.contains(Integer.parseInt(value));
                } catch (NumberFormatException e) { // contains unit (non-decimal char)
                    // only accept integer followed by h or H
                    Pattern integerAndTime = Pattern.compile("(^[0-9]+)([h|H]?)$");
                    Matcher matcher = integerAndTime.matcher(value);
                    if (matcher.find()) {
                        try {
                            int val = new BigInteger(matcher.group(1)).intValueExact();
                            hasErrors = !range.contains(val);
                        } catch (ArithmeticException ae) { // value too large for Integer
                            hasErrors = true;
                        }
                    } else { // did not contain or start with integer
                        hasErrors = true;
                    }
                }
            } else {
                try {
                    hasErrors = !range.contains(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    hasErrors = true;
                }
            }
            diagnosticType = LibertyConfigFileManager.isBootstrapPropertiesFile(textDocumentItem) ? 
                                            "INVALID_PROPERTY_INTEGER_RANGE" : "INVALID_VARIABLE_INTEGER_RANGE";
        } else if (ServerPropertyValues.usesPackageNames(property)) { // defines packages
            // simple check for comma-delimited list of Java packages
            Pattern packageList = Pattern.compile("^([a-z_]+((\\.[a-z]|\\._[0-9])[a-z0-9_]*)*)(,[a-z_]+((\\.[a-z]|\\._[0-9])[a-z0-9_]*)*)*$");
            Matcher matcher = packageList.matcher(value);
            if (!matcher.find()) {
                hasErrors = true;
            }
            diagnosticType = "INVALID_PACKAGE_LIST_FORMAT";
        }
        endChar = entry.getLineContent().length();
        return;
    }

    /**
     * validate multi value property
     *      1. set hasErrors to true if value starts or ends with comma
     *      2. if multi value has some valid and invalid values
     *          a. send a custom message back to show only invalid values in error message
     *          b. send valid values along with diagnostic data,
     *             so that code action can show option to quickfix using valid values
     * @param value
     * @param validValues
     */
    private void validateMultiValueProperty(String value, List<String> validValues) {
        if (value.startsWith(",") || value.endsWith(",")) {
            hasErrors = true;
        } else {
            List<String> enteredValues = ServerPropertyValues.getCommaSeparatedValues(value);
            hasErrors = !new HashSet<>(validValues).containsAll(enteredValues);
            HashSet<String> invalidValues = new HashSet<>(enteredValues);
            validValues.forEach(invalidValues::remove);
            // setting a custom value for diagnostic message, to show only invalid values in the message
            setCustomValue(String.join(",", invalidValues));
            // getting all valid prefix in case of multiple values
            // example, user entered,WLP_LOGGING_CONSOLE_SOURCE=abc,audit,message,kyc
            // quickfix should contain something like
            //      replace with "audit,message
            //      replace with "audit,message,trace"
            //      replace with "audit,message,ffdc"
            //      replace with "audit,message,auditLog"
            List<String> retainValues = new ArrayList<>(validValues);
            retainValues.retainAll(enteredValues);
            retainValues.sort(Comparator.comparingInt(
                    ServerPropertyValues.LOGGING_SOURCE_VALUES::indexOf));
            setMultiValuePrefix(String.join(",", retainValues));
        }
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public Integer getStartChar() {
        return this.startChar;
    }

    public Integer getEndChar() {
        return this.endChar;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public boolean hasWarnings() {
        return hasWarnings;
    }

    public boolean hasDiagnostics() {
        return hasErrors || hasWarnings;
    }

    public String getKey() {
        return entry.getKey();
    }

    public String getValue() {
        return entry.getValue();
    }

    /**
     * Get the diagnostic type, documented in DiagnosticMessages.properties
     * @return String indicating the type of diagnostic
     */
    public String getDiagnosticType() {
        return diagnosticType;
    }

    public String getMultiValuePrefix() {
        return multiValuePrefix;
    }

    public void setMultiValuePrefix(String multiValuePrefix) {
        this.multiValuePrefix = multiValuePrefix;
    }

    public String getCustomValue() {
        return customValue;
    }

    public void setCustomValue(String customValue) {
        this.customValue = customValue;
    }
}
