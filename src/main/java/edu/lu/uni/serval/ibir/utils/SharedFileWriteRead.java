package edu.lu.uni.serval.ibir.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeoutException;

/**
 * check that the file exists or creates it.
 * for every read or write request, first, try to lock the file with a timeout 1 minute.
 * implement the read, write
 * don't forget the closing of the streams.
 * test this with some system tests, that try to read/write many lines in parallel, and assert:
 * all what they tried to write is in the end written
 * everytime that they try to write something, check that they could write it.
 */
public final class SharedFileWriteRead {

    private static Logger log = LoggerFactory.getLogger(SharedFileWriteRead.class);

    private static final long ACCESS_TIMEOUT_MS = 1l * 60l * 1000l;


    private SharedFileWriteRead() {
        throw new IllegalAccessError("Utility class: No instance allowed, static access only.");
    }

    public static void tryWriteNewLine(String path, String insertLine) {
        try {
            writeNewLine(path, insertLine);
        } catch (Throwable throwable) {
            log.info("failed to write to file:");
            log.info(path);
            log.info("failed to write line: " + insertLine);
            log.info("failure cause: ", throwable);
        }
    }

    public static boolean tryFindLine(String path, String searchLine) {
        try {
            return containsLine(path, searchLine);
        } catch (Throwable throwable) {
            log.info("failed to search in file:");
            log.info(path);
            log.info("failed to search line: " + searchLine);
            log.info("failure cause: ", throwable);
        }
        return false;
    }

    public static void writeNewLine(String path, String insertLine) throws IOException, TimeoutException {
        assert insertLine != null && insertLine.length() > 0;
        RandomAccessFile randomAccessFile = null;
        FileLock fl = null;
        OutputStream outputStream = null;
        PrintWriter out = null;
        FileOutputStream fileOutputStream = null;
        try {
            randomAccessFile = getAccess(path);
            fl = lockFile(randomAccessFile);
            if (fl != null) {
                fileOutputStream = new FileOutputStream(path, true);
                // outputStream = Channels.newOutputStream(fl.channel());
                out = new PrintWriter(fileOutputStream);
                out.println(insertLine);
                out.flush();
            }
        } catch (FileNotFoundException e) { // better to create this before hand.
            File file = new File(path);
            FileUtils.createFile(file, insertLine);
        } finally {
            try {
                if (randomAccessFile != null)
                    randomAccessFile.close();
            } catch (Throwable ignored) {
            }

            try {
                if (randomAccessFile != null)
                    randomAccessFile.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fl != null)
                    fl.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (Throwable ignored) {
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (Throwable ignored) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (Throwable ignored) {
            }

        }
    }

    public static boolean containsLine(String path, String searchLine) throws IOException, TimeoutException {
        assert searchLine != null && searchLine.length() > 0;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = getAccess(path);
        } catch (FileNotFoundException e) {
            log.info("file doesn't exist : " + path);
            return false;
        }
        FileLock fl = null;
        InputStream fin = null;
        BufferedReader reader = null;
        try {
            fl = lockFile(randomAccessFile);
            fin = Channels.newInputStream(fl.channel());
            reader = new BufferedReader(new InputStreamReader(fin));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (searchLine.equals(line)) return true;
            }
            return false;
        } finally {
            try {
                if (randomAccessFile != null)
                    randomAccessFile.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fl != null)
                    fl.close();
            } catch (Throwable ignored) {
            }
            try {
                if (fin != null)
                    fin.close();
            } catch (Throwable ignored) {
            }
            try {
                if (reader != null)
                    reader.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static FileLock lockFile(RandomAccessFile randomAccessFile) throws IOException, TimeoutException {
        long startTime = System.currentTimeMillis();
        FileLock result = null;
        while (result == null && System.currentTimeMillis() - startTime < ACCESS_TIMEOUT_MS) {
            result = randomAccessFile.getChannel().tryLock();
        }
        if (result == null) {
            throw new TimeoutException("couldn't lock this file");
        }
        return result;
    }

    public static RandomAccessFile getAccess(String path) throws FileNotFoundException {
        return new RandomAccessFile(path, "rw");
    }
}
