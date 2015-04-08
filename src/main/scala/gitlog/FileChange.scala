package gitlog

/**
 * Created by chanjinpark on 15. 3. 27..
 */

class FileChange(f: String, cid: String) {
  // times, amount, period
  // co-changed (a, 4) 파일 a와 4번 변경됨
  val name = f
  var birth = cid //commit id
  var death = ""

  var renamedfrom: (GitCommit, FileChange) = (null, null)
  var times = 0
  var amount = 0

  var history = List[(Int, Int)]()
  var cochanges = List[(Int, List[FileChange])]()  // Commit Order -> changed files

  def amountAfter(order: Int) = {
    history.foldLeft((0, 0))((s, h) => if (h._1 > order) (s._1 + h._2, s._2 + 1) else s)
    // amount, times
  }

  def cochangeAfter(order: Int) = {
    cochanges.foldLeft(Map[String, Int]())((m, coc) => {
      val (ord, files) = coc
      if ( ord > order ) {
        files.foldLeft(m)((m1, fc) => {
          val f1 = fc.name
          if (m.contains(f1)) {
            val cnt = m(f1)
            (m - f1) + (f1 -> (cnt + 1))
          }
          else
            m + (f1 -> 1)
        })
      }
      else m
    }).toMap  // File -> Frequency
  }

  //var changes = List[(String, Int, List[String])]() // commit id, change amount, co-changes
  def summary = name + " (" + times + ", " + amount + "): \t" + birth + "~" + death + ", cochanged#" + cochanges.size //.values.mkString("\n\t")
}

object FileChange {
  def change(fc: FileChange, order: Int, amount: Int) = {
    fc.times += 1
    fc.amount += amount
    fc.history ::= (order, amount)
  }

  def changeAmountAfter(order: Int, ch: Map[String, FileChange], chb: Map[String, FileChange]) = {
    val after = ch.map(f => f._1 -> f._2.amountAfter(order))
    val before = chb.map(f => f._1 -> f._2.amountAfter(-1))
    val all = ch.map(f => f._1 -> f._2.amountAfter(-1))
    (all, before, after)
  }

  def summaryAmount(amounts: Map[String, (Int, Int)]) = {
    val sum = amounts.foldLeft((0, 0, 0))((s, f) => {
      (s._1 + f._2._1, s._2 + f._2._2, s._2 + 1)
    })
    (sum._3, sum._1, sum._2, sum._1.toDouble/sum._3, sum._2.toDouble/sum._3)
    //files#, amount, times
  }

  // 같이 변경된 파일들의 정보. 누구와 몇번?. 이전과 전체.
  def cochangeAfter(order: Int, ch: Map[String, FileChange], chb: Map[String, FileChange]) = {
    val after = ch.map(f => f._1 -> f._2.cochangeAfter(order))
    val before = chb.map(f => f._1 -> f._2.cochangeAfter(-1))
    val all = ch.map(f => f._1 -> f._2.cochangeAfter(-1))
    (all, before, after)
  }

  def summaryCochange(co: Map[String, Map[String, Int]]) = {
    val cosum = co.foldLeft((0, 0, 0))((s, f) => (s._1 + f._2.size, s._2 + f._2.foldLeft(0)((s, f1) => s + f1._2), s._3 + 1))

    (cosum._3, cosum._1, cosum._2, cosum._1/cosum._3, cosum._2/cosum._3)
    //files#, cochanged files#, cochanged freq.
  }
}