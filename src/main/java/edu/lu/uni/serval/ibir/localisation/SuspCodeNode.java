package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.jdt.tree.ITree;

import java.io.File;
import java.util.Objects;

public class SuspCodeNode {

    public File javaBackup;
    public File classBackup;
    public File targetJavaFile;
    public File targetClassFile;
    public int startPos;
    public int endPos;
    public ITree suspCodeAstNode;
    public String suspCodeStr;
    public String suspiciousJavaFile;
    public int[] buggyLines;

    public SuspCodeNode(File javaBackup, File classBackup, File targetJavaFile, File targetClassFile, int startPos,
                        int endPos, ITree suspCodeAstNode, String suspCodeStr, String suspiciousJavaFile, int[] buggyLines) {
        this.javaBackup = javaBackup;
        this.classBackup = classBackup;
        this.targetJavaFile = targetJavaFile;
        this.targetClassFile = targetClassFile;
        this.startPos = startPos;
        this.endPos = endPos;
        this.suspCodeAstNode = suspCodeAstNode;
        this.suspCodeStr = suspCodeStr;
        this.suspiciousJavaFile = suspiciousJavaFile;
        this.buggyLines = buggyLines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuspCodeNode that = (SuspCodeNode) o;
        return startPos ==  that.startPos && endPos == that.endPos && Objects.equals(suspiciousJavaFile, that.suspiciousJavaFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPos, endPos, suspiciousJavaFile);
    }
}
