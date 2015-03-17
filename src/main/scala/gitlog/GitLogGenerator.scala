package gitlog

/**
 * Created by chanjinpark on 15. 3. 13..
 */

import java.io.File

import config.Configuration

import scala.io.Source
import sys.process._

object GitLogGenerator {


  // git --git-dir /Users/chanjinpark/GitHub/junit/.git log --name-status --date=iso
  // git --git-dir /Users/chanjinpark/GitHub/junit/.git show --date=iso 47707e8d86ad0927f6c67472615646949d313ab3

  def generateLogs(pname: String) = {
    val f = Configuration.logfile(pname)
    val repo = Configuration.gitrepo(pname)
    val command = "git --git-dir " + repo + " log --name-status --date=iso"

    ( command #> new File(f) )!
  }

  def generateCommitDetails(pname: String, cid: String) = {
    val f = Configuration.commitdir(pname) + "/" + cid
    val repo = Configuration.gitrepo(pname)
    val command = "git --git-dir " + repo + " show -M --date=iso " + cid
    ( command #> new File(f) )!
  }

  def readCommitDetails(p: String) : Map[String, List[GitDiff]] = {
    GitCommitDetails.getall(p)
  }

  def storeLog2Mongo(pname: String) = {
    val f = Configuration.logfile(pname)

  }

  def storeCommit2Mongo = {

  }


  //--date-order
  def main(args: Array[String]): Unit = {
    // 1. .git의 리포지토리로부터 git 로그를 추출함.
    // 2. 로그를 파싱해서 데이터를 MongoDB에 넣는다
    generateLogs("junit")
    val commits = GitCommit.from(Source.fromFile("gitlogs/junit.txt").getLines())
    commits.foreach(c => {
      generateCommitDetails("junit", c.commitid)
    } )
  }
}
