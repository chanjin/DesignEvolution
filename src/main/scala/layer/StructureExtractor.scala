package layer

import layer.configuration.{ProjectBasicInfo, Project}
import Project.StructureKind._
import config.Configuration
import layer.constructor.{GraphFile, GraphConstructor}
import layer.module._

/**
 * Created by chanjinpark on 15. 3. 19..
 */

object StructureExtractor {

  def extractGeneralization(tdg: TypeStructure):  Map[String, List[String]] = {
    val gens = tdg.edges.filter(e => e.isSubtypeEdge).map(e => (e.getTarget.value, e.getSource.value)).groupBy(_._1)
    gens.map(g => (g._1 -> g._2.map(_._2)))
    //println(gens.map(g => g._1 + "\n\t" + g._2.mkString("\n\t")).mkString("\n"))
    //gens
  }

  def getTypeGraph(p: String, jar: String) = {
    val project: ProjectBasicInfo = new ProjectBasicInfo(p, jar, Configuration.srcdir(p), "", false)
    Project.proj = project

    layer.util.FileOut.removeFiles(project.getOutputPath, "dot")

    Graph2Dot.prefix = project.prefix
    var dgs: (TypeStructure, PackageStructure) = null
    if (project.requireReanalyze) {
      println("generate layer class information")
      dgs = GraphConstructor.constructTypeStructure(project.rootpath)
    } else {
      dgs = GraphFile.load(project.getOutputPath)
    }

    val tdg = dgs._1
    val pdg = dgs._2

    tdg
  }

  def main(args: Array[String]) = {
    val p = "junit"
    val f = Configuration.jarfile(p, "junit-4.11.jar")
    val tg = getTypeGraph(p, f)
    extractGeneralization(tg)
  }
}