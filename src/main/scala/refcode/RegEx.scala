package refcode

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}

/**
 * Created by chanjinpark on 15. 3. 14..
 */
object RegEx {

  def main1(args: Array[String]): Unit = {
    val re = "^[ACDMRTUX]\t".r
    val test = "M\tsrc/main/java/org/junit/runners/BlockJUnit4ClassRunner.java\nA\tsrc/test/java/org/junit/runners/CustomBlockJUnit4ClassRunnerTest.java\nM\tsrc/test/java/org/junit/tests/AllTests.java"
    val ls = test.lines.toList
    ls.foreach( l => {
      l match {
        case re => {
          println(l.charAt(0))
        }
        case _ => {
          println(l)
        }
      }
    })
    val log = "commit a90b496a6595856066504baf4f737fb853a6e45d\nAuthor: Sam Brannen <sam@sambrannen.com>\nDate:   Sun Feb 15 21:53:52 2015 +0100\n\n    Ensure exceptions from methodBlock() don't result in unrooted tests.\n\n    The introduction of the runLeaf() method in BlockJUnit4ClassRunner in\n    JUnit 4.9 introduced a regression with regard to exception handling.\n\n    Specifically, the invocation of methodBlock() is no longer executed\n    within a try-catch block as was the case in previous versions of JUnit.\n\n    Custom modifications to methodBlock() or the methods it invokes may in\n    fact throw exceptions. In such cases, exceptions thrown from\n    methodBlock() cause the current test execution to abort immediately. As\n    a result, the failing test method is unrooted in test reports, and\n    subsequent test methods are never invoked. Furthermore, RunListeners\n    registered with JUnit are not notified.\n\n    This commit addresses this issue by wrapping the invocation of\n    methodBlock() within a try-catch block. If an exception is not thrown,\n    the resulting Statement is passed to runLeaf(). If an exception is\n    thrown, it is wrapped in a Fail statement which is passed to runLeaf().\n\n    Closes #1066\n    Closes #1082\n\nM\tsrc/main/java/org/junit/runners/BlockJUnit4ClassRunner.java\nA\tsrc/test/java/org/junit/runners/CustomBlockJUnit4ClassRunnerTest.java\nM\tsrc/test/java/org/junit/tests/AllTests.java"
    val lines = log.lines.toList
    val s = lines span (_.length > 0)
    println(s._1)
    println(s._2)

    val status = "ACDMRTUX"
    val s1 = s._2 span (l =>  l.length > 2 && status.contains(l.charAt(0)) && l.charAt(1) == '\t')
    println(s1._1)
    println(s1._2)

    lines.foreach(l => {
      if (l.length > 2 && status.contains(l.charAt(0)) && l.charAt(1) == '\t') println("*****" + l)
    })
    println("done")
  }

  def main(args: Array[String]): Unit = {
    val f = DateTimeFormat.forPattern("yyyy-MM-dd HH.mm.ss")
    println(DateTime.parse("2004-06-09 10.20.30", f))

    val d1 = "2015-03-01 20:36:02 +0200"
    println(DateTime.parse(d1, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z")))

    val d = "Sun Mar 1 20:36:02 2015 +0200"
    val df = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss yyyy Z")
    val date = DateTime.parse(d, df) //ISODateTimeFormat.dateTime)
    print(date)
  }

}
