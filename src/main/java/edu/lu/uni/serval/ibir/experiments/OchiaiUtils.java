package edu.lu.uni.serval.ibir.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Utils class to calculate ochiai coeffecient: semantic similarity of a mutant with a bug.
 * Ochiai formula I.
 *
 */
public final class OchiaiUtils {

    private static Logger log = LoggerFactory.getLogger(OchiaiUtils.class);

    public static final String OCHIAI_COLUMN_NAME = "OchiaiCoef";

    private static final String FAILED_TESTS_FILE_END = ".txt";
    private static final String FAILED_TESTS_BY_BUG_DIR = "/Users/ahmed.khanfir/anil-tbar/FailedTestCases/";

    private OchiaiUtils() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }

    public static double calculateOchiai(Set<String> brokenTestsByBug, Collection<String> brokenTestsByMutant) {
        assert brokenTestsByBug != null && !brokenTestsByBug.isEmpty();
        if (brokenTestsByMutant == null || brokenTestsByMutant.isEmpty()) return 0d;
        Set<String> intersection = intersection(brokenTestsByBug, brokenTestsByMutant);
        double prod = brokenTestsByBug.size() * brokenTestsByMutant.size();
        if (prod == 0d || intersection.isEmpty()) return 0d;
        return ((double) intersection.size()) / Math.sqrt(prod);
    }

    public static <T> Set<T> intersection(Collection<T> list1, Collection<T> list2) {
        Set<T> set = new HashSet<>();
        if (list1 != null && list2 != null && !list1.isEmpty() && !list2.isEmpty()) {
            for (T t : list1) {
                if (list2.contains(t)) {
                    set.add(t);
                }
            }
        }
        return set;
    }
}
