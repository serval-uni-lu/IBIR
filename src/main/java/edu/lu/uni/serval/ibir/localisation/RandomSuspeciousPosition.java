package edu.lu.uni.serval.ibir.localisation;

public class RandomSuspeciousPosition extends PrioSuspeciousPosition {

// todo
    public RandomSuspeciousPosition(String javaFilePath, int localisationResultLine) {
        // removing the slash in the begin
        super(javaFilePath.substring(1),-1, "0.0",localisationResultLine);
    }
}
