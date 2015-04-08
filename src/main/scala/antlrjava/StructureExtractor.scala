package antlrjava

/**
 * Created by chanjinpark on 15. 4. 2..
 */
import antlrjava.recognizer.{JavaLexer, JavaParser, JavaBaseVisitor}
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.NotNull



case class DefMethod(retType: String, signature: String) { }

case class DefType(n: String, isIntf: Boolean) {
  def name = n
  def isInterface = isIntf
  var pkgname = ""
  var methods = List[DefMethod]()
}

class StructureExtractor extends JavaBaseVisitor[List[Any]]{
  var types = List[DefType]()

  override def visitCompilationUnit(@NotNull ctx: JavaParser.CompilationUnitContext): List[Any] = {
    val result = visitChildren(ctx)
    println("visitCompilationUnit " + result.mkString("\n"))
    return result
  }

  var packagename = ""
  override def visitPackageDeclaration(@NotNull ctx: JavaParser.PackageDeclarationContext): List[Any] = {
    val result = visitChildren(ctx)
    println("visitPackageDeclaration " + result.mkString(" : "))
    return result
  }

  override def visitImportDeclaration(@NotNull ctx: JavaParser.ImportDeclarationContext): List[Any] = {
    val result = visitChildren(ctx)
    retult
  }

  var curType: DefType = null
  override def visitTypeDeclaration(@NotNull ctx: JavaParser.TypeDeclarationContext): List[Any] = {
    if ( curType == null) curType = new DefType("a", true)
    val result = visitChildren(ctx)
    types ::= curType
    curType = null

    println("visitTypeDeclaration " + result.mkString(" : "))
    return result
  }

  override def visitClassDeclaration(@NotNull ctx: JavaParser.ClassDeclarationContext): List[Any] = {
    // visitTypeParameters
    // parameter 가지는 지만 체크
    val result = visitChildren(ctx)
    println("visitClassDeclaration " + result.mkString(" : "))
    return result
  }

  override def visitTypeParameters(@NotNull ctx: JavaParser.TypeParametersContext): List[Any] = {
    val result = visitChildren(ctx)
    println("visitTypeParameters " + result.mkString(" : "))
    return result
  }

  protected override def defaultResult() = {
    List[Any]()
  }

  protected override def aggregateResult(aggregate: List[Any], nextResult: List[Any]) = {
    nextResult ++ aggregate
  }
}

object StructureExtractor {
  def parseFile(f: String) {
    try {
      val lexer: Lexer = new JavaLexer(new ANTLRFileStream(f))
      val tokens: CommonTokenStream = new CommonTokenStream(lexer)
      val parser: JavaParser = new JavaParser(tokens)
      val t: ParserRuleContext = parser.compilationUnit
      println(parser.structure.get("pkgdecl") + " : " + parser.structure.size())
      println(parser.importList.toArray().mkString(", "))

      t.accept(new StructureExtractor)

      println("--------")
      System.out.println(t.toStringTree(parser))
    }
    catch {
      case e: Exception => {
        System.err.println("parser exception: " + e)
        e.printStackTrace
      }
    }
  }

  def main(args: Array[String]) = {
    parseFile("/Users/chanjinpark/GitHub/DesignEvolution/src/main/java/antlrjava/recognizer/Test.java")
  }
}
