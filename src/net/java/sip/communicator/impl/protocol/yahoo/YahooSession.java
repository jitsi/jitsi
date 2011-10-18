/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import ymsg.network.*;

/**
 * Extends The Yahoo session to have access to some
 * protected functionality
 * Not working for now.
 *
 * @author Damian Minkov
 */
public class YahooSession
    extends Session
{
    /**
     * Renames a group. Not working for now
     */
    public void renameGroup(String oldName, String newName)
        throws IOException
    {
        transmitGroupRename(oldName, newName);
    }

    /**
     * Removes the server part from the given id
     */
    public static String getYahooUserID(String id)
    {
        return (id.indexOf("@") > -1 )
                    ? id.substring(0, id.indexOf("@"))
                    : id;
    }

    /**
     * Sending typing notifications
     * @param to user we are notifing
     * @param from our user id
     */
    void keyTyped(String to, String from)
    {
        try {
            transmitNotify(to, from, true, " ", NOTIFY_TYPING);
        }catch(IOException e){}
    }

    /**
     * Sending stop typing notifications
     * @param to user we are notifing
     * @param from our user id
     */
    void stopTyping(String to, String from)
    {
        try {
            transmitNotify(to, from, false, " ", NOTIFY_TYPING);
        }catch(IOException e){}
    }
}
