package config

/**
 * Created by chanjinpark on 15. 3. 14..
 */
object Configuration {
  val gitlogs = "gitlogs"
  val gitsrc = "gitsrc"
  val projects = List("junit")

  def logfile(pname: String) = {
    gitlogs + "/" + pname + ".txt"
  }

  def srcdir(p: String) = gitsrc + "/" + p
  def jarfile(p: String, jar: String) = gitsrc + "/" + p + "/" + jar

  def commitdir(pname: String) = {
    val dir = gitlogs + "/" + pname
    // TODO: if dir not exists then mkdir
    dir
  }

  def gitrepo(pname: String) = {
    "/Users/chanjinpark/GitHub/" + pname + "/.git"
  }

}
