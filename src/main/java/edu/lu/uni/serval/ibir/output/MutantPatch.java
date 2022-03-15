package edu.lu.uni.serval.ibir.output;

import edu.lu.uni.serval.ibir.experiments.OchiaiUtils;
import edu.lu.uni.serval.ibir.localisation.IbirSuspiciousPosition;
import edu.lu.uni.serval.ibir.localisation.PrioSuspeciousPosition;
import edu.lu.uni.serval.tbar.info.Patch;

import java.util.Objects;
import java.util.Set;

public class MutantPatch {


    private final String projectName;
    private final Patch patch;
    private final String patchCode;
    private final String patternUsedToInjectBug;
    private final int brokenTestsCount;
    private final Set<String> brokenTests;
    private final int patchId;
    private final IbirSuspiciousPosition prioSuspeciousPosition;


    public MutantPatch(String projectName, Set<String> brokenTests, int brokenTestsCount, Patch patch, String patchCode, String patternUsedToInjectBug, int patchId, IbirSuspiciousPosition prioSuspeciousPosition) {
        this.projectName = projectName;
        this.brokenTests = brokenTests;
        this.patchId = patchId;
        this.patch = patch;
        this.patchCode = patchCode;
        this.patternUsedToInjectBug = patternUsedToInjectBug;
        this.brokenTestsCount = brokenTestsCount;
        this.prioSuspeciousPosition = prioSuspeciousPosition;
    }


    public Set<String> getBrokenTests() {
        return brokenTests;
    }

    public String getProjectName() {
        return projectName;
    }

    public int getBrokenTestsCount() {
        return brokenTestsCount;
    }

    public String getPatternUsedToInjectBug() {
        return patternUsedToInjectBug;
    }

    public String getPatchCode() {
        return patchCode;
    }

    public Patch getPatch() {
        return patch;
    }

    public int getPatchId() {
        return patchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutantPatch that = (MutantPatch) o;
        return Objects.equals(projectName, that.projectName) &&
                Objects.equals(patch, that.patch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, patch);
    }

    public IbirSuspiciousPosition getPrioSuspeciousPosition() {
        return prioSuspeciousPosition;
    }

    public double getOchiaiCoef(Set<String> testsBrokenByOriginalBug) {
        return OchiaiUtils.calculateOchiai(testsBrokenByOriginalBug, brokenTests);
    }

    public boolean hasSameBrokenTestsAsOriginalBug(Set<String> testsBrokenByOriginalBug) {
        return testsBrokenByOriginalBug.equals(brokenTests);
    }

    public boolean breaksOnlySubsetOfBrokenTestsByOriginalBug(Set<String> testsBrokenByOriginalBug) {
        return hasSameBrokenTestsAsOriginalBug(testsBrokenByOriginalBug) || brokenTests != null && !brokenTests.isEmpty() && testsBrokenByOriginalBug.containsAll(brokenTests);
    }

    public int getLineStart(){
        return this.getPrioSuspeciousPosition().getLineStart();
    }

    public int getLineEnd(){
        return this.getPrioSuspeciousPosition().getLineEnd();
    }

    /**
     * it will add the following values in the same line  : prjName, localisationLine, confidence, patchId, brokenTestsCount, pattern.
     *
     * @param testsBrokenByOriginalBug the tests broken by the ground truth.
     * @param elapsedTime
     * @return a csv line.
     * @see CsvConsts#DEFAULT_MUTATION_MATRIX_CSV_HEADERS
     */
    public String toCsv(Set<String> testsBrokenByOriginalBug, long elapsedTime) {
        return this.getProjectName() + "," +
                this.getPrioSuspeciousPosition().getLocalisationResultLine() + "," +
                this.getPrioSuspeciousPosition().getLocalisationResultLineConfidence() + "," +
                this.getPatchId() + "," +
                this.getBrokenTestsCount() + "," +
                this.getPatternUsedToInjectBug() + "," +
                this.getOchiaiCoef(testsBrokenByOriginalBug) + "," +
                this.hasSameBrokenTestsAsOriginalBug(testsBrokenByOriginalBug) + "," +
                this.breaksOnlySubsetOfBrokenTestsByOriginalBug(testsBrokenByOriginalBug) + "," +
                String.join(" ", this.getBrokenTests()) + "," +
                Serializer.to(patch) + "," +
                elapsedTime+ "," +
                this.getLineStart() + "," +
                this.getLineEnd()+ "," +
                this.getPrioSuspeciousPosition().classPath ;
    }
}
