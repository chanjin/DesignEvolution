/**
 * DependencyGraph.scala
 *
 * Project: DesignRecovery
 * Copyright (c) 2011 Chanjin Park
 * License - GNU LESSER GENERAL PUBLIC LICENSE v3.0 (LGPL v3.0)
 *
 */

package layer.module

import layer.util._

class NodeElem(v: String) {
  val value = v
  var outedges: List[DepEdge] = Nil
  var inedges: List[DepEdge] = Nil

  def neighbors: List[NodeElem] = outedges.map(_.getTarget)

  def outdegree: Int = outedges.length

  def indegree: Int = outedges.length

  override def toString = value

  def toDot = value
}

class DepEdge(src: NodeElem, dst: NodeElem) {
  val edgeSep = ">"

  def toTuple = (src.value, dst.value)

  override def toString = src.value + edgeSep + dst.value

  def isSubtypeEdge: Boolean = false

  def getTarget = dst

  def getSource = src

  def toDot: String = ""
}


class DependencyGraph {
  var nodes: Map[String, NodeElem] = Map()
  var edges: List[DepEdge] = Nil

  def addNode(n: NodeElem): NodeElem = {
    nodes = Map(n.value -> n) ++ nodes
    n
  }

  protected def addEdge(e: DepEdge): DepEdge = {
    edges = e :: edges
    e.getSource.outedges = e :: e.getSource.outedges
    e.getTarget.inedges = e :: e.getTarget.inedges
    e
  }

  protected def removeEdge(e: DepEdge) = {
    edges = edges.filter(!_.equals(e))
    e.getSource.outedges = e.getSource.outedges.filter(!_.equals(e))
    e.getSource.inedges = e.getSource.inedges.filter(!_.equals(e))
  }

  def removeEdges(es: List[DepEdge]) = {
    es.foreach(removeEdge(_))
  }

  def removeNodes(elems: List[NodeElem]) = {
    var es = List[DepEdge]()
    for (n <- elems) {
      es = n.inedges ::: n.outedges ::: es
    }

    nodes --= elems.map(_.value)
    for (e <- es) removeEdge(e)
  }

  def getNode(value: String): NodeElem =
    if (nodes.contains(value)) nodes(value) else null

  def getNodes(vs: List[String]): List[NodeElem] = vs.map(v => nodes(v))

  def getEdge(from: String, to: String): DepEdge = {
    //print("*** getEdge " + from + "->" + to + " : ")
    if (!nodes.contains(from) || !nodes.contains(to)) return null
    val edges = nodes(from).outedges.filter(_.getTarget == nodes(to))
    var edge: DepEdge = null
    if (edges.length > 0) {
      assert(edges.length == 1)
      edge = edges.head
    }
    //println(edge)
    edge
  }

  def getEdges(nodes: List[NodeElem]): List[DepEdge] = {
    edges.filter(e => nodes.contains(e.getSource) && nodes.contains(e.getTarget))
  }

  //------------------------
  def detectSCC(): List[List[NodeElem]] = {
    val sccdetect = new SCCDetector(nodes)
    sccdetect.run
  } // end of detectSCC

  def filterEdges(ns: List[NodeElem]): List[DepEdge] = {
    edges.filter(edge => ns.contains(edge.getSource) && ns.contains(edge.getTarget))
  }

  // ----------------------

  def listcontains(l: List[_], l2: List[_]) =
    l2.foldLeft(true)((r, e) => r && l.contains(e))

  override def equals(o: Any) = o match {
    case g: DependencyGraph =>
      (listcontains(nodes.keys.toList, g.nodes.keys.toList) &&
        listcontains(edges.map(_.toTuple), g.edges.map(_.toTuple)))
    case _ => false
  }

  override def toString = {
    val (edgeStrs, unlinkedNodes) =
      edges.foldLeft((Nil: List[String], nodes.values.toList))((r, e) => (e.toString :: r._1, r._2.filter((n) => n != e.getSource && n != e.getTarget)))
    "[" + (unlinkedNodes.map(_.value.toString) ::: edgeStrs).mkString(", \n") + "]"
  }

  // direct cycles between two nodes (no transitive cycles)
  def directCycles: List[(NodeElem, List[NodeElem])] = {
    def directCycles(from: NodeElem): List[NodeElem] = {
      val tos = from.outedges.map(_.getTarget)
      tos.foldLeft(List[NodeElem]())((ns, to) => if (to.outedges.map(_.getTarget).contains(from)) to :: ns else ns)
    }
    nodes.values.map(n => (n, directCycles(n))).toList.filter(cycle => cycle._2.length > 0)
  }
}

