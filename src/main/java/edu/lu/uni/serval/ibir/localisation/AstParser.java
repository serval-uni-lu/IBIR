package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @See edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser
 */
public class AstParser {

    private File javaFile;
    private static Logger log = LoggerFactory.getLogger(AstParser.class);
    // todo in the future may be add setters for these fields.
    private boolean allowFieldDeclaration = false;
    private boolean allowMethodDeclaration = false;
    private boolean allowTypeDeclaration = false;

    public List<Pair<ITree, String>> parseSuspiciousCode(File javaFile) {
        this.javaFile = javaFile;
        ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, ASTGenerator.TokenType.EXP_JDT);
        return identifySuspiciousCodeAst(rootTree);
    }

    private List<Pair<ITree, String>> identifySuspiciousCodeAst(ITree tree) {
        List<Pair<ITree, String>> suspiciousCode = new ArrayList<>();
        if (!Checker.isBlock(tree.getType())) {
            if (tree != null && isRequiredAstNode(tree)) {
                Pair<ITree, String> pair = new Pair<>(tree, readSuspiciousCode(tree));
                 if (!suspiciousCode.contains(pair)) { // todo check before reading the code because the previous line is expensive. but in this case it should be fine.
                    suspiciousCode.add(pair);
                }
            }
            if (tree.getChildren() != null && !tree.getChildren().isEmpty()) {
                for (ITree child : tree.getChildren()) {
                    suspiciousCode.addAll(identifySuspiciousCodeAst(child));
                }
            }
        } else {
            for (ITree child : tree.getChildren()) {
                suspiciousCode.addAll(identifySuspiciousCodeAst(child));
            }
        }
//        List<ITree> children = tree.getChildren();
//
//        for (ITree child : children) {
//            int startPosition = child.getPos();
//            int startLine = unit.getLineNumber(startPosition);
//
//            if (Checker.isBlock(child.getType())) {
//                suspiciousCode.addAll(identifySuspiciousCodeAst(child, unit));
//            } else {
//                if (!isRequiredAstNode(child)) {
//                    child = traverseParentNode(child);
//                    if (child == null) continue;
//                }
//                Pair<ITree, String> pair = new Pair<ITree, String>(child, readSuspiciousCode(child));
//                if (!suspiciousCode.contains(pair)) { // todo check before reading the code because the previous line is expensive.
//                    suspiciousCode.add(pair);
//                }
//            }
//        }
        return suspiciousCode;
    }

    private boolean isRequiredAstNode(ITree tree) {
        int astNodeType = tree.getType();
        if (Checker.isStatement(astNodeType)
                || allowFieldDeclaration && Checker.isFieldDeclaration(astNodeType)
                || allowMethodDeclaration && Checker.isMethodDeclaration(astNodeType)
                || allowTypeDeclaration && Checker.isTypeDeclaration(astNodeType)) {
            return true;
        }
        return false;
    }

    private String readSuspiciousCode(ITree suspiciousCodeAstNode) {
        String javaFileContent = FileHelper.readFile(this.javaFile);
        int startPos = suspiciousCodeAstNode.getPos();
        int endPos = startPos + suspiciousCodeAstNode.getLength();
        return javaFileContent.substring(startPos, endPos);
    }

}
