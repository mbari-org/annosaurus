#!/usr/bin/env -S scala shebang

//> using dep "io.nats:jnats:2.25.2"

import io.nats.client.Nats
import java.nio.charset.StandardCharsets
import java.time.Instant


if (args.length != 2) {
    println("Usage: nats-monitor <url> <topic>")
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
