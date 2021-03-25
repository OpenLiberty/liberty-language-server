/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
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

package io.microshed.libertyls.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * API of the client consuming the Liberty Language Server. Used to send
 * messages back to the client to ask for information about the Java project
 * Client then delegates that request to the IDEs built in java language support.
 */
public interface LibertyLanguageClientAPI extends LanguageClient {
    @JsonRequest("liberty/properties/hover")
    default CompletableFuture<Hover> getJavaHover(HoverParams params) {
        return CompletableFuture.completedFuture(null);
    }
}