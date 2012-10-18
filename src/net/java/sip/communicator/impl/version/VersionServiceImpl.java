/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import org.jitsi.service.version.*;

import java.util.regex.*;

/**
 * The version service keeps track of the Jitsi version that we are
 * currently running. Other modules (such as a Help->About dialog) query and
 * use this service in order to show the current application version.
 * <p>
 * This version service implementation is based around the VersionImpl class
 * where all details of the version are statically defined.
 *
 * @author Emil Ivov
 */
public class VersionServiceImpl
    implements VersionService
{
    /**
     * The pattern that will parse strings to version object.
     */
    private static final Pattern PARSE_VERSION_STRING_PATTERN =
        Pattern.compile("(\\d+)\\.(\\d+)\\.([\\d\\.]+)");

    /**
     * Returns a <tt>Version</tt> object containing version details of the
     * Jitsi version that we're currently running.
     *
     * @return a <tt>Version</tt> object containing version details of the
     *   Jitsi version that we're currently running.
     */
    public Version getCurrentVersion()
    {
        return VersionImpl.currentVersion();
    }

    /**
     * Returns a Version instance corresponding to the <tt>version</tt>
     * string.
     *
     * @param version a version String that we have obtained by calling a
     *   <tt>Version.toString()</tt> method.
     * @return the <tt>Version</tt> object corresponding to the
     *   <tt>version</tt> string. Or null if we cannot parse the string.
     */
    public Version parseVersionString(String version)
    {
        Matcher matcher = PARSE_VERSION_STRING_PATTERN.matcher(version);

        if(matcher.matches() && matcher.groupCount() == 3)
        {
            return VersionImpl.customVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3));
        }

        return null;
    }
}
