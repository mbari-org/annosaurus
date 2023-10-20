addSbtPlugin("com.github.atais"  % "sbt-eclipselink-static-weave" % "0.1.2")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"                  % "0.6.3")
addSbtPlugin("com.github.sbt"  % "sbt-git"                      % "2.0.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"                   % "5.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"                 % "2.5.2")
// addSbtPlugin("org.xerial.sbt"    % "sbt-pack"                     % "0.13")
addSbtPlugin("com.codecommit"    % "sbt-github-packages"          % "0.5.3")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

libraryDependencies ++= Seq(
  "org.eclipse.persistence" % "org.eclipse.persistence.jpa" % "4.0.2"
)