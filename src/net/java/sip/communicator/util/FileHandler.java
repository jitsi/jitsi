/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.util;

import java.io.*;
import java.util.logging.*;

/**
 * Simple file logging <tt>Handler</tt>.
 * Extends java.util.logging.FileHandler and adds the special component to
 * the file pattern - %s which is replaced at runtime with sip-communicator's
 * home directory. If the pattern option is missing creates log
 * directory in sip-communicator's home directory.
 * If the directory is missing create it.
 *
 * @author Damian Minkov
 */
public class FileHandler
    extends java.util.logging.FileHandler
{
    /**
     * Specifies how many output files to cycle through (defaults to 1).
     */
    private static int count = -1;

    /**
     * Specifies an approximate maximum amount to write (in bytes)
     * to any one file.  If this is zero, then there is no limit.
     * (Defaults to no limit).
     */
    private static int limit = -1;

    /**
     * Specifies a pattern for generating the output file name.
     * A pattern consists of a string that includes the special
     * components that will be replaced at runtime such as :
     *  %t, %h, %g, %u.
     * Also adds the special component :
     * %s sip-communicator's home directory, typically -
     * ${net.java.sip.communicator.SC_LOG_DIR_LOCATION}/
     * ${net.java.sip.communicator.SC_HOME_DIR_NAME}.
     * <p>
     * The field is public so that our <tt>Logger</tt> could reset it if
     * necessary.
     */
    public static String pattern = null;

    /**
     * Initialize a <tt>FileHandler</tt> to write to a set of files.  When
     * (approximately) the given limit has been written to one file,
     * another file will be opened.  The output will cycle through a set
     * of count files.
     * <p>
     * The <tt>FileHandler</tt> is configured based on <tt>LogManager</tt>
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control")</tt>.
     * @exception IllegalArgumentException if limit < 0, or count < 1.
     * @exception  IllegalArgumentException if pattern is an empty string
     */
    public FileHandler(String pattern, int limit, int count)
        throws IOException, SecurityException
    {
        super(pattern, limit, count);
    }

    /**
     * Construct a default <tt>FileHandler</tt>.  This will be configured
     * entirely from <tt>LogManager</tt> properties (or their default values).
     * Will change
     * <p>
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     * the caller does not have <tt>LoggingPermission("control"))</tt>.
     * @exception  NullPointerException if pattern property is an empty String.
     */
    public FileHandler()
        throws  IOException,
                SecurityException
    {
        super(getPattern(), getLimit(), getCount());
    }

    /**
     * Returns the limit size for one log file or default 0, which
     * is unlimited.
     * @return the limit size
     */
    private static int getLimit()
    {
        if(limit == -1)
        {
            String limitStr = LogManager.getLogManager().getProperty(
                            FileHandler.class.getName() + ".limit");

            // default value
            limit = 0;

            try
            {
                limit = Integer.parseInt(limitStr);
            }
            catch (Exception ex) {}
        }

        return limit;
    }

    /**
     * Substitute %s in the pattern and creates the directory if it
     * doesn't exist.
     *
     * @return the file pattern.
     */
    private static String getPattern()
    {
        if(pattern == null)
        {
            pattern =
                LogManager.getLogManager().getProperty(
                    FileHandler.class.getName() + ".pattern");

            String homeLocation = System.getProperty(
                "net.java.sip.communicator.SC_LOG_DIR_LOCATION");
            String dirName = System.getProperty(
                "net.java.sip.communicator.SC_HOME_DIR_NAME");

            if(homeLocation != null && dirName != null)
            {
                if(pattern == null)
                    pattern = homeLocation + "/" + dirName +
                            "/log/jitsi%u.log";
                else
                    pattern = pattern.replaceAll("\\%s",
                        homeLocation + "/" + dirName);
            }

            // if pattern is missing and both dir name and home location
            // properties are also not defined its most probably running from
            // source or testing - lets create log directory in working dir.
            if(pattern == null)
                pattern = "./log/jitsi%u.log";

            checkDestinationDirectory(pattern);
        }

        return pattern;
    }

    /**
     * Returns the count of the log files or the default value 1;
     * @return file count
     */
    private static int getCount()
    {
        if(count == -1)
        {
            String countStr = LogManager.getLogManager().getProperty(
                            FileHandler.class.getName() + ".count");

            // default value
            count = 1;

            try
            {
                count = Integer.parseInt(countStr);
            }
            catch (Exception ex) {}
        }

        return count;
    }

    /**
     * Creates the directory in the pattern.
     *
     * @param pattern the directory we'd like to check.
     */
    private static void checkDestinationDirectory(String pattern)
    {
        try
        {
            int ix = pattern.lastIndexOf('/');

            if(ix != -1)
            {
                String dirName = pattern.substring(0, ix);
                dirName = dirName.replaceAll(
                                "%h", System.getProperty("user.home"));
                dirName = dirName.replaceAll(
                                "%t", System.getProperty("java.io.tmpdir"));

                new File(dirName).mkdirs();
            }
        }
        catch (Exception e){}
    }
}
