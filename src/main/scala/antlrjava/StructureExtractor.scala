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

class StructureExtractor extends JavaBaseVisitor[StructureParseResult]{


  override def visitCompilationUnit(@NotNull ctx: JavaParser.CompilationUnitContext): StructureParseResult = {
    val result = visitChildren(ctx)
    result.childresults.reverse.foreach(r => println(r))
    println(result.packagename)
    println()
    return result
  }


  //visitPackageDeclaration
  override def visitPackageDeclaration(@NotNull ctx: JavaParser.PackageDeclarationContext): StructureParseResult = {
    val result = visitChildren(ctx)
    result.packagename = ctx.qualifiedName.Identifier().toArray.mkString(".")
    println(result.packagename)
    return result
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
