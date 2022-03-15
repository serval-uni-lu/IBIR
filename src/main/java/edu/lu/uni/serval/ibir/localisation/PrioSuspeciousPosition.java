package edu.lu.uni.serval.ibir.localisation;

import java.util.Objects;

public class PrioSuspeciousPosition extends IbirSuspiciousPosition {

    public final String localisationResultLineConfidence;
    public final int localisationResultLine;

    public PrioSuspeciousPosition(String classpath, int lineNumber, String localisationResultLineConfidence, int localisationResultLine) {
        super(classpath, lineNumber);
        this.localisationResultLineConfidence = localisationResultLineConfidence;
        this.localisationResultLine = localisationResultLine;
    }


    public int getLocalisationResultLine(){
        return localisationResultLine ;
    }

    public String getLocalisationResultLineConfidence(){
        return localisationResultLineConfidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrioSuspeciousPosition)) return false;
        if (!super.equals(o)) return false;
        PrioSuspeciousPosition that = (PrioSuspeciousPosition) o;
        return localisationResultLine == that.localisationResultLine && Objects.equals(localisationResultLineConfidence, that.localisationResultLineConfidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localisationResultLineConfidence, localisationResultLine);
    }
}
