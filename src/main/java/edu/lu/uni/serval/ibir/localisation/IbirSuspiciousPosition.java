package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

import java.util.Arrays;
import java.util.Objects;

public class IbirSuspiciousPosition extends SuspiciousPosition {
    public final static int[] NOT_PROVIDED_POSITION = {-1, -1};
    public final int[] lineNumbers;
    public final int[] strPos;

    public int getLineStart() {
        return lineNumbers[0];
    }

    public int getLineEnd() {
        return lineNumbers[1];
    }

    public IbirSuspiciousPosition(String classpath, int lineNumber) {
        this.classPath = classpath;
        this.lineNumber = lineNumber;
        this.lineNumbers = new int[]{lineNumber, lineNumber};
        this.strPos = NOT_PROVIDED_POSITION;
    }


    public int getLocalisationResultLine(){
        return -1;
    }

    public String getLocalisationResultLineConfidence(){
        return null;
    }


    public IbirSuspiciousPosition(int[] lineNumbers, String classPath, int[] positionInSourceCodeFile) {
        this.lineNumber = lineNumbers[0];
        this.classPath = classPath;
        this.lineNumbers = lineNumbers;
        this.strPos = positionInSourceCodeFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IbirSuspiciousPosition)) return false;
        IbirSuspiciousPosition that = (IbirSuspiciousPosition) o;
        return classPath.equals(that.classPath) && Arrays.equals(lineNumbers, that.lineNumbers) && Arrays.equals(strPos, that.strPos);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(lineNumbers);
        result = 31 * result + Arrays.hashCode(strPos);
        return Objects.hash(classPath, result);
    }


}
