/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import org.apache.tools.ant.*;

/**
 * An ant task that we use for recording
 * @author Emil Ivov
 */
public class SipCommunicatorVersionTask
    extends Task
{
    /**
     * The property to store the sip-communicator version.
     */
    private String property;

    /**
     * Sets the name of the property where we should store the sip-communicator
     * version.
     * @param property the name of the property where we should store the
     * sip-communicator version.
     */
    public void setProperty(String property)
    {
        this.property = property;
    }

    /**
     * Called by the project to let the task do its work. This method may be
     * called more than once, if the task is invoked more than once.
     * For example,
     * if target1 and target2 both depend on target3, then running
     * "ant target1 target2" will run all tasks in target3 twice.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute()
        throws BuildException
    {
        String version = VersionImpl.currentVersion().toString();
        getProject().setProperty(property, version);
    }
}
