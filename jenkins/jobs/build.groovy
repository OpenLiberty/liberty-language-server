pipelineJob("LibertyLanguageServerBuild") {
   description("Builds liberty-language-server.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/liberty-language-server.git", "main")
         }
         scriptPath("jenkins/build.jenkinsfile")
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
