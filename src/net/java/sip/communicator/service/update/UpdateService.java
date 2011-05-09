/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.update;

/**
 * Checking for software updates service.
 *
 * @author Yana Stamcheva
 */
public interface UpdateService
{
    /**
     * Checks for updates.
     *
     * @param notifyAboutNewestVersion <tt>true</tt> if the user is to be
     * notified if they have the newest version already; otherwise,
     * <tt>false</tt>
     */
    public void checkForUpdates(boolean notifyAboutNewestVersion);
}
