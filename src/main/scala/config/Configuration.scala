package config

import java.io.File

/**
 * Created by chanjinpark on 15. 3. 14..
 */
object Configuration {
  val gitlogs = "./gitlogs"
  val gitsrc = "./gitsrc"

  val extension = List("java", "scala")

  private val junit = ("junit", ("junit", List("src/main/java/")))
  private val hadoop = ("hadoop", ("apache-hadoop", List[String]()))
  private val tomcat = ("tomcat", ("apache-tomcat", List[String]()))
  //private val hive = ("hive", "apache-hive")

  val projs = List(junit, hadoop, tomcat).toMap[String, (String, List[String])]

  private def mkdir(p: String) = {
    val dir = new File(p)
    if (!dir.exists()) dir.mkdir()
  }

  def init =  projs.keys.foreach(p => mkdir("./" + gitlogs + "/" +p))
  def logfile(p: String) = gitlogs + "/" + p + ".txt"
  def csvfile(p: String, name: String) = gitlogs + "/" + p + "_" + name +".csv"
  def srcdir(p: String) = gitsrc + "/" + p
  def jarfile(p: String, jar: String) = gitsrc + "/" + p + "/" + jar
  def commitdir(p: String) = gitlogs + "/" + p

  def classpath(p: String): List[String] = projs(p)._2
  def gitrepo(p: String) = {
    "/Users/chanjinpark/GitHub/" + projs(p)._1 + "/.git"
  }

}
