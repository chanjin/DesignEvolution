package gitlog

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.io.Source

/**
 * Created by chanjinpark on 15. 3. 14..
 */
/*
commit a90b496a6595856066504baf4f737fb853a6e45d
Author: Sam Brannen <sam@sambrannen.com>
Date:   Sun Feb 15 21:53:52 2015 +0100

    Ensure exceptions from methodBlock() don't result in unrooted tests.

    The introduction of the runLeaf() method in BlockJUnit4ClassRunner in
    JUnit 4.9 introduced a regression with regard to exception handling.

    Specifically, the invocation of methodBlock() is no longer executed
    within a try-catch block as was the case in previous versions of JUnit.

    Custom modifications to methodBlock() or the methods it invokes may in
    fact throw exceptions. In such cases, exceptions thrown from
    methodBlock() cause the current test execution to abort immediately. As
    a result, the failing test method is unrooted in test reports, and
    subsequent test methods are never invoked. Furthermore, RunListeners
    registered with JUnit are not notified.

    This commit addresses this issue by wrapping the invocation of
    methodBlock() within a try-catch block. If an exception is not thrown,
    the resulting Statement is passed to runLeaf(). If an exception is
    thrown, it is wrapped in a Fail statement which is passed to runLeaf().

    Closes #1066
    Closes #1082

M	src/main/java/org/junit/runners/BlockJUnit4ClassRunner.java
A	src/test/java/org/junit/runners/CustomBlockJUnit4ClassRunnerTest.java
M	src/test/java/org/junit/tests/AllTests.java
 */

class GitCommit(cid: String, a: String, d: DateTime, comm: List[String], fs: List[(String, Char)]) {
  val commitid = cid
  val author = a
  val date = d
  val comment = comm
  val changedfs = fs

  def summary = commitid + ": " + date + " by " + author +  ", " + changedfs.length + " files are changed"
}

object GitCommit {
  private def commitFrom(strs: List[String]): GitCommit = {
    val cid = strs.head.substring(7)
    var author = ""
    var parents: List[String] = null
    var date: DateTime = null
    var comments = List[String]()
    var changedfs = List[(String, Char)]()

    var lines = strs.tail

    val (header, remains) = lines span (l => (l.length > 0))

    header.foreach( l => {
      if (l.startsWith("Merge: ")) {
        parents = l.substring(7).split(" ").toList
      }
      else if (l.startsWith("Author: ")) {
        author = l.substring(8)
      }
      else if (l.startsWith("Date: ")) {
        // Sun Feb 15 21:53:52 2015 +0100
        val pattern = "EEE MMM dd HH:mm:ss YYYY"
        date = DateTime.parse(l.substring(6).trim, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z"))
      }
      else {
        assert(true, "New header component " + l)
      }
    })

    val status = "ACDMRTUX"
    remains.foreach( l => {
      if (l.length > 2 && status.contains(l.charAt(0)) && l.charAt(1) == '\t') {
        val fs = (l.substring(2), l.charAt(0))
        changedfs ::= fs
      }
      else
        comments ::= l
    })

    new GitCommit(cid, author, date, comments.reverse, changedfs.reverse)
  }

  def from(it: Iterator[String]) : List[GitCommit] = {
    var commits = List[GitCommit]()

    var beginning = true
    var strs = List[String]()
    while (it.hasNext) {
      val l = it.next
      if ( l.startsWith("commit ")) {
        if (!beginning) {
          commits ::= commitFrom(strs.reverse)
          strs = List[String]()
        }
        else {
          beginning = false
        }
      }
      strs = l :: strs
    }
    if (!beginning)
      commits ::= commitFrom(strs.reverse)

    commits.reverse
  }
}

object GitCommitLog {
  def getCommits(p: String): List[GitCommit] = {
    GitCommit.from(Source.fromFile("gitlogs/" + p + ".txt").getLines())
  }

  def main(args: Array[String]): Unit = {
    val commits = getCommits("junit")
    println(commits.map(_.summary).mkString("\n"))
  }
}