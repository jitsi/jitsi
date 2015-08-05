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
