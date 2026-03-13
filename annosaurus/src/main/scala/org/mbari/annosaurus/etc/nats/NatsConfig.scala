package org.mbari.annosaurus.etc.nats

final case class NatsConfig(
    url: String,
    enable: Boolean,
    topic: String
)
