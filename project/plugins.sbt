addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"        % "0.14.2")
addSbtPlugin("com.codecommit"    % "sbt-github-packages" % "0.5.3")
addSbtPlugin("com.github.sbt"    % "sbt-git"             % "2.1.0")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager" % "1.11.1")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"         % "0.6.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.5.4")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
