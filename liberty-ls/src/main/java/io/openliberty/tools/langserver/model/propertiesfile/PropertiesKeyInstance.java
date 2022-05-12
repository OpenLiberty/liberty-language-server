/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.model.propertiesfile;

import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;
import io.openliberty.tools.langserver.resources.ServerEnvOptions;

public class PropertiesKeyInstance {

    private String propertyKey;
    private PropertiesEntryInstance propertyEntryInstance;
    private LibertyTextDocument textDocumentItem;

    public static ResourceBundle serverenvKeys = ResourceBundle.getBundle("io.openliberty.tools.langserver.resources.ServerEnvOptions");
    public static ResourceBundle bootstrapKeys = ResourceBundle.getBundle("io.openliberty.tools.langserver.resources.BootstrapPropertiesOptions");

    public PropertiesKeyInstance(String propertyKeyInstanceString, PropertiesEntryInstance propertyEntryInstance, LibertyTextDocument textDocumentItem) {
        this.propertyKey = propertyKeyInstanceString;
        this.propertyEntryInstance = propertyEntryInstance;
        this.textDocumentItem = textDocumentItem;
    }

    public int getEndPosition() {
        return propertyKey.length();
    }

    public CompletableFuture<Hover> getHover() {
        Hover hover = new Hover();
        String message = null;
        if (textDocumentItem.getUri().endsWith("properties")) {
            message = bootstrapKeys.getString(propertyKey);
        } else {
            message = serverenvKeys.getString(propertyKey);
        }
        hover.setContents(new MarkupContent("markdown", message));
        return CompletableFuture.completedFuture(hover);
    }

    @Override
    public String toString() {
        return this.propertyKey;
    }
}
