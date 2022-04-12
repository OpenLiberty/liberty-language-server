package io.openliberty.lemminx.liberty.models.settings;

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
