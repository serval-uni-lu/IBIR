package edu.lu.uni.serval.ibir.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

public class SharedFileWriteReadTest {

    private static final Path SHARED_DIR = Paths.get("src/test/resources/INPUT/SharedFileWriteReadTest/existingFile");
    private static final Path SHARED_FILE = Paths.get("src/test/resources/INPUT/SharedFileWriteReadTest/existingFile/sharedFile1.txt");

    private static final Path NOT_EXISTING_SHARED_FILE = Paths.get("src/test/resources/INPUT/SharedFileWriteReadTest/existingFile/sharedFile2.txt");

    @Before
    public void setUp() throws Exception {
        FileUtils.createFile(SHARED_FILE.toFile(), "someLine1\n" +
                "someLine2\n");
    }


    @Test
    public void tryWriteNewLine() throws IOException, TimeoutException {
        String sharedFilePath = SHARED_FILE.toAbsolutePath().toString();
        long now = System.currentTimeMillis();
        String lineNames = "dummyLine_" + now + "_";
        for (int i = 0; i < 200; i++) {
            SharedFileWriteRead.writeNewLine(sharedFilePath, lineNames + i);
            assertTrue(SharedFileWriteRead.containsLine(sharedFilePath, lineNames + i));
        }
        for (int i = 0; i < 200; i++) {
            assertTrue(SharedFileWriteRead.containsLine(sharedFilePath, lineNames + i));
        }
    }

    @Test
    public void tryWriteNewLineNotExistingFile() throws IOException, TimeoutException {
        String notExistingSharedFilePath = NOT_EXISTING_SHARED_FILE.toAbsolutePath().toString();
        long now = System.currentTimeMillis();
        String lineNames = "dummyLine_" + now + "_";
        for (int i = 0; i < 200; i++) {
            SharedFileWriteRead.writeNewLine(notExistingSharedFilePath, lineNames + i);
            assertTrue(SharedFileWriteRead.containsLine(notExistingSharedFilePath, lineNames + i));
        }
        for (int i = 0; i < 200; i++) {
            assertTrue(SharedFileWriteRead.containsLine(notExistingSharedFilePath, lineNames + i));
        }
    }

    @Test
    public void tryFindLine() {
        String sharedFilePath = SHARED_FILE.toAbsolutePath().toString();
        String notExistingSharedFilePath = NOT_EXISTING_SHARED_FILE.toAbsolutePath().toString();
        Assert.assertTrue(SharedFileWriteRead.tryFindLine(sharedFilePath, "someLine1"));
        Assert.assertTrue(SharedFileWriteRead.tryFindLine(sharedFilePath, "someLine2"));
        Assert.assertFalse(SharedFileWriteRead.tryFindLine(sharedFilePath, "someLine3"));
        Assert.assertFalse(SharedFileWriteRead.tryFindLine(notExistingSharedFilePath, "someLine1"));
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(SHARED_DIR.toFile());
    }
}