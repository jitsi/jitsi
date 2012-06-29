/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.loggingutils;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * Collects logs and save them in compressed zip file.
 * @author Damian Minkov
 */
public class LogsCollector
{
    /**
     * Our Logger.
     */
    private static final Logger logger
            = Logger.getLogger(LogsCollector.class);

    /**
     * The name of the log dir.
     */
    final static String LOGGING_DIR_NAME = "log";

    /**
     * The prefix name of standard java crash log file.
     */
    private static final String JAVA_ERROR_LOG_PREFIX = "hs_err_pid";

    /**
     * The date format used in file names.
     */
    private static final SimpleDateFormat FORMAT
        = new SimpleDateFormat("yyyy-MM-dd@HH.mm.ss");

    /**
     * The pattern we use to match crash logs.
     */
    private static Pattern JAVA_ERROR_LOG_PATTERN =
            Pattern.compile(
                    Pattern.quote("sip.communicator"),
                    Pattern.CASE_INSENSITIVE);

    /**
     * Save the log files in archived file. If destination is a folder
     * we generate filename with current date and time. If the destination
     * is null we do nothing and if its a file we use at, as we check
     * does it end with zip extension, is missing we add it.
     * @param destination the possible destination archived file
     * @param optional an optional file to be added to the archive.
     * @return the resulting file in zip format
     */
    public static File collectLogs(File destination, File optional)
    {
        if(destination == null)
            return null;

        if(!destination.isDirectory())
        {
            if(!destination.getName().endsWith("zip"))
                destination = new File(destination.getParentFile(),
                        destination.getName() + ".zip");
        }
        else
        {
            destination = new File(destination, getDefaultFileName());
        }

        try
        {
            ZipOutputStream out = new ZipOutputStream(
                new FileOutputStream(destination));

            collectHomeFolderLogs(out);
            collectJavaCrashLogs(out);

            if(optional != null)
            {
                addFileToZip(optional, out);
            }

            out.close();

            return destination;
        }
        catch(FileNotFoundException ex)
        {
            logger.error("Error creating logs file archive", ex);
        }
        catch(IOException ex)
        {
            logger.error("Error closing archive file", ex);
        }

        return null;
    }

    /**
     * The default filename to use.
     * @return the default filename to use.
     */
    public static String getDefaultFileName()
    {
        return FORMAT.format(new Date()) + "-logs.zip";
    }

    /**
     * Collects all files from log folder except the lock file.
     * And put them in the zip file as zip entries.
     * @param out the output zip file.
     */
    private static void collectHomeFolderLogs(ZipOutputStream out)
    {
        try
        {
            File[] fs = LoggingUtilsActivator.getFileAccessService()
                .getPrivatePersistentDirectory(LOGGING_DIR_NAME).listFiles();

            for(File f : fs)
            {
                if(f.getName().endsWith(".lck"))
                    continue;

                addFileToZip(f, out);
            }
        }
        catch(Exception e)
        {
            logger.error("Error obtaining logs folder", e);
        }
    }

    /**
     * Copies a file to the given archive.
     * @param file the file to copy.
     * @param out the output archive stream.
     */
    private static void addFileToZip(File file, ZipOutputStream out)
    {
        byte[] buf = new byte[1024];

        try
        {
            FileInputStream in = new FileInputStream(file);

            // new ZIP entry
            out.putNextEntry(new ZipEntry(
                LOGGING_DIR_NAME + File.separator + file.getName()));

            // transfer bytes
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }

            out.closeEntry();
            in.close();
        }
        catch(FileNotFoundException ex)
        {
            logger.error("Error obtaining file to archive", ex);
        }
        catch(IOException ex)
        {
            logger.error("Error saving file to archive", ex);
        }
    }

    /**
     * Searches for java crash logs belonging to us and add them to
     * the log archive.
     *
     * @param out the output archive stream.
     */
    private static void collectJavaCrashLogs(ZipOutputStream out)
    {
        // First check in working dir
        addCrashFilesToArchive(
            new File(".").listFiles(),
            JAVA_ERROR_LOG_PREFIX,
            out);

        // If we don't have permissions to write to working directory
        // the crash logs maybe in the temp directory,
        // or on the Desktop if its windows
        if(OSUtils.IS_WINDOWS)
        {
            // check Desktop
            File[] desktopFiles = new File(System.getProperty(
                    "user.home") + File.separator + "Desktop").listFiles();
            addCrashFilesToArchive(desktopFiles, JAVA_ERROR_LOG_PREFIX, out);
        }

        if(OSUtils.IS_MAC)
        {
            // ~/Library/Logs/CrashReporter
            addCrashFilesToArchive(
                new File("./Library/Logs/CrashReporter").listFiles(),null, out);
            // /Library/Logs/CrashReporter
            addCrashFilesToArchive(
                new File("/Library/Logs/CrashReporter").listFiles(), null, out);
        }
        else
        {
            // search in /tmp folder
            // Solaris OS and Linux the temporary directory is /tmp
            // windows TMP or TEMP environment variable is the temporary folder

            //java.io.tmpdir
            File[] tempFiles =
                new File(System.getProperty("java.io.tmpdir")).listFiles();
            addCrashFilesToArchive(tempFiles, JAVA_ERROR_LOG_PREFIX, out);
        }
    }

    /**
     * Checks if file is a crash log file and does it belongs to us.
     * @param files files to check.
     * @param filterStartsWith a prefix for the files, can be null if no
     * prefix check should be made.
     * @param out the output archive stream.
     */
    private static void addCrashFilesToArchive(
            File files[], String filterStartsWith, ZipOutputStream out)
    {
        // no files to add
        if(files == null)
            return;

        // First check in working dir
        for(File f: files)
        {
            if(filterStartsWith != null
                && !f.getName().startsWith(JAVA_ERROR_LOG_PREFIX))
            {
                continue;
            }

            if(isOurCrashLog(f))
            {
                addFileToZip(f, out);
            }
        }
    }

    /**
     * Checks whether the crash log file is for our application.
     * @param file the crash log file.
     * @return <tt>true</tt> if error log is ours.
     */
    private static boolean isOurCrashLog(File file)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            while((line = reader.readLine()) != null)
                if(JAVA_ERROR_LOG_PATTERN.matcher(line).find())
                    return true;
        }
        catch(Throwable e)
        {}

        return false;
    }
}
