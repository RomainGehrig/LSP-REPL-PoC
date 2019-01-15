package repl

import sun.misc.{Signal, SignalHandler}
import java.io.{ File => JFile, InputStream, OutputStream, PrintWriter }
import java.net._
import java.nio.channels._
import java.util.concurrent.{CompletableFuture, CancellationException}

import org.eclipse.lsp4j
import org.eclipse.lsp4j._
import org.eclipse.lsp4j.services._
import org.eclipse.lsp4j.launch._
import org.eclipse.lsp4j.jsonrpc.Launcher

import scala.collection._
import scala.collection.JavaConverters._
import scala.language.implicitConversions

import jupyterlsp._

import dotty.tools.repl.server


trait Server extends LanguageServer with TextDocumentService with ReplService

object LspReplClient {
  def apply(port: Int): LspReplClient = {
    val client = new LspReplClient

    val writer = new PrintWriter(new JFile("lsp-client.log"))
    // val writer = new PrintWriter(System.err, true)

    val socket = new Socket("localhost", port)

    val launcher = Launcher.createLauncher(client, classOf[Server],
                                           socket.getInputStream, socket.getOutputStream, /*validate =*/ false,  writer)
    launcher.startListening()
    val server = launcher.getRemoteProxy
    client.server = server

    val params = new InitializeParams
    // TODO What should the rootUri be ? (question for all ReplClients)
    params.setRootUri(System.getProperty("user.dir"))
    server.initialize(params)

    client
  }
}

class LspReplClient extends ReplClient { thisClient =>
  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}

  private var server: Server = _

  def currentLine: Int = count
  @volatile private var count = 0

  def start(): Unit = {
    println("ReplStarted !")

    while (true) {
      val code = scala.io.StdIn.readLine("lsp-repl-poc> ")

      // Code completion
      if (code.endsWith("?")) {
        val position = code.length - 1
        val futureCompletions = server.replCompletion(ReplCompletionParams(position=position, code=code.init))
        val completionsEither = futureCompletions.get()
        val completions: List[CompletionItem] = (if (completionsEither.isLeft) completionsEither.getLeft
                                                 else completionsEither.getRight().getItems).asScala.toList
        println(completions.map(_.getLabel).distinct.mkString("  –  "))
      } else {
        // Code execution
        var futureResult = server.interpret(ReplInterpretParams(code))

        val waitingInterrupt = new CompletableFuture[Boolean]()
        val runInterrupt = futureResult.thenCombine(waitingInterrupt, (replRes: ReplInterpretResult, _: Boolean) =>  replRes.runId)
          .thenCompose{ runId: Int => {
                         server.interruptRun(ReplRunIdentifier(runId))
                       }
          }

        interruptibleFuture(waitingInterrupt) {
          var result = futureResult.get()
          print(result.output)
          while (result.hasMore && !futureResult.isCancelled) {
            futureResult = server.interpretResults(ReplRunIdentifier(result.runId))
            CompletableFuture.anyOf(futureResult, runInterrupt).get match {
              case int: ReplInterruptResult =>
                println(int.stacktrace)
                futureResult.cancel(true)
              case res: ReplInterpretResult =>
                result = res
                print(result.output)
            }
          }
        }
      }
    }
  }

  private def interruptibleFuture[T](future: => CompletableFuture[Boolean])(t: => T): T = {
    val oldHandler = sun.misc.Signal.handle(new Signal("INT"),
                                            new SignalHandler() {
                                              def handle(sig: Signal) = {
                                                future.complete(true)
                                              }
                                            })
    try {
      t
    } finally {
      sun.misc.Signal.handle(new Signal("INT"), oldHandler)
    }
  }

  // Lsp client
  override def logMessage(params: MessageParams): Unit = {}
  override def showMessage(params: MessageParams): Unit = {}
  override def showMessageRequest(params: ShowMessageRequestParams): CompletableFuture[MessageActionItem] = null
  override def publishDiagnostics(params: PublishDiagnosticsParams): Unit = {}
  override def telemetryEvent(params: Any): Unit = {}
}
