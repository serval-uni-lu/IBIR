package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.ibir.info.IBIrPatch;
import edu.lu.uni.serval.jdt.tree.ITree;

public final class PatchFactory {

    public static IBIrPatch createReplacePatch(ITree predicateExp, String expression){
        IBIrPatch patch = new IBIrPatch();
        patch.setFixedCodeStr1(expression);
        patch.setBuggyCodeEndPos(predicateExp.getEndPos());
        patch.setBuggyCodeStartPos(predicateExp.getPos());
        patch.setNeedBugyCode(false);
        return patch;
    }
}
