sonatypeProfileName := "com.github.ancane"

publishMavenStyle := true

publishArtifact in Test := false

pomExtra := {
  <url>https://github.com/ancane/haldr</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com:ancane/haldr</connection>
      <developerConnection>scm:git:git@github.com:ancane/haldr</developerConnection>
      <url>https://github.com/ancane/haldr</url>
    </scm>
    <developers>
      <developer>
        <id>ancane</id>
        <email>igor.shimko@gmail.com</email>
        <name>Igor Shymko</name>
        <url>https://github.com/ancane</url>
      </developer>
    </developers>
}
