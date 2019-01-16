LSP REPL Proof-of-Concept
---------

It works !

Need to run in Dotty's sbt: `repl-server/publishLocal` to have the LSP API available here. Then, still in Dotty's sbt: `repl-server/run 12555` to run the LSP Repl server (need to run a new instance for each new connection)

You can now run `sbt run` in this project to launch a LSP-backed REPL.

* `<Enter>` to run the code
* `printl?<Enter>` to ask for the completion of `printl`
* `<CTRL>+C` to issue an interrupt request when code is running
*  Quit with `<CTRL>+C` twice.
