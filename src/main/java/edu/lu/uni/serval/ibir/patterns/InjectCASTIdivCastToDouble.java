package edu.lu.uni.serval.ibir.patterns;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixpattern.FixTemplate;
import edu.lu.uni.serval.tbar.fixpatterns.ICASTIdivCastToDouble;
import edu.lu.uni.serval.tbar.utils.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix patterns for ICAST_IDIV_CAST_TO_DOUBLE violations.
 * <p>
 * Context: InfixExpression and the operator is "/".
 *
 * @author ahmed.khanfir
 * @see ICASTIdivCastToDouble
 */
public class InjectCASTIdivCastToDouble extends FixTemplate implements NamedPatterns {

    private static Logger log = LoggerFactory.getLogger(InjectCASTIdivCastToDouble.class);

    private static final String PATTERN_NAME_PREFIX = "NbreDivType_";

    enum InjectionPatterns {
        /**
         * Fix pattern 1:
         * - intVarExp / 10;
         * + intVarExp / 10d(or f);
         * Fix pattern 5:
         * - intVarExp / 2;
         * + 0.5 * intVarExp;
         */
        FP1o5,
        /**
         * Fix pattern 2:
         * -   1 / var
         * +   1.0 / var
         */
        FP2,
        /**
         * Fix pattern 3:
         * -    dividend / divisor;
         * +    dividend / (float or double) divisor;
         * <p>
         * Fix pattern 4:
         * -   dividend / divisor;
         * +   (double or float) dividend / divisor;
         */
        FP3o4,
        /**
         * intVarExp / 10d(or f); --> intVarExp / 10;
         */
        IP1,
        /**
         * 1.0 / var --> 1 / var
         */
        IP2,
        /**
         * dividend / (double or float) divisor; --> dividend / divisor;
         */
        IP3,
        /**
         * (double or float) dividend / divisor; --> dividend / divisor;
         */
        IP4,
        /**
         * 0.5 * intVarExp; --> intVarExp / 2;
         */
        IP5
    }

    private InjectionPatterns fixPatternType;

    @Override
    public String getName() {
        return PATTERN_NAME_PREFIX + fixPatternType.name();
    }


    /*
     * Fix pattern 1:
     * - intVarExp / 10;
     * + intVarExp / 10d(or f);
     *
     * Fix pattern 2:
     * -   1 / var
     * +   1.0 / var
     *
     * Fix pattern 3:
     * -    dividend / divisor;
     * +    dividend / (float or double) divisor;
     *
     * Fix pattern 4:
     * -   dividend / divisor;
     * +   (double or float) dividend / divisor;
     *
     * Fix pattern 5:
     * - intVarExp / 2;
     * + 0.5 * intVarExp;
     *
     * additional inject patterns:
     * p1:
     * i1 : intVarExp / 10d(or f); --> intVarExp / 10;
     * i2 : 1.0 / var --> 1 / var
     * i3 : dividend / (double or float) divisor; --> dividend / divisor;
     * i4 : (double or float) dividend / divisor; --> dividend / divisor;
     * i5 : 0.5 * intVarExp; --> intVarExp / 2;
     * i6 :
     * i7 :
     *
     */

    @Override
    public void generatePatches() {
        ITree suspCodeTree = this.getSuspiciousCodeTree();
        List<Pair<ITree, InjectionPatterns>> buggyExps = identifyBuggyExpressions(suspCodeTree);

        for (Pair<ITree, InjectionPatterns> buggyExp : buggyExps) {
            ITree buggyExpTree = buggyExp.getFirst();
            int startPos = buggyExpTree.getPos();
            int endPos = startPos + buggyExpTree.getLength();
            String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
            String codePart2 = this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
            fixPatternType = buggyExp.getSecond();

            switch (fixPatternType) {
                case FP1o5:
                    //FP1
//                    String code = this.getSubSuspiciouCodeStr(startPos, endPos);
//                    this.generatePatch(codePart1 + code + "d" + codePart2);
//                    this.generatePatch(codePart1 + code + "f" + codePart2);
//
//                    //FP5
//                    endPos = buggyExpTree.getChild(0).getPos() + buggyExpTree.getChild(0).getLength();
//                    String numberLiteral = buggyExpTree.getChild(2).getLabel();
//                    code = "(1.0 / " + numberLiteral + ") * " + this.getSubSuspiciouCodeStr(startPos, endPos);
//                    this.generatePatch(codePart1 + code + codePart2);
//                    break;
                case FP2://FP2
//                    numberLiteral = buggyExpTree.getChild(0).getLabel();
//                    startPos = buggyExpTree.getChild(0).getPos() + buggyExpTree.getChild(0).getLength();
//                    code = this.getSubSuspiciouCodeStr(startPos, endPos);
//                    code = numberLiteral + ".0" + code;
//                    this.generatePatch(codePart1 + code + codePart2);
//                    break;
                case FP3o4:
                    //FP3
//                    ITree rightHandExp = buggyExpTree.getChild(2);
//                    int startPos1 = rightHandExp.getPos();
//                    int endPos1 = startPos1 + rightHandExp.getLength();
//                    String rightHandExpCode = this.getSubSuspiciouCodeStr(startPos1, endPos1);
//                    code = this.getSubSuspiciouCodeStr(startPos, startPos1);
//                    this.generatePatch(codePart1 + code + "(double)" + rightHandExpCode + codePart2);
//                    this.generatePatch(codePart1 + code + "(float)" + rightHandExpCode + codePart2);
//
//                    //FP4
//                    code = this.getSubSuspiciouCodeStr(startPos, endPos);
//                    this.generatePatch(codePart1 + "(double)" + code + codePart2);
//                    this.generatePatch(codePart1 + "(float)" + code + codePart2);
                    break;
                case IP1:
                    i1removeDivisorDorF(buggyExpTree, codePart1, codePart2, startPos);
                    break;
                case IP2:
                    i2removeComaZero(buggyExpTree, codePart1, codePart2, endPos);
                    break;
                case IP3:
                    i3removeDivisorCast(buggyExpTree, codePart1, codePart2, startPos);
                    break;
                case IP4:
                    i4removeDividendCast(buggyExpTree, codePart1, codePart2, endPos);
                    break;
                case IP5:
                    i5changeMultiplicationToDivision(buggyExpTree, codePart1, codePart2);
                    break;
                default:
                    break;
            }
        }
    }

    boolean isInt(Double aDouble) {
        return !Double.isInfinite(aDouble) && !aDouble.isNaN() && (aDouble == Math.floor(aDouble));
    }

    // 0.5 * intVarExp; --> intVarExp / 2; <==> intVarExp / (1/0.5); <==> right / (1/left)
    void i5changeMultiplicationToDivision(ITree buggyExpTree, String codePart1, String codePart2) {
        ITree leftHandExp = buggyExpTree.getChild(0);
        ITree rightHandExp = buggyExpTree.getChild(2);
        String newDivisorStr;
        try {
            Double divisor = Double.valueOf(1d / Double.valueOf(leftHandExp.getLabel()));
            newDivisorStr = String.valueOf(divisor.intValue());
            String code = rightHandExp.getLabel() + "/" + newDivisorStr;
            this.generatePatch(codePart1 + code + codePart2);
        } catch (Throwable throwable) {
            log.error("something went wrong : leftHandExp = " + leftHandExp.getLabel(), throwable);
        }
    }

    // (double or float) dividend / divisor; --> dividend / divisor;
    void i4removeDividendCast(ITree buggyExpTree, String codePart1, String codePart2, int endPos) {
        ITree leftHandExp = buggyExpTree.getChild(0);
        int startPos = leftHandExp.getPos() + leftHandExp.getLength();
        String code = this.getSubSuspiciouCodeStr(startPos, endPos);
        int textToRemoveLength = leftHandExp.getLabel().startsWith("(double)") ? "(double)".length() : "(float)".length();
        code = leftHandExp.getLabel().substring(textToRemoveLength) + code;
        this.generatePatch(codePart1 + code + codePart2);
    }

    void i1removeDivisorDorF(ITree buggyExpTree, String codePart1, String codePart2, int startPos) {
        ITree rightHandExp = buggyExpTree.getChild(2);
        int startPos1 = rightHandExp.getPos();
        int endPos1 = startPos1 + rightHandExp.getLength();
        String rightHandExpCode = this.getSubSuspiciouCodeStr(startPos1, endPos1 - 1);
        String code = this.getSubSuspiciouCodeStr(startPos, startPos1);
        this.generatePatch(codePart1 + code + rightHandExpCode + codePart2);
    }

    // 1.0 / var --> 1 / var
    void i2removeComaZero(ITree buggyExpTree, String codePart1, String codePart2, int endPos) {
        ITree leftHandExp = buggyExpTree.getChild(0);
        int startPos = leftHandExp.getPos() + leftHandExp.getLength();
        String code = this.getSubSuspiciouCodeStr(startPos, endPos);
        code = leftHandExp.getLabel().substring(0, leftHandExp.getLabel().length() - ".0".length()) + code;
        this.generatePatch(codePart1 + code + codePart2);
    }

    // dividend / (double or float) divisor; --> dividend / divisor;
    void i3removeDivisorCast(ITree buggyExpTree, String codePart1, String codePart2, int startPos) {
        ITree rightHandExp = buggyExpTree.getChild(2);
        int startPos1 = rightHandExp.getPos();
        int endPos1 = startPos1 + rightHandExp.getLength();
        int textToRemoveLength = rightHandExp.getLabel().startsWith("(double)") ? "(double)".length() : "(float)".length();
        String rightHandExpCode = this.getSubSuspiciouCodeStr(startPos1 + textToRemoveLength, endPos1);
        String code = this.getSubSuspiciouCodeStr(startPos, startPos1);
        this.generatePatch(codePart1 + code + rightHandExpCode + codePart2);
    }

    List<Pair<ITree, InjectionPatterns>> identifyBuggyExpressions(ITree suspCodeTree) {
        List<Pair<ITree, InjectionPatterns>> buggyExps = new ArrayList<>();
        List<ITree> children = suspCodeTree.getChildren();
        for (ITree child : children) {
            int childType = child.getType();
            if (Checker.isComplexExpression(childType)) {
                if (Checker.isInfixExpression(childType)) {
                    String operator = child.getChild(1).getLabel();
                    if ("/".equals(operator)) { //   x / y
                        if (Checker.isNumberLiteral(child.getChild(0).getType())) { // number / *
                            String leftHandExpLabel = child.getChild(0).getLabel();
                            if (!leftHandExpLabel.endsWith("f") && !leftHandExpLabel.endsWith("d") && !leftHandExpLabel.endsWith("l")) { // [int or real] / *
                                if (!child.getChild(2).getLabel().startsWith("(double)") && !child.getChild(2).getLabel().startsWith("(float)")) { // [int or real] / [no cast double or float] *
                                    try {
                                        if (!leftHandExpLabel.endsWith(".0") && isInt(Double.valueOf(leftHandExpLabel))) { // int / [no cast double or float] *
                                            buggyExps.add(new Pair<>(child, InjectionPatterns.FP2));
                                        }
                                    } catch (Throwable throwable) {
                                        log.error("something went wrong : leftHandExp = " + leftHandExpLabel, throwable);
                                    }
                                } else {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP3));
                                }
                                if (leftHandExpLabel.endsWith(".0")) {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP2));
                                }
                            }
                        } else if (Checker.isNumberLiteral(child.getChild(2).getType())) { // x / number
                            String rightHandExpLabel = child.getChild(2).getLabel();
                            if (!child.getLabel().startsWith("(double)") && !child.getLabel().startsWith("(float)")) {
                                if (!rightHandExpLabel.endsWith("f") && !rightHandExpLabel.endsWith("d") && !rightHandExpLabel.endsWith("l")) {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.FP1o5));
                                } else {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP1));
                                }
                            } else if (!rightHandExpLabel.startsWith("(double)") && !rightHandExpLabel.startsWith("(float)")) {
                                buggyExps.add(new Pair<>(child, InjectionPatterns.IP4));
                            }
                        } else {
                            String leftHandExpLabel = child.getChild(0).getLabel();
                            String rightHandExpLabel = child.getChild(2).getLabel();
                            if (!child.getLabel().startsWith("(double)") && !child.getLabel().startsWith("(float)") &&
                                    !rightHandExpLabel.startsWith("(double)") && !rightHandExpLabel.startsWith("(float)")) {
                                buggyExps.add(new Pair<>(child, InjectionPatterns.FP3o4));
                                if (leftHandExpLabel.endsWith(".0")) {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP2));
                                }
                            } else {
                                if (leftHandExpLabel.startsWith("(double)") || leftHandExpLabel.startsWith("(float)")) {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP4));
                                }
                                if (rightHandExpLabel.startsWith("(double)") || rightHandExpLabel.startsWith("(float)")) {
                                    buggyExps.add(new Pair<>(child, InjectionPatterns.IP3));
                                }
                            }
                        }
                    } else if ("*".equals(operator)) { // x * y
                        if (Checker.isNumberLiteral(child.getChild(0).getType())) { // number * y
                            if (child.getChild(0).getLabel().matches("\\d+\\.\\d+")) {
                                buggyExps.add(new Pair<>(child, InjectionPatterns.IP5));
                            }
                        }
                    }
                }
                buggyExps.addAll(identifyBuggyExpressions(child));
            } else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
                buggyExps.addAll(identifyBuggyExpressions(child));
            } else if (Checker.isStatement(childType)) {
                break;
            }
        }
        return buggyExps;
    }

}
