package antlrjava

/**
 * Created by chanjinpark on 15. 4. 2..
 */
import antlrjava.recognizer.{JavaLexer, JavaParser, JavaBaseVisitor}
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.NotNull

class StructureParseResult {
  var packagename = ""
  var childresults = List[StructureParseResult]()
}



class DefType {
  var name = ""
}

class StructureExtractor extends JavaBaseVisitor[StructureParseResult]{

  override def visitCompilationUnit(@NotNull ctx: JavaParser.CompilationUnitContext): StructureParseResult = {
    val result = visitChildren(ctx)
    result.childresults.reverse.foreach(r => println(r))
    println(result.packagename)
    println()
    return result
  }


  var packagename = ""
  override def visitPackageDeclaration(@NotNull ctx: JavaParser.PackageDeclarationContext): StructureParseResult = {
    packagename = ctx.qualifiedName.Identifier().toArray.mkString("."))
    return visitChildren(ctx)
  }


  var types = List[DefType]()
  var curType: DefType = null
  override def visitTypeDeclaration(@NotNull ctx: JavaParser.TypeDeclarationContext): StructureParseResult = {
    if ( curType == null) curType = new DefType
    val result = visitChildren(ctx)
    types ::= curType
    curType = null
    return result
  }

  override def visitClassDeclaration(@NotNull ctx: JavaParser.ClassDeclarationContext): StructureParseResult = {
    // visitTypeParameters
    // parameter 가지는 지만 체크

    return visitChildren(ctx)
  }

  override def visitTypeParameters(@NotNull ctx: JavaParser.TypeParametersContext): StructureParseResult = {
    return visitChildren(ctx)
  }

  protected override def defaultResult() = {
    new StructureParseResult
  }

  protected override def aggregateResult(aggregate: StructureParseResult, nextResult: StructureParseResult) = {
    aggregate.childresults ::= nextResult
    aggregate
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
