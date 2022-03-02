package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.jdt.generator.ExpJdtTreeGenerator;
import edu.lu.uni.serval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.IOException;

public final class StringAstParser {

    private StringAstParser(){
        throw new IllegalArgumentException("utility class, static access only.");
    }


    public static ITree expressionToAst(String str) throws IOException {
        return new ExpJdtTreeGenerator().generateFromCodeFragment(str, ASTParser.K_EXPRESSION).getRoot();
    }

    public static ITree statementsToAst(String str) throws IOException {
        return new ExpJdtTreeGenerator().generateFromCodeFragment(str, ASTParser.K_STATEMENTS).getRoot();
    }

}
