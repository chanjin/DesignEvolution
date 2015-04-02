package config

import java.io.File

/**
 * Created by chanjinpark on 15. 3. 14..
 */
object Configuration {
  val gitlogs = "./gitlogs"
  val gitsrc = "./gitsrc"

  private val junit = ("junit", "junit")
  private val hadoop = ("hadoop", "apache-hadoop")
  private val tomcat = ("tomcat", "apache-tomcat")
  private val hive = ("hive", "apache-hive")

  val projs = List(junit, hadoop, tomcat).toMap[String, String]

  private def mkdir(p: String) = {
    val dir = new File(p)
    if (!dir.exists()) dir.mkdir()
  }

  def init =  projs.keys.foreach(p => mkdir("./" + gitlogs + "/" +p))

  def logfile(p: String) =
    gitlogs + "/" + p + ".txt"

  def srcdir(p: String) = gitsrc + "/" + p

  def jarfile(p: String, jar: String) = gitsrc + "/" + p + "/" + jar

  def commitdir(p: String) = gitlogs + "/" + p

  def gitrepo(p: String) = {
    "/Users/chanjinpark/GitHub/" + projs(p) + "/.git"
  }

}
