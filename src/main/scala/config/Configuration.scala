package config

import java.io.File

/**
 * Created by chanjinpark on 15. 3. 14..
 */
object Configuration {
  val gitlogs = "gitlogs"
  val gitsrc = "gitsrc"

  val projects = List("junit")

  private def mkdir(dir: String) = {
    val dirf = new File(dir)
    if ( !dirf.exists() ) dirf.mkdir()
  }
  projects.foreach(p => {
    val logd = gitlogs + "/" + p
    val srcd = gitsrc + "/" + p
    mkdir(logd)
    mkdir(srcd)
  })

  def logfile(pname: String) = gitlogs + "/" + pname + ".txt"
  def srcdir(p: String) =  gitsrc + "/" + p
  def jarfile(p: String, jar: String) = gitsrc + "/" + p + "/" + jar
  def commitdir(pname: String) = gitlogs + "/" + pname
  def gitrepo(pname: String) = "/Users/chanjinpark/GitHub/" + pname + "/.git"
}
