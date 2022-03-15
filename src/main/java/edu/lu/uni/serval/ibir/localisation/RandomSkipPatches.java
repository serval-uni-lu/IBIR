package edu.lu.uni.serval.ibir.localisation;

import edu.lu.uni.serval.tbar.fixpattern.FixTemplate;
import edu.lu.uni.serval.tbar.info.Patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomSkipPatches {
    /**
     * Once we achieve this <Code>STOP_SKIPPING * numberOfMutants</Code> of tried patches we stop skipping patches.
     */
    public static final Integer STOP_SKIPPING = Integer.getInteger("STOP_SKIPPING",3);
    private final List<SkippedPatch> skippedPatches;
    private final int stopSkippingLimit;
    private final Random sourceOfRandomness;

    public RandomSkipPatches(int numberOfMutants, Random sourceOfRandomness) {
        this.skippedPatches = new ArrayList<>();
        this.stopSkippingLimit = numberOfMutants * STOP_SKIPPING;
        this.sourceOfRandomness = sourceOfRandomness;
    }

    public boolean stopSkipping(int triedPatches){
        return triedPatches > stopSkippingLimit;
    }

    public void addSkippedPatches(List<Patch> patchCandidates, SuspCodeNode scn, FixTemplate ft, IbirSuspiciousPosition prioSuspeciousPosition){
        for (Patch patchCandidate : patchCandidates) {
            skippedPatches.add(new SkippedPatch(patchCandidate,scn,ft,prioSuspeciousPosition));
        }
    }

    public List<SkippedPatch> pollShuffledSkippedPatches(){
        List<SkippedPatch> result = new ArrayList<>(skippedPatches);
        Collections.shuffle(result,sourceOfRandomness);
        skippedPatches.clear();
        return result;
    }


    public static class SkippedPatch {

        public final Patch patch;
        public final SuspCodeNode scn;
        public final FixTemplate ft;
        public final IbirSuspiciousPosition prioSuspeciousPosition;

        public SkippedPatch(Patch patch, SuspCodeNode scn, FixTemplate ft, IbirSuspiciousPosition prioSuspeciousPosition) {
            this.patch = patch;
            this.scn = scn;
            this.ft = ft;
            this.prioSuspeciousPosition = prioSuspeciousPosition;
        }
    }
}
