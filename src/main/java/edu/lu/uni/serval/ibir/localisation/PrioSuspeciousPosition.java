package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

import java.util.Objects;

public class PrioSuspeciousPosition extends SuspiciousPosition {

    public final String localisationResultLineConfidence;
    public final int localisationResultLine;

    public PrioSuspeciousPosition(String classpath, int lineNumber, String localisationResultLineConfidence, int localisationResultLine) {
        this.classPath = classpath;
        this.lineNumber = lineNumber;
        this.localisationResultLineConfidence = localisationResultLineConfidence;
        this.localisationResultLine = localisationResultLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrioSuspeciousPosition that = (PrioSuspeciousPosition) o;
        return lineNumber == that.lineNumber
                && classPath.equals(that.classPath)
        && localisationResultLine == that.localisationResultLine
                && localisationResultLineConfidence.equals(that.localisationResultLineConfidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classPath, lineNumber, localisationResultLineConfidence, localisationResultLine);
    }
}
