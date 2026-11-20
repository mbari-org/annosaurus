

final class nats$minusmonitor$_ {
def args = nats$minusmonitor_sc.args$
def scriptPath = """./nats-monitor.sc"""
/*<script>*/


//> using dep "io.nats:jnats:2.25.2"

import io.nats.client.Nats
import java.nio.charset.StandardCharsets
import java.time.Instant


if (args.length != 2) {
    println("Usage: nats-monitor <url> <topic>")
    println("Example: nats-monitor nats://localhost:4222 'vars'")
    System.exit(1)
}
val (url, topic) = (args(0), args(1))

println(s"Connecting to $url ...")
val nc = Nats.connect(url)
println(s"Subscribed to '$topic'. Waiting for messages (Ctrl+C to exit)...")

val dispatcher = nc.createDispatcher { msg =>
    val timestamp = Instant.now()
    val payload   = new String(msg.getData, StandardCharsets.UTF_8)
    println(s"[$timestamp] ${msg.getSubject}: $payload")
}
dispatcher.subscribe(topic)

Runtime.getRuntime.addShutdownHook(new Thread(() =>
    println("\nShutting down...")
    nc.close()
))

Thread.currentThread().join() // block until Ctrl+C

/*</script>*/ /*<generated>*//*</generated>*/
}

object nats$minusmonitor_sc {
  private var args$opt0 = Option.empty[Array[String]]
  def args$set(args: Array[String]): Unit = {
    args$opt0 = Some(args)
  }
  def args$opt: Option[Array[String]] = args$opt0
  def args$: Array[String] = args$opt.getOrElse {
    sys.error("No arguments passed to this script")
  }

  lazy val script = new nats$minusmonitor$_

  def main(args: Array[String]): Unit = {
    args$set(args)
    val _ = script.hashCode() // hashCode to clear scalac warning about pure expression in statement position
  }
}

export nats$minusmonitor_sc.script as `nats-monitor`

