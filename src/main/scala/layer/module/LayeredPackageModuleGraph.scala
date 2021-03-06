/**
 * LayeredPackageModuleGraph.scala
 *
 * Project: DesignRecovery
 * Copyright (c) 2011 Chanjin Park
 * License - GNU LESSER GENERAL PUBLIC LICENSE v3.0 (LGPL v3.0)
 *
 */
package layer.module

class LayeredPackageModuleGraph(pkg: String, lps: LayerModuleStructure, pdg: PackageStructure) {
  //mapT2PKG: NodeElem => PackageNode) {
  def dotname = pkg.replace(".", "_") + "_NST_tdg"

  private def modulesInPackage(pkgname: String, lps: LayerModuleStructure): List[LayerModuleNode] = {
    lps.nodes.values.filter(m => (m.asInstanceOf[LayerModuleNode].pkgname == pkgname)).toList.
      map(_.asInstanceOf[LayerModuleNode]).sortWith((m1, m2) => m1.asInstanceOf[LayerModuleNode].rank > m2.asInstanceOf[LayerModuleNode].rank)
  }

  class PackageNode(v: String, tns: List[NodeElem]) extends NodeElem(v) {
    def pkgname = v

    def getTypeNodes = tns

    override def toDot = {
      var dotstr = tns.map(n => n.value.substring(n.value.lastIndexOf(".") + 1)).mkString(",")
      /*	if (dotstr.length > 20) dotstr.substring(0, 10) + " ... " + tns.size
        else dotstr*/
      pkgname + "\\n" + (
        if (dotstr.length > 20) dotstr.substring(0, 20) + " ... " + tns.size
        else dotstr)
    }
  }

  def construct(dotfilename: String) = {
    val pkg2lms = lps.nodes.values.foldLeft(scala.collection.mutable.Map[String, List[LayerModuleNode]]())((m, lm) => {
      val pkgname = lm.asInstanceOf[LayerModuleNode].pkgname
      if (!m.contains(pkgname)) m += (pkgname -> List(lm.asInstanceOf[LayerModuleNode]))
      else m(pkgname) = lm.asInstanceOf[LayerModuleNode] :: m(pkgname)
      m
    })

    val lmnodes = modulesInPackage(pkg, lps)
    val nodes = modulesInPackage(pkg, lps).flatMap(_.types)
    val edges = lps.tdg.edges.filter(edge => nodes.contains(edge.getSource) && nodes.contains(edge.getTarget))

    //def packagename(tname: String): String = tname.substring(0, tname.lastIndexOf("."))
    val mP2N = scala.collection.mutable.Map[String, List[NodeElem]]()
    nodes.foreach(n => {
      val srcs = n.inedges.map(_.getSource).filter(n => !nodes.contains(n))
      srcs.foreach(sn => {
        val spkg = pdg.mapT2M(sn.asInstanceOf[TypeNode]).value //mapT2PKG(sn) //packagename(sn.value)
        if (!mP2N.contains(spkg)) mP2N += (spkg -> List(sn))
        else mP2N(spkg) = sn :: mP2N(spkg)
      })
    })

    val pnodes: List[PackageNode] =
      mP2N.map(p2n => new PackageNode(p2n._1, p2n._2)).toList ::: lmnodes.map(lm => new PackageNode(lm.value, lm.types))

    val mP2PN = pnodes.foldLeft(Map[String, PackageNode]())((m, pn) => m + (pn.pkgname -> pn))
    val pedges: Map[String, List[DepEdge]] = pnodes.foldLeft(Map[String, List[DepEdge]]())((m, pn) => {
      var toedges = List[DepEdge]()
      m + (pn.pkgname -> pn.getTypeNodes.flatMap(_.outedges.filter(e => nodes.contains(e.getTarget))))
    })

    val dotname =
      if (dotfilename.indexOf("/") != -1) dotfilename.substring(dotfilename.lastIndexOf("/") + 1) else dotfilename
    var (node2id, count) =
      (nodes ::: pnodes).foldLeft((scala.collection.mutable.Map[NodeElem, Int](), 0))((m, node) => ((m._1 + (node -> m._2)), m._2 + 1))

    def nodestrlist(ns: List[NodeElem]): List[String] = {
      def label(n: NodeElem) = if (n.isInstanceOf[PackageNode]) n.toDot else n.value.substring(n.value.lastIndexOf(".") + 1)
      ns.map(stn => node2id(stn) + "[ label=\"" + label(stn) + "\"]")
    }
    def moduleString(m2ts: (String, List[NodeElem]), count: Int) = {
      "subgraph cluster" + count +
        "	{  \n" + "node [shape=plaintext, fontname=Skia, fontsize=10];" +
        "\nlabel=\"" + m2ts._1 + "\";\n" +
        nodestrlist(m2ts._2).mkString(";\n") + ";\n" +
        "}\n"
    }


    var mLM2Ts: scala.collection.mutable.Map[String, List[NodeElem]] = modulesInPackage(pkg, lps).
      foldLeft(scala.collection.mutable.Map[String, List[NodeElem]]())((m, lm) => m + (lm.value -> lm.types))
    pnodes.foreach(pn => {
      /*	if (! lmnodes.map(_.value).contains(pn.pkgname) ) mLM2Ts += (pn.pkgname -> List(pn))
        else{
          mLM2Ts(pn.pkgname) = pn :: mLM2Ts(pn.pkgname)
        }*/
      if (lmnodes.map(_.value).contains(pn.pkgname)) mLM2Ts(pn.pkgname) = pn :: mLM2Ts(pn.pkgname)
    })

    var nodeStrs: (List[String], Int) = {
      mLM2Ts.foldLeft((Nil: List[String], 0))((liststr, m2ts) => (moduleString(m2ts, liststr._2) :: liststr._1, liststr._2 + 1))
    }

    val edgeStrs = {
      def edgestr(e: DepEdge, node2id: NodeElem => Int): String =
        (node2id(e.getSource) + "->" + node2id(e.getTarget)) +
          (if (e.isSubtypeEdge) Graph2Dot.edgeSubtype else Graph2Dot.edgeSimple) + "]"

      var strs = edges.foldLeft(Nil: List[String])((r, e) => edgestr(e, node2id) :: r)

      def pedgestr(p: String, es: List[DepEdge], node2id: NodeElem => Int): List[String] = {
        val es2add = es.map(e => ((node2id(mP2PN(p))), (node2id(mP2PN(lps.mapT2M(e.getTarget.asInstanceOf[TypeNode]).value))), e.isSubtypeEdge))
        //val tos = es2add.foldLeft(Map[Int, ]

        var edgesmap = scala.collection.mutable.Map[(Int, Int), Boolean]()
        es.map(e => {
          val from = node2id(mP2PN(p))
          val to = node2id(mP2PN(lps.mapT2M(e.getTarget.asInstanceOf[TypeNode]).value)) //node2id(e.getTarget))
          val key = (from, to)
          if (!edgesmap.contains(key)) edgesmap += (key -> e.isSubtypeEdge)
          else {
            if (!edgesmap(key)) edgesmap(key) = e.isSubtypeEdge
          }
        })

        edgesmap.map(entry => {
          (entry._1._1 + "->" + entry._1._2) +
            (if (entry._2) Graph2Dot.edgeSubtype else Graph2Dot.edgeSimple) + "]"
        }
        ).toList
      }

      strs = pedges.foldLeft(List[String]())((r, e) => pedgestr(e._1, e._2, node2id) ::: r) ::: strs
      strs
    }

    layer.util.FileOut.saveFile(dotfilename + ".dot",
      "digraph " + dotname + " { \n"
        + "fontname=Skia; \nfontsize=9; \n"
        + Graph2Dot.edgestyle
        + nodeStrs._1.mkString(" ")
        + "node [shape=plaintext, fontname=Skia, fontsize=10];"
        + nodestrlist(pnodes).mkString(";\n") + ";\n"
        + edgeStrs.mkString(";\n") + "\n}")
  }

}


