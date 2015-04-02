package experiments

import java.io.File

import config.Configuration
import gitlog.{GitLogGenerator, GitCommit}

import scala.io.Source

/**
 * Created by chanjinpark on 15. 4. 2..
 */
object InterfaceHasLessChanges {


  def generateLogs = {
    Configuration.init
    Configuration.projs.keys.foreach(p => {
      println("Log generations - " + p)
      GitLogGenerator.generateLogs(p)
      val commits = GitCommit.from(Source.fromFile(Configuration.logfile(p)).getLines())
      if ( !(new File(Configuration.commitdir(p))).exists )
        commits.foreach(c => GitLogGenerator.generateCommitDetails(p, c._2.commitid))
    })
  }

  // interface 클래스의 변경은 다른 클래스의 변경 보다 작은가?
  // 같은 hierarchy 내의 implementation 클래스의 변경은 interface 클래스 변경 보다 큰가?
  // 대상 프로젝트 - junit, hadoop, tomcat, hive
  def main(args: Array[String]) = {
    // generateLogs


  }
}
