package gitlog

import org.joda.time.DateTime

/**
 * Created by chanjinpark on 15. 3. 16..
 */

class FileChange(f: String, cid: String) {
  // times, amount, period
  // co-changed (a, 4) 파일 a와 4번 변경됨
  val name = f
  var birth = cid //commit id
  var death = ""

  //var changes = Map[String, List[GitDiff]]()
  var cochanges = Map[String, List[FileChange]]()
  var renamedfrom: (GitCommit, FileChange) = (null, null)
  var times = 0
  var amount = 0

  def summary = name + " (" + times + ", " + amount + "): \t" + birth + "~" + death + ", cochanged#" + cochanges.size //.values.mkString("\n\t")
}

import ChangeType._

object GitChanges {

  def getchanges(commits: List[GitCommit], details: Map[String, List[GitDiff]]) : Map[String, FileChange] = {
    var fchanges = Map[String, FileChange]()

    // commits are ordered chronically with commit date not author date
    commits.foreach(c => {
      val diffs = details(c.commitid)
      var cochanged = List[FileChange]()
      diffs.foreach(d => {
        d.changetype match {
          case Added => {
            //if (d.to.contains("/") && d.to.substring(d.to.lastIndexOf("/") + 1).startsWith("LICENSE")) println("Added. " + d.to + " " + c.date + " " + c.commitid)
            if (fchanges.contains(d.to)) {
              fchanges -= d.to
              println("duplicate added - " + c.date + ": " + d.to + " --- " + c.commitid)
            }
            //assert(!fchanges.contains(d.to), c.summary + ": " + d.summary)
            val fc = new FileChange(d.to, c.commitid)
            fchanges += (d.to -> fc)

            fc.times += 1
            fc.amount += d.nadd
            if (d.ndel > 0) {
              println("file added but it has deletion lines (" + d.ndel + ", " + c.date + "), " + d.to + ", " + c.commitid)
            }
            cochanged ::= fc
          }

          case Deleted => {
            //if (d.from.contains("/") && d.from.substring(d.from.lastIndexOf("/")+1).startsWith("LICENSE")) println("Deleted. " + d.from + " " + c.date + " " +c.commitid)
            if (!fchanges.contains(d.from)) {
              println("duplicate deleted - " + d.from + " --- " + c.commitid)
              fchanges += (d.from -> (new FileChange(d.from, c.commitid)))
            }
            val fc = fchanges(d.from)
            fc.death = c.commitid
            fc.times += 1
            fc.amount += d.ndel
            fchanges -= d.from

            cochanged ::= fc
          }

          case Modified => {
            //assert(d.from.equals(d.to), "Modified. from should equal to to")
            //if (d.to.contains("/") && d.to.substring(d.to.lastIndexOf("/") + 1).startsWith("LICENSE")) println("Modified. " + c.date + " " + d.to + ", " + c.commitid)
            if (!fchanges.contains(d.to)) {
              //assert(fchanges.contains(d.to), c.summary + " --- " + d.summary)
              println("simultaneous file modification and addition. " + d.to + ", " + c.date + "), " + c.commitid )
              fchanges += (d.to -> (new FileChange(d.to, c.commitid)))
            }
            val fc = fchanges(d.to)
            fc.times += 1
            fc.amount += (d.ndel + d.nadd)
            cochanged ::= fc
          }

          case Renamed => {
            /*if (d.from.contains("/") && d.from.substring(d.from.lastIndexOf("/") + 1).startsWith("LICENSE")) {
              println("Renamed. " + d.from + ", " + c.commitid)
              println("\t" + d.to + " " + c.date)
            }*/

            if (!fchanges.contains(d.from)) {
              println("duplicate rename commit. might be rename commit in other branch for already renamed file")
              println("\t" + d.from + "->" + d.to + " in " + c.commitid)

              //later commit needs to be reflected
              //from has been removed by previous commit
              val prev_to = fchanges(d.to)
              val prev_from = prev_to.renamedfrom._2
              val amount = prev_from.amount + (d.nadd + d.ndel)

              prev_to.amount = if (prev_to.amount > amount) prev_to.amount else amount
            }
            else {
              val fcfrom = fchanges(d.from)
              fcfrom.death = c.commitid

              val fc = new FileChange(d.to, c.commitid)
              fchanges += (d.to -> fc)

              fc.times = fcfrom.times + 1
              fc.amount = fcfrom.amount + ( d.nadd + d.ndel )
              fc.renamedfrom = (c, fcfrom)
              cochanged ::= fc

              fchanges -= d.from
            }
          }
          case _ => {
            assert(false)
          }
        }
      })
      if (cochanged.length > 0) {
        val coset = cochanged.distinct
        coset.foreach(cc => cc.cochanges += (c.commitid -> coset))
      }
    })

    fchanges
  }

  def changesAfter(p: String, cid: String, paths: List[String], extensions: List[String]) = {
    val commits = GitCommitLog.getCommits(p)
    val clist = commits.values.toList.sortWith((c, c1) => c.isBefore(c1))
    //println(clist.map(_.summary).mkString("\n"))
    println(clist.head.summary)
    println(clist.last.summary)

    val details = GitCommitDetails.getall(p, clist)

    val (before, after) = clist.span(!_.commitid.equals(cid))
    val changes = {
      val chs = getchanges(clist, details)
      chs.filter(f => paths.exists(p => f._1.startsWith(p)) && extensions.exists(ext => f._1.endsWith(ext)))
    }

    println(changes.map(_._2.summary).mkString("\n"))

    val changesbefore = {
      val chs = getchanges(before, details)
      chs.filter(f => paths.exists(p => f._1.startsWith(p)) && extensions.exists(ext => f._1.endsWith(ext)))
    }
    //println(changesbefore.map(_._2.summary).mkString("\n"))

    (commits, details, changes, changesbefore)
  }

  def changes(p: String): (Map[String, GitCommit], Map[String, List[GitDiff]], Map[String, FileChange])  = {
    val commits = GitCommitLog.getCommits(p)
    val clist = commits.values.toList.sortWith((c, c1) => c.isBefore(c1))
    val details = GitCommitDetails.getall(p, clist)

    (commits, details, getchanges(commits.values.toList,details))
  }

  def main(args: Array[String]): Unit = {
    //1. (init, c , head) 파일별 변경량을 조사
    val commits = GitCommitLog.getCommits("junit")
    val clist = commits.values.toList.sortWith((c, c1) => c.isBefore(c1))
    val details = GitCommitDetails.getall("junit", clist)
    val changes = getchanges(commits.values.toList, details)
    println(changes.values.map(_.summary).mkString("\n"))
    //def
  }
}