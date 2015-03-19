package changes

import gitlog.{GitCommit, FileChange, GitChanges}
import layer.StructureExtractor
import layer.configuration.Configuration

/**
 * Created by chanjinpark on 15. 3. 18..
 */

class GenChanges(g: String) {
  // (g, s)
}

class GeneralizationAnalyzer(commitmap: Map[String, GitCommit]) {

  def analyzer_origin(c: GitCommit, ch: Map[String, FileChange], chb: Map[String, FileChange]): (List[FileChange], List[FileChange], List[(FileChange, FileChange)]) = {
    val newlyfound = ch.filter(f => !chb.contains(f._1))
    println("파일 수 - 현재: "+ ch.size + ", 이전: " + chb.size + ", 차이 - " + (ch.size - chb.size))
    println("현재 파일이 해당 커밋의 파일로 Mapping 안되는 파일 수 - " + newlyfound.size)
    //println(renamed.size + "\n" + renamed.map(r => commitmap(r._2.birth).order + ", " + r._2.birth + ", " + r._1 ).mkString("\n"))

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
        //if (!found) assert(!commitmap(fc.birth).isBefore(c) || commitmap(fc.birth) != c, "나중에 생긴 파일이어야 함 " + commitmap(fc.birth).summary)
        (found, ren._2, fc)
      }
    }

    // mapped는 cid 이 후로 이름이 바뀐 파일, renamed - mapped는 새로 생긴 파일
    val renamed = newlyfound.map(f => getOriginAt(c, ch(f._1))).filter(_._1).map(e => (e._2, e._3)).toList
    // 커밋 c 이 후로 새로 생긴 파일 들, 이름이 바뀐 파일 들
    // chb + add + delete = ch (renamed는 뺌)
    val addedfiles = newlyfound.values.filter(f => !renamed.map(_._2).contains(f)).toList
    val deletedfiles = chb.values.filter(f => !ch.contains(f.name)).toList
    (addedfiles, deletedfiles, renamed)
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

  def timesAndamount(ch: Map[String, FileChange]) = {
    val ta = ch.foldLeft((0, 0, 0))((s, op) => (s._1 + op._2.times, s._2 + op._2.amount, s._3 + 1))
    // times, amount, mean times, mean amount, files
    (ta._1, ta._2, ta._1.toDouble/ta._3, ta._2.toDouble/ta._3, ta._3)
  }

  def main(args: Array[String]) = {
    // junit 4.11 c2e4d911fadfbd64444fb285342a8f1b72336169,  @marcphilipp marcphilipp released this on 15 Nov 2012 · 747 commits to master since this release
    val p = "junit"
    val f = Configuration.jarfile(p, "junit-4.11.jar")
    val cid = "c2e4d911fadfbd64444fb285342a8f1b72336169"

    val cp = List("src/main/java/") //, "src/test/java/")
    val extension = List("java", "scala")
    val (c, cd, ch, chbefore) = GitChanges.changesAfter (p, cid, cp, extension)

    // --------- class
    val tdg = StructureExtractor.getTypeGraph(p, f)
    val gens = StructureExtractor.extractGeneralization(tdg).map(gs => (gs._1, gs._2.filter(!_._2.contains("$")))).filter(gs => gs._2.length > 0).toMap
    val classes = tdg.nodes.map(_._1).filter(!_.contains("$"))

    def classNameofFile(f: String): String = {
      val path = cp.filter(p => f.startsWith(p))
      if (path.length == 1 ) {
        f.substring(path.head.length, f.lastIndexOf(".")).replace("/", ".")
      }
      else {
        assert(false, "not matched " + f)
        ""
      }
    }

    val mc2f = chbefore.map(f => classNameofFile(f._1) -> f).toMap

    println(mc2f.size + " " + classes.size)
    // nested classes = tdg.nodes.filter(n => n._1.contains("$"))
    // val gens_s = gens.keys.
    val fgens = gens.map( gs => {
      val (g, subs) = gs  // (gen, list(gen, sub))
      val gch = (mc2f(g)._2.times, mc2f(g)._2.amount)
      val lsch = subs.map(s => (s._2, mc2f(s._2)._2.times, mc2f(s._2)._2.amount))
      (g, gch, lsch)
    })

    //println(fgens.map(r => r._1 + " " + r._2 + "\n\t" + r._3.mkString("\n\t")).mkString("\n"))

    def summary(t: List[String], m: Map[String, (String, FileChange)]) = {
      val s = t.map(f => (m(f)._2.times, m(f)._2.amount)).foldLeft((0, 0, 0))((s, ta) => (s._1 + ta._1, s._2 + ta._2, s._3 + 1))
      // times, amount, mean times, mean amount, files
      (s._1, s._2, s._1.toDouble/s._3, s._2.toDouble/s._3, s._3)
    }

    println("클래스 변경량 - " + summary(classes.toList, mc2f))
    println("일반 클래스 변경량 - " + summary(gens.keys.toList, mc2f))
    println(gens.keys.mkString(", "))


    // gen 클래스의 나중 변경
    val analyzer = new GeneralizationAnalyzer(c)
    val (added, deleted, renamed) = analyzer.analyzer_origin(c(cid), ch, chbefore)

    println(added.size + " files are added. " + ( chbefore.size - (ch.size - added.size) ) + " files are deleted, since " + c(cid).date)
    println(renamed.map(r => r._1.name + " -> " + r._2.name).mkString("\n"))

    val mc2flater = ch.map(f => (classNameofFile(f._1), f)).toMap

    println("변경량 이전 (Java) - " + timesAndamount(chbefore))
    println("변경량 전체 (Java) - " + timesAndamount(ch))

    // 기준 - gens, classes
    // evolved 매핑되거나 리네임된 것은 변경의 차를 구해야 함
    // added 추가 된 것은 전체 변경량 관점에서 더해야 함
    // 이 후 전체 변경량 = evolved + added
    // 클래스 이름, 횟수, 변경량으로 맵 테이블 만들면 됨. 그리고, gen 클래스 확인.

    val (addedc, deletedc, renamedc) = (added.map(fc => classNameofFile(fc.name)), deleted.map(fc => classNameofFile(fc.name)), renamed.map(fc => classNameofFile(fc._1.name)))
    val modifiedc = classes.filter(c => !addedc.contains(c) && !deletedc.contains(c)).toList
    println("now = clsses + added - deleted (renamed): " + mc2flater.size + " = " + classes.size + " + " + addedc.size + " - " + deletedc.size + " ( " + renamedc.size + " )")
    // deleted contains renamed

    println("추가된 변경량 - " + summary(addedc, mc2flater))
    println("재명명 변경량 - " + summary(renamed.map(r => classNameofFile(r._2.name)), mc2flater))
    println("재명명 변경량 이전 - " + summary(renamed.map(r => classNameofFile(r._1.name)), mc2f))
    println("기존  변경량 - " + summary(modifiedc, mc2flater))
    println("기존  변경량 이전 - " + summary(modifiedc, mc2f))

    //val evolved =


  }
}
