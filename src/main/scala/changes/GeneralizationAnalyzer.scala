package changes

import gitlog.{GitCommit, FileChange, GitChanges}

/**
 * Created by chanjinpark on 15. 3. 18..
 */
class GeneralizationAnalyzer(commitmap: Map[String, GitCommit]) {

  def analyzer_origin(c: GitCommit, ch: Map[String, FileChange], chb: Map[String, FileChange]) = {
    val renamed = ch.filter(f => !chb.contains(f._1))
    println("파일 수 - 현재: "+ ch.size + ", 이전: " + chb.size + ", 차이 - " + (ch.size - chb.size))
    //println(renamed.size + "\n" + renamed.map(r => commitmap(r._2.birth).order + ", " + r._2.birth + ", " + r._1 ).mkString("\n"))

    def getOriginAt(c: GitCommit, fc: FileChange) = {
      var org = fc.renamedfrom
      var found = false
      while ( org._1 != null && !found ) {
        // more recent commit has lower order
        if ( org._1.isBefore(c) ) found = true
        else
          org = org._2.renamedfrom
      }
      if (found) println("\tfound " + org._1 + ", " + org._2.summary)
      else {
        //println("\tnot found " + commitmap(fc.birth).date)
        assert(!commitmap(fc.birth).isBefore(c) || commitmap(fc.birth) != c, "나중에 생긴 파일이어야 함 " + commitmap(fc.birth).summary )
      }
      (found, org._2, fc)
    }

    val mapped = renamed.map(f => getOriginAt(c, ch(f._1))).filter(_._1)

    println("----------- " + mapped.size)
    println(mapped.map(o => o._1 + " -> " + o._2.summary).mkString("\n"))

    // 커밋 c 이 후로 새로 생긴 파일 들, 이름이 바뀐 파일 들

  }
}

/*
  1. 특정 commit id에 대해 설계 정보를 추출한다 - Generalization 관계, Dependency 정보
  2. 클래스별 이 후 변경량을 조사한다.
  3. 다음을 확인한다. (g, s)
    1) 재사용성 - 클래스 평균 변경량에 비해 Super 클래스의 변경량이 현저하게 작은가? 비슷하거나 큰 경우, 어떤 특징을 가지고 있는가?
      g의 재사용성, g의 변경이 적어야 한다. c라는 클래스를 g + s로 나누고, g는 변경없이 재사용되기를 기대.
    2) 독립적인 변경 - s 변경은 g 또는 s의 sibling과 같이 변경되지 않는다. s와 g & s sibling 간의 co-change 횟수가 다른 클래스와의 co-change 횟수보다 현저하게 적은가?
      s의 독립적 변경.
    3) 확장성 - g의 변경없이 s sibling들이 추가되는가? s sibling s'의 Addition을 체크
    4) 좋은 generalization을 정량화할 수 있는가? g에 대해 정량적으로 scoring.

  Next, 여러 프로젝트로부터 좋은 generalization은 어떤 특성을 가지는 지를 분석
  구조적 특성 (메소드 수(g,s), s의 수, g의 외부와의 의존관계 수, s의 외부 와의 의존관계수, Violation, Cycle) => g 점수
 */

object GeneralizationAnalyzer {
  def main(args: Array[String]) = {
    // junit 4.11 c2e4d911fadfbd64444fb285342a8f1b72336169,  @marcphilipp marcphilipp released this on 15 Nov 2012 · 747 commits to master since this release
    val cid = "c2e4d911fadfbd64444fb285342a8f1b72336169"
    val (c, cd, ch, chbefore) = GitChanges.changesAfter("junit", cid)
    println("전체 commits 수 - " + c.size)
    val analyzer = new GeneralizationAnalyzer(c)
    analyzer.analyzer_origin(c(cid), ch, chbefore)
  }
}
