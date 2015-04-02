package changes

import config.Configuration
import gitlog.{GitCommit, FileChange, GitChanges}
import layer.StructureExtractor
import layer.module.TypeStructure

/**
 * Created by chanjinpark on 15. 3. 18..
 */

class ChangeDelta(proj: String, jarf: String, cid: String) {
  // (g, s)
  //만약, class path가 달라졌다면, cp1을 새로 정의할 필요
  val (commits, cdetails, ch, chb) = GitChanges.changesAfter (proj, cid, C2F.cp, C2F.extension)
  val tdg = StructureExtractor.getTypeGraph(proj, jarf)

  val c2f = new C2F(tdg, chb)

  val (added, deleted, renamed) = GeneralizationAnalyzer.analyzer_origin(commits(cid), ch, chb)
  val (addedc, deletedc, renamedc) = (C2F.getClassesFromFiles(added), C2F.getClassesFromFiles(deleted), C2F.getClassesFromFiles(renamed.map(_._1)))
  val modifiedc = c2f.classes.filter(c => !addedc.contains(c) && !deletedc.contains(c)).toList



  // 전체 중 co change 파일을 가지고 있는 파일 수는? co-change 파일을 가진 파일은 평균적으로 몇 개 co-change를 가지고 있고, co-change 횟수는 얼마인가?
  val mc2flater = ch.map(f => C2F.classNameofFile(f._1, C2F.cp) -> f._2).toMap

  /*
  val gensChanges = c2f.gens.map( gs => {
    val (g, subs) = gs  // (gen, list(gen, sub))
    val gch = (c2f.mc2f(g).times, c2f.mc2f(g).amount)
    val lsch = subs.map(s => (s, c2f.mc2f(s).times, c2f.mc2f(s).amount))
    (g, gch, lsch) //
  })
*/

  def summaryDelta = {
    def summary(t: List[String], m: Map[String, FileChange], m1: Map[String, FileChange] ) = {
      val s = t.map(f => (m(f).times, m(f).amount)).foldLeft((0, 0, 0))((s, ta) => (s._1 + ta._1, s._2 + ta._2, s._3 + 1))
      // times, amount, mean times, mean amount, files
      if ( m1 == null )
        (s._1, s._2, s._1.toDouble/s._3, s._2.toDouble/s._3, s._3)
      else {
        val s1 = t.map(f => (m1(f).times, m1(f).amount)).foldLeft((0, 0, 0))((s, ta) => (s._1 + ta._1, s._2 + ta._2, s._3 + 1))
        val delta = (s1._1 - s._1, s1._2 - s._2, s1._3)
        (delta._1, delta._2, delta._1.toDouble/delta._3, delta._2.toDouble/delta._3, delta._3)
      }
    }

    println("전체 파일수, 클래스 수 (at t) - " + c2f.mc2f.size + ", " + c2f.classes.count(!_.contains("$")))
    println("t 이후 추가 파일 수 - " + added.size + ", t 이 후 삭제 파일 수 - " + ( chb.size - (ch.size - added.size) ) + ", t 시점 - " + commits(cid).date)
    println("t 이후 재명명된 파일 수 - " + renamed.size + "\n\t" + renamed.map(r => r._1.name + " -> " + r._2.name).mkString("\n\t"))

    println("t 이전 클래스 변경량 - " + summary(c2f.classes.filter(!_.contains("$")).toList, c2f.mc2f, null))
    println("t 이전 일반 클래스 변경량 - " + summary(c2f.gens.keys.filter(!_.contains("$")).toList, c2f.mc2f, null))
    println("t 이전 구현 클래스 변경량 - " + summary(c2f.impl.filter(!_.contains("$")).toList, c2f.mc2f, null))

    val classes1 = c2f.classes.filter(c => !deletedc.contains(c))
    val gens1: Map[String, List[String]] = c2f.gens.filter(g => !deletedc.contains(g._1))
    val impl1 = c2f.gens.flatMap(g => g._2.filter(c => !deletedc.contains(c)))
    println("t 이후 클래스 변경량 " + summary(classes1.filter(!_.contains("$")).toList, c2f.mc2f, mc2flater))
    println("t 이후 인터페이스 변경량 " + summary(gens1.keys.filter(!_.contains("$")).toList, c2f.mc2f, mc2flater))
    println("t 이후 구현클래스 변경량 " + summary(impl1.filter(!_.contains("$")).toList, c2f.mc2f, mc2flater) )
  }

  def summaryCochange = {
    val (coall, cobefore, coafter) = FileChange.cochangeAfter(commits(cid).order, ch, chb) // (f -> map(f, int))
    println("Co change all - " + FileChange.summaryCochange(coall))
    println("Co change before - " + FileChange.summaryCochange(cobefore))
    println("Co change after - " + FileChange.summaryCochange(coafter))
  }

  def summaryAmount = {
    val (amountall, amountbefore, amountafter) = FileChange.changeAmountAfter(commits(cid).order, ch, chb)
    println("Amount all - " + FileChange.summaryAmount(amountall))
    println("Amount before - " + FileChange.summaryAmount(amountbefore))
    println("Amount after - " + FileChange.summaryAmount(amountafter))
  }

  summaryDelta
  summaryCochange
  summaryAmount
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

  def analyzer_origin(c: GitCommit, ch: Map[String, FileChange], chb: Map[String, FileChange]): (List[FileChange], List[FileChange], List[(FileChange, FileChange)]) = {
    def getOriginAt(c: GitCommit, fc: FileChange): (Boolean, FileChange, FileChange) = {
      var ren = fc.renamedfrom //
      if (ren._1 == null || ren._1.isBefore(c)) (false, null, null)
      else {
        var found = false
        while (ren._1 != null && !found) {
          // c 이 후에 rename 된 것들 중에 가장 오래된 것을 리턴
          val ren1 = ren._2.renamedfrom
          if (ren1._1 != null && ren1._1.isBefore(c)) {
            found = true
            println("FOUND " + ren._1.commitid + " " + ren._1.date + " " + ren._2.name + "->" + fc.name)
            if ( ren1._1 != null)
              println("FOUND " + ren1._1.commitid + " " + ren1._1.date + " " + ren1._2.name+ "->" + ren._2.name)
          }
          else
            ren = ren._2.renamedfrom
        }
        (found, ren._2, fc)
      }
    }

    val newlyfound = ch.filter(f => !chb.contains(f._1))
    // mapped는 cid 이 후로 이름이 바뀐 파일, renamed - mapped는 새로 생긴 파일
    val renamed = newlyfound.map(f => getOriginAt(c, ch(f._1))).filter(_._1).map(e => (e._2, e._3)).toList
    // 커밋 c 이 후로 새로 생긴 파일 들, 이름이 바뀐 파일 들
    // chb + add + delete = ch (renamed는 뺌)
    val addedfiles = newlyfound.values.filter(f => !renamed.map(_._2).contains(f)).toList
    val deletedfiles = chb.values.filter(f => !ch.contains(f.name)).toList
    (addedfiles, deletedfiles, renamed)
  }

  def main(args: Array[String]): Unit = {
    // junit 4.11 c2e4d911fadfbd64444fb285342a8f1b72336169,  @marcphilipp marcphilipp released this on 15 Nov 2012 · 747 commits to master since this release
    val p = "junit"
    val f = Configuration.jarfile(p, "junit-4.11.jar")
    val cid = "c2e4d911fadfbd64444fb285342a8f1b72336169"

    val delta = new ChangeDelta(p, f, cid)
  }
}
