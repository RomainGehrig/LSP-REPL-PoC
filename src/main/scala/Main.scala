package repl

import java.io.{ File => JFile, InputStream, OutputStream, PrintWriter, PrintStream }
import java.net._
import java.nio.channels._

import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.launch._
import org.eclipse.lsp4j.jsonrpc.Launcher

import jupyterlsp.ReplClient

import dotty.tools.repl.server

object Main {

  def startServer(port: Int): java.lang.Process = {
    val artifact = "ch.epfl.lamp:repl-server_0.11:0.11.0-bin-SNAPSHOT" // TODO Change hardcoded value
    val command = s"./coursier launch $artifact -M dotty.tools.repl.server.Main -- $port"

    val serverPB = new java.lang.ProcessBuilder(command.split(" "): _*)
    serverPB.redirectError(new JFile("lsp-server-printer.log"))
    serverPB.start()
  }

  def main(args: Array[String]): Unit = {
    // TODO Agree on port ?
    val port = 12555
    // val serverProcess = startServer(port)
    // Runtime.getRuntime().addShutdownHook(new Thread(
    //                                        new Runnable {
    //                                          def run: Unit = {
    //                                            serverProcess.destroy()
    //                                          }
    //                                        }));
    // Thread.sleep(2000) // Wait for the server to start (TODO is there a better way ?)
    val repl: LspReplClient = LspReplClient(port)
    repl.start()
  }
}
