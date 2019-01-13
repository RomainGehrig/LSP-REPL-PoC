val dottyVersion = "0.11.0-RC1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "lsp-repl-poc",
    version := "0.1.0",

    connectInput in run := true,
    outputStrategy := Some(StdoutOutput),

    // fork in run := true,
    // fork in Test := true,

    scalaVersion := dottyVersion,

    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "ch.epfl.lamp" % "repl-server_0.11" % "0.11.0-bin-SNAPSHOT",
      "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.5.0"
    )
  )
