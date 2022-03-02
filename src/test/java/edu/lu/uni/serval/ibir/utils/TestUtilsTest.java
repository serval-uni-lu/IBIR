package edu.lu.uni.serval.ibir.utils;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;


public class TestUtilsTest {

    @Test
    public void isFailingTestLine() {
        Assert.assertTrue(TestUtils.isFailingTestLine("  - org.apache.commons.lang.text.StrBuilderAppendInsertTest::testAppendFixedWidthPadRight_int"));
        Assert.assertTrue(TestUtils.isFailingTestLine("  - org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299"));
        Assert.assertTrue(TestUtils.isFailingTestLine("  - org.apache.commons.lang.text.StrBuilderAppendInsertTest::testAppendFixedWidthPadRight"));
        Assert.assertFalse(TestUtils.isFailingTestLine("Running ant (compile.tests)................................................ OK"));
        Assert.assertFalse(TestUtils.isFailingTestLine("Running ant (run.dev.tests)................................................ OK"));
    }

    @Test
    public void loadTestsBrokenByOriginalBug() throws CsvValidationException, IOException {
        Set<String> expectedRes = new HashSet<String>() {{
            addAll(Arrays.asList("org.apache.commons.cli.OptionsTest::testMissingOptionsException;org.apache.commons.cli.OptionsTest::testMissingOptionException".split(";")));
        }};
        Set<String> res = TestUtils.loadTestsBrokenByOriginalBug("/Users/ahmed.khanfir/PycharmProjects/iFixR/input/d4j_v2/evaluation/bugs/Cli_bugs.csv", 4);
        assertNotNull(res);
        assertTrue(res.size() > 0);
        assertEquals(expectedRes,res);
    }

}