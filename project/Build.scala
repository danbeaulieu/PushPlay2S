import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "PushPlay2S"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe" %% "play-plugins-redis" % "2.1-1-RC2",
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    

    resolvers += "Sedis Repo" at "http://pk11-scratch.googlecode.com/svn/trunk"  
  )

}
