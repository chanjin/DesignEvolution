package gitlog

import java.io.{BufferedWriter, FileWriter, FileOutputStream, File}

import config.Configuration
import gitlog.ChangeType._

/**
 * Created by chanjinpark on 15. 4. 7..
 */

class FileWiseChange(n: String) {
  val name = n
  var changes = List[(Int, Int, ChangeType)]() // order, amount, changetype, 언제, 얼마나
  def getChangesAmount =  changes.foldLeft(0)((s, c) => s + c._2)
  def getChangesOrdered = changes.sortBy(_._1)
  var renamedfrom: FileWiseChange = null

  def todata = (name, changes.size, getChangesAmount, getChangesOrdered)
  def tocsv = name + ", " + changes.size + ", " + getChangesAmount + ", " + getChangesOrdered.mkString(", ")
  override def toString = name + ": " + changes.size + " times, " + getChangesAmount + " lines\n\t" + getChangesOrdered.mkString(", ")
}

class CommitWiseChange(ord: Int) {
  val order = ord
  var changes = List[(String, Int, ChangeType)]() // file, amount, changetype
  def getChangesAmount = changes.foldLeft(0)((s, c) => s + c._2)
  def getChangesOrdered = changes.sortBy(_._1)
  def todata = (order, changes.size, getChangesAmount, getChangesOrdered)
  def tocsv = order + ", " + changes.size + ", " + getChangesAmount + ", " + getChangesOrdered.mkString(", ")
  override def toString = order + ", " + changes.size + " files, " + getChangesAmount + " lines, " + getChangesOrdered.mkString(", ")
}

class FileChangeData(p: String) {
  val commits = GitCommitLog.getCommits(p)
  val details = GitCommitDetails.getall(p, commits.values.toList.sortWith((c, c1) => c.isBefore(c1)))

  def tocsvheader = "name, times, amount, changelogs"

  val paths = Configuration.classpath(p)
  def filterExtension(file: String) = {
    paths.exists(p => file.startsWith(p)) && Configuration.extension.exists(ext => file.endsWith(ext))
  }


  def commitwiseChanges = {
    var cchanges = Map[Int, CommitWiseChange]()
    commits.values.foreach(c => {
      val cc = new CommitWiseChange(c.order)
      cchanges += (c.order -> cc)
      val diffs = details(c.commitid)
      diffs.foreach(d => {
        if (filterExtension(d.from) || filterExtension(d.to)) {
          d.changetype match {
            case Added => cc.changes ::= (d.to, d.nadd, Added)
            case Deleted => cc.changes ::= (d.from, 0, Deleted)
            case Modified => cc.changes ::= (d.to, d.ndel + d.nadd, Modified)
            case Renamed => {
              cc.changes ::= (d.from, 0, Deleted)
              cc.changes ::= (d.to, d.nadd + d.ndel, Renamed)
            }
            case _ => {
              assert(false)
            }
          }
        }
      })
    })
    cchanges.filter(kv => kv._2.changes.size > 0)
  }

  def filewiseChanges = {
    var fchanges = Map[String, FileWiseChange]()
    def getFileChange(f: String) = {
      if ( fchanges.contains(f) ) fchanges(f) else {
        val newfc = new FileWiseChange(f)
        fchanges += (f -> newfc)
        newfc
      }
    }

    commits.values.foreach(c => {
      val diffs = details(c.commitid)
      val cochfiles = c.changedfs // List[String, Char], Char는 ACDMRTUX
      diffs.foreach(d => {
        if (filterExtension(d.from) || filterExtension(d.to)) {
          d.changetype match {
            case Added => getFileChange(d.to).changes ::=(c.order, d.nadd, Added)
            case Deleted => getFileChange(d.from).changes ::=(c.order, 0, Deleted)
            case Modified => getFileChange(d.to).changes ::=(c.order, d.ndel + d.nadd, Modified)
            case Renamed => {
              val fcfrom = getFileChange(d.from)
              fcfrom.changes ::=(c.order, 0, Deleted)
              val fc = getFileChange(d.to)
              fc.changes ::=(c.order, d.nadd + d.ndel, Renamed)

              fc.renamedfrom = fcfrom
            }
            case _ => {
              assert(false)
            }
          }
        }
      })
    })
    fchanges.filter(kv => kv._2.changes.size > 0)
  }



  def commitfileChanges = {

  }

  def generateFilewiseCSV = {
    val writer = new BufferedWriter(new FileWriter(new File(Configuration.csvfile(p, "filewise"))))
    filewiseChanges.values.foreach(c => writer.write(c.tocsv + "\n"))
    writer.close()
  }

  def generateCommitwiseCSV = {
    val writer = new BufferedWriter(new FileWriter(new File(Configuration.csvfile(p, "commitwise"))))
    commitwiseChanges.values.foreach(c => writer.write(c.tocsv + "\n"))
    writer.close()
  }


  def printChanges = {
    val fcs = filewiseChanges.values.toList.sortBy(_.name)
    fcs.foreach(f => println(f.toString))
    println("---------------------")
    val ccs = commitwiseChanges.values.toList.sortBy(_.order)
    ccs.foreach(c => println(c.toString))
    println("---------------------")
    println("Total changes (file# x change times, amount) filewise - " + fcs.foldLeft((0, 0))((s, fc) => (s._1 + fc.changes.size,s._2 + fc.getChangesAmount)))
    println("Total changes (file# x change times, amount) commitwise - " + ccs.foldLeft((0, 0))((s, cc) => (s._1 + cc.changes.size,s._2 + cc.getChangesAmount)))
  }

  def changesOverall = {

  }
}

object FileChangeData {
  def main(args: Array[String]) = {
    val p = "junit"
    val csv = new FileChangeData(p)
    //csv.printChanges
    csv.generateFilewiseCSV
    csv.generateCommitwiseCSV
  }
}
