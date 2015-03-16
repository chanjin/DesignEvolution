package gitlog

import java.io.File
import java.nio.charset._

import config.Configuration
import scala.io.{Codec, Source}

/**
 * Created by chanjinpark on 15. 3. 15..
 */

/*
old mode <mode>
new mode <mode>
deleted file mode <mode>
new file mode <mode>
copy from <path>
copy to <path>
rename from <path>
rename to <path>
similarity index <number>
dissimilarity index <number>
index <hash>..<hash> <mode>

diff --git a/src/org/junit/internal/runners/ClassRoadie.java b/src/org/junit/internal/runners/Roadie.java
similarity index 56%
rename from src/org/junit/internal/runners/ClassRoadie.java
rename to src/org/junit/internal/runners/Roadie.java
---
+++


old mode 100644
new mode 100755

deleted file mode 100644

 */

object ChangeType extends Enumeration {
  val Modified, Deleted, Added, Renamed = Value
}

class GitDiff(strs: List[String]) {
  private def getfromto(s: String): (String, String) = {
    val ab = s.substring(11).split(" ").map(_.substring(2).trim)
    if ( ab.length == 1 ) (ab.head, ab.head)
    else if ( ab.length == 2 ) (ab.head, ab.tail.head)
    else {
      assert(true, "wrong diff files " + s)
      ("", "")
    }
  }

  import ChangeType._

  val (from, to) = getfromto(strs.head)
  var similarity: Int = -1
  var (nadd, ndel) = (0, 0)
  var changetype = Modified

  def summary = changetype + "(" + nadd + "+," + ndel + "-): " + from + " -> " + to

  strs.tail.foreach(s => {
    val w = s.split(" ").map(_.trim).toList
    if (w.size > 0) {
      w.head match {
        case "new" => {
          changetype = Added
        }
        case "rename" => {
          changetype = Renamed
          if (w(1).equals("from")) assert(w(2).equals(from), w(2) + ", expected: " + from)
          else if (w(1).equals("to")) assert(w(2).equals(to), w(2) + ", expected: " + to)
        }
        case "deleted" => {
          changetype = Deleted
        }
        case "similarity" => {
          similarity = w(2).substring(0, w(2).length - 1).toInt
        }
        case "index" => {}
        case "dissimilarity" => {}
        case "copy" => {}
        case "old" => {}
        case "+++" => {
          //assert(w(1).substring(2).equals(from) || w(1).startsWith("/dev"), w(1) + ", expected: " + from)
        }
        case "---" => {
          //assert(w(1).substring(2).equals(to) || w(1).startsWith("/dev"), w(1) + ", expected: " + to)
        }
        case _ => {
          if (s.length > 0 ) {
            s.charAt(0) match {
              case '+' => {
                nadd += 1
              }
              case '-' => {
                ndel += 1
              }
              case ' ' => {}
              case _ => {
                //TODO: Error 처리
                assert(true, "this line is not yet considered: " + s)
              }
            }
          }
        }
      }
    }
  })


  // +, -, space
  // TODO: 나중에 --stat으로 체크해볼 것 017ce048adbc360fed2e8d7ae55dfbb3dbdc76dc
}

/*
commit 0a9a389570a171b67c3155d312f2e90bdab5aaaf
Author: Noel Yap <noel.yap+github.com@gmail.com>
Date:   2013-03-01 14:22:47 -0800

    Changes due to suggestions from @kcooney.

diff --git a/src/main/java/org/junit/experimental/categories/Categories.java b/src/main/java/org/junit/experimental/categories/Categories.java
index 5c78483..3f0cdc7 100644
--- a/src/main/java/org/junit/experimental/categories/Categories.java
+++ b/src/main/java/org/junit/experimental/categories/Categories.java
@@ -3,6 +3,7 @@ package org.junit.experimental.categories;
 import java.lang.annotation.Retention;
...
 */

object GitCommitDetails {
  def get(f: String) : (String, List[GitDiff]) = {
    val codec = Codec("UTF-8").onMalformedInput(CodingErrorAction.IGNORE)
    val lines = Source.fromFile(f)(codec).getLines()

    var l = lines.next()
    var cid = ""
    do {
      // only process commit id
      if (l.startsWith("commit ")) {
        cid = l.substring(7).trim
      }
      l = lines.next()
    } while (!l.startsWith("diff --git ") && lines.hasNext)

    var diffs = List[GitDiff]()
    var diff = List[String]()
    var first = true
    while (lines.hasNext) {
      if (l.startsWith("diff --git")) {
        if (first) first = false
        else {
          diffs ::= (new GitDiff(diff.reverse))
          diff = List[String]()
        }
      }
      diff ::= l
      l = lines.next()
    }
    if (!first) diffs ::= (new GitDiff(diff.reverse))

    (cid, diffs.reverse)
  }

  def getall(p: String): Map[String, List[GitDiff]] = {
    val dir = new File(Configuration.commitdir(p))
    val files = dir.listFiles.map(_.getAbsolutePath) //.take(10)
    files.map(get(_)).toMap
  }

  def main(args: Array[String]) = {
    val details = getall("junit")
    println(details.map(kv => kv._1 + ": " + kv._2.mkString("\n")))
  }
}