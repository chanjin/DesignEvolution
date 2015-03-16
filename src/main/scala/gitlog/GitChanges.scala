package gitlog

import org.joda.time.DateTime

/**
 * Created by chanjinpark on 15. 3. 16..
 */

class FileChange(f: String, date: DateTime) {
  // times, amount, period
  // co-changed (a, 4) 파일 a와 4번 변경됨
  val name = f
  var birth = date
  var death: DateTime = null

  //var changes = Map[String, List[GitDiff]]()
  var cochanges = Map[String, List[FileChange]]()
  var renamedfrom: FileChange = null
  var times = 0
  var amount = 0

  def summary = name + "(" + times + ", " + amount + "): \t" + birth + "~" + death + ", cochanged#" + cochanges.size //.values.mkString("\n\t")
}

import ChangeType._
object GitChanges {
  def getchanges(commits: List[GitCommit], details: Map[String, List[GitDiff]]) : Map[String, FileChange] = {
    var fchanges = Map[String, FileChange]()
    var duplicates = Map[(String, String), FileChange]()
    commits.sortWith((c1, c2) => c1.date.isBefore(c2.date)).foreach(c => {
      val diffs = details(c.commitid)
      var cochanged = List[FileChange]()
      diffs.foreach(d => {
        d.changetype match {
          case Added => {
            if (fchanges.contains(d.to)) {
              duplicates += ((d.to, c.commitid) -> fchanges(d.to))
              fchanges -= d.to
              println("duplicate added - " + d.to + " --- " + c.commitid)
            }
            //assert(!fchanges.contains(d.to), c.summary + ": " + d.summary)
            val fc = new FileChange(d.to, c.date)
            fchanges += (d.to -> fc)

            fc.times += 1
            fc.amount += d.nadd
            if (d.ndel == 0) {
              println("file added but it has deletion lines " + d.to + ", " + c.commitid)
            }
            cochanged ::= fc
          }
          case Deleted => {
            assert(fchanges.contains(d.from))
            val fc = fchanges(d.from)
            fc.death = c.date
            fc.times += 1
            fc.amount += d.ndel
            cochanged ::= fc
          }
          case Modified => {
            if (!fchanges.contains(d.to)) {
              //assert(fchanges.contains(d.to), c.summary + " --- " + d.summary)
              println("file modified that has not been added (maybe simultaneously file modified and added. " + d.to + ", " + c.commitid )
              fchanges += (d.to -> (new FileChange(d.to, c.date)))
            }
            val fc = fchanges(d.to)
            fc.times += 1
            fc.amount += (d.ndel + d.nadd)
            cochanged ::= fc
          }
          case Renamed => {
            assert(fchanges.contains(d.from), "renamed so that " + d.from + " should have been added")
            if (fchanges.contains(d.to)) {
              duplicates += ((d.to, c.commitid) -> fchanges(d.to))
              fchanges -= d.to
              println("file renamed to that has already been added (maybe simultaneously file renamed and added " + d.to + ", " + c.commitid)
            }
            val fcfrom = fchanges(d.from)
            fcfrom.death = c.date
            val fc = new FileChange(d.to, c.date)
            fchanges += (d.to -> fc)

            fc.times = fcfrom.times + 1
            fc.amount = fcfrom.amount + d.nadd
            cochanged ::= fc
            cochanged ::= fcfrom
          }
          case _ => {
            assert(true)
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

  def main(args: Array[String]): Unit = {
    //1. (init, c , head) 파일별 변경량을 조사
    val commits = GitCommitLog.getCommits("junit")
    val details = GitCommitDetails.getall("junit")
    val changes = getchanges(commits, details)
    println(changes.values.map(_.summary).mkString("\n"))
    //def
  }
}