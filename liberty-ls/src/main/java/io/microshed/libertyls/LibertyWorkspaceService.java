package io.microshed.libertyls;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public class LibertyWorkspaceService implements WorkspaceService {

  private final LibertyLanguageServer libertyLanguageServer;

  public LibertyWorkspaceService(LibertyLanguageServer libertyls) {
    this.libertyLanguageServer = libertyls;
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    libertyLanguageServer.updateSettings(params.getSettings());
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    // Do nothing
  }

}
