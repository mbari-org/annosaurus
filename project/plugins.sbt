// https://github.com/earldouglas/xsbt-web-plugin
addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.0.2")

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.12")

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0")

// http://www.scalastyle.org/sbt.html
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.4.0")

// https://github.com/atais/sbt-eclipselink-static-weave
addSbtPlugin("com.github.atais" % "sbt-eclipselink-static-weave" % "0.1.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.4")

resolvers += Resolver.sonatypeRepo("releases")
