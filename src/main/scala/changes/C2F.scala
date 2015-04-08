package changes

import gitlog.FileChange
import layer.StructureExtractor
import layer.module.TypeStructure

/**
 * Created by chanjinpark on 15. 3. 27..
 */

class C2F(tdg: TypeStructure, chb: Map[String, FileChange]) {
  val classes = tdg.nodes.map(_._1).filter(!_.contains("$"))
  val gens = // gen -> descendants (g -> list(s))
    StructureExtractor.extractGeneralization(tdg)
  val impl = gens.flatMap(_._2).toList.distinct
  val mc2f = chb.map(f => C2F.classNameofFile(f._1, C2F.cp) -> f._2).toMap
}

object C2F {
  val cp = List("src/main/java/") //, "src/test/java/")
  val extension = List("java", "scala")

  def classNameofFile(f: String, classpath: List[String]): String = {
    val path = classpath.filter(p => f.startsWith(p))
    if (path.length == 1 ) {
      f.substring(path.head.length, f.lastIndexOf(".")).replace("/", ".")
    }
    else {
      assert(false, "not matched " + f)
      ""
    }
  }

  def getClassesFromFiles(files: List[FileChange]) = {
    files.map(fc => C2F.classNameofFile(fc.name, C2F.cp))
  }
}
