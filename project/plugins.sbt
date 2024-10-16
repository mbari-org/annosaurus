addSbtPlugin("com.timushev.sbt"  % "sbt-updates"         % "0.6.4")
addSbtPlugin("com.github.sbt"    % "sbt-git"             % "2.0.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("com.codecommit"    % "sbt-github-packages" % "0.5.3")
addSbtPlugin("com.github.sbt"    % "sbt-native-packager" % "1.10.4")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
