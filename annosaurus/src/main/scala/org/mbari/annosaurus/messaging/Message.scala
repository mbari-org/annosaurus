package org.mbari.annosaurus.messaging

trait Message[+A]:
    def content: A
    def toJson: String

