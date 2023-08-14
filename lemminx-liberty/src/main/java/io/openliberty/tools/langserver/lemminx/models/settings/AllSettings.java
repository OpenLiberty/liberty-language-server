/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.models.settings;

import com.google.gson.annotations.JsonAdapter;
import org.eclipse.lsp4j.jsonrpc.json.adapters.JsonElementTypeAdapter;

/**
 * Model for top level xml settings JSON object.
 */
public class AllSettings {
    @JsonAdapter(JsonElementTypeAdapter.Factory.class)
    private Object liberty;

    public Object getLiberty() {
        return liberty;
    }

    public void setLiberty(Object liberty) {
        this.liberty = liberty;
    }

}
