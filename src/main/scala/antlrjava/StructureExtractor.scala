package antlrjava

/**
 * Created by chanjinpark on 15. 4. 2..
 */
import antlrjava.recognizer.{JavaParser, JavaBaseVisitor}
import org.antlr.v4.runtime.misc.NotNull

class StructureParseResult {
  var packagename = ""
}

class StructureExtractor extends JavaBaseVisitor[StructureParseResult]{


  override def visitCompilationUnit(@NotNull ctx: JavaParser.CompilationUnitContext): StructureParseResult = {
    val result = visitChildren(ctx)

    println(result.packagename)
    return result
  }


  //visitPackageDeclaration
  override def visitPackageDeclaration(@NotNull ctx: JavaParser.PackageDeclarationContext): StructureParseResult = {
    val result = visitChildren(ctx)
    result.packagename = ctx.qualifiedName.Identifier().toArray.mkString(", ")
    return result
  }
}

object StructureExtractor {
  def main(args: Array[String]) = {

  }
}
