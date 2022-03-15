package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private LRUMap<File, String> lruCache;

    public AstParser() {
        lruCache = new LRUMap<>(5);
    }

    public List<Pair<ITree, AstNode>> parseSuspiciousCode(File javaFile) {
        this.javaFile = javaFile;
        ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, ASTGenerator.TokenType.EXP_JDT);
        return identifySuspiciousCodeAst(rootTree);
    }

    private List<Pair<ITree, AstNode>> identifySuspiciousCodeAst(ITree tree) {
        List<Pair<ITree, AstNode>> suspiciousCode = new ArrayList<>();
        if (!Checker.isBlock(tree.getType())) {
            if (tree != null && isRequiredAstNode(tree)) {
                Pair<ITree, AstNode> pair = new Pair<>(tree, new AstNode(tree, javaFile, readFile()));
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

    private String readFile() {
        String res = lruCache.get(this.javaFile);
        if (res == null) {
            res = FileHelper.readFile(this.javaFile);
            lruCache.put(this.javaFile, res);
        }
        return res;
    }

    public static class AstNode {

        public final File file;
        public final int startPos;
        public final int endPos;
        public final String nodeStr;
        public final ITree suspiciousCodeAstNode;
        public final int startLine;
        public final int endLine;

        private AstNode(ITree suspiciousCodeAstNode, File file, String javaFileContent) {
            this.file = file;
            this.suspiciousCodeAstNode = suspiciousCodeAstNode;
            this.startPos = suspiciousCodeAstNode.getPos();
            this.endPos = startPos + suspiciousCodeAstNode.getLength();
            this.nodeStr = javaFileContent.substring(startPos, endPos);
            String endLineSubs = javaFileContent.substring(0, endPos);
            this.endLine = getLineNumber(endLineSubs);
            this.startLine = getLineNumber(endLineSubs.substring(0, startPos));
        }

        private int getLineNumber(String string) {
            String lineBreak = "\n";
            return ((string.length() - string.replace(lineBreak, "").length()) / lineBreak.length()) + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AstNode astNode = (AstNode) o;
            return startPos == astNode.startPos && endPos == astNode.endPos && Objects.equals(file, astNode.file) ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, startPos, endPos);
        }
    }

}
