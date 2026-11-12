pipelineJob("LibertyLanguageServerDependencyBuild") {
   description("Builds liberty-language-server dependency.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/liberty-language-server.git", "main")
         }
         scriptPath("jenkins/buildDependency.jenkinsfile")
         lightweight()
      }
   }
   disabled(false)
   configure {
      it / 'properties' / 'com.sonyericsson.rebuild.RebuildSettings' {
         'autoRebuild'('false')
         'rebuildDisabled'('false')
      }
   }
}
