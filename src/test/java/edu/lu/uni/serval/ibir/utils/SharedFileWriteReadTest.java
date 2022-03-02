package edu.lu.uni.serval.ibir.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

public class SharedFileWriteReadTest {

    private static final String SHARED_FILE = "/Users/ahmed.khanfir/anil-tbar/src/test/resources/INPUT/SharedFileWriteReadTest/existingFile/sharedFile1.txt";

    private static final String NOT_EXISTING_SHARED_FILE = "/Users/ahmed.khanfir/anil-tbar/src/test/resources/INPUT/SharedFileWriteReadTest/existingFile/sharedFile2.txt";

    @Before
    public void setUp() throws Exception {
        FileUtils.createFile(new File(SHARED_FILE), "someLine1\n" +
                "someLine2\n");
    }


    @Test
    public void tryWriteNewLine() throws IOException, TimeoutException {
        long now = System.currentTimeMillis();
        String lineNames = "dummyLine_" + now + "_";
        for (int i = 0; i < 200; i++) {
            SharedFileWriteRead.writeNewLine(SHARED_FILE, lineNames + i);
            assertTrue(SharedFileWriteRead.containsLine(SHARED_FILE, lineNames + i));
        }
        for (int i = 0; i < 200; i++) {
            assertTrue(SharedFileWriteRead.containsLine(SHARED_FILE, lineNames + i));
        }
    }

    @Test
    public void tryWriteNewLineNotExistingFile() throws IOException, TimeoutException {
        long now = System.currentTimeMillis();
        String lineNames = "dummyLine_" + now + "_";
        for (int i = 0; i < 200; i++) {
            SharedFileWriteRead.writeNewLine(NOT_EXISTING_SHARED_FILE, lineNames + i);
            assertTrue(SharedFileWriteRead.containsLine(NOT_EXISTING_SHARED_FILE, lineNames + i));
        }
        for (int i = 0; i < 200; i++) {
            assertTrue(SharedFileWriteRead.containsLine(NOT_EXISTING_SHARED_FILE, lineNames + i));
        }
    }

    @Test
    public void tryFindLine() {
        Assert.assertTrue(SharedFileWriteRead.tryFindLine(SHARED_FILE, "someLine1"));
        Assert.assertTrue(SharedFileWriteRead.tryFindLine(SHARED_FILE, "someLine2"));
        Assert.assertFalse(SharedFileWriteRead.tryFindLine(SHARED_FILE, "someLine3"));
        Assert.assertFalse(SharedFileWriteRead.tryFindLine(NOT_EXISTING_SHARED_FILE, "someLine1"));
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(new File("/Users/ahmed.khanfir/anil-tbar/src/test/resources/INPUT/SharedFileWriteReadTest/existingFile"));
    }
}