package org.mbari.annosaurus

final case class NatsConfig(
    url: String,
    enable: Boolean,
    subject: String
)
