/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import net.java.sip.communicator.service.version.*;

/**
 * The version service keeps track of the SIP Communicator version that we are
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
     * Returns a <tt>Version</tt> object containing version details of the
     * SIP Communicator version that we're currently running.
     *
     * @return a <tt>Version</tt> object containing version details of the
     *   SIP Communicator version that we're currently running.
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
     *   <tt>version</tt> string.
     */
    public Version parseVersionString(String version)
    {
        /** @todo implement parseVersionString() */
        return null;
    }
}
