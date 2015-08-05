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
package net.java.sip.communicator.impl.protocol.icq;

import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;

/**
 * Implements the buddy interface as a means of providing volatile contacts.
 *
 * @author Emil Ivov
 */
class VolatileBuddy
    implements Buddy
{
    private Screenname screenname = null;

    /**
     *  Use when creating unresolved contact during authorization process
     *  to display contact in the group with awaiting authorization contacts
     */
    private boolean isAwaitingAuthorization = false;

    /**
     * Constructs a <tt>VolatileBuddy</tt> from the specified screenname.
     * @param screenname the <tt>screenname</tt> to construct the buddy from.
     */
    public VolatileBuddy(Screenname screenname)
    {
        this.screenname = screenname;
    }

    /**
     * Returns the screenname of this contact
     * @return this contact's screen name.
     */
    public String getAlias()
    {
        return null;
    }

    /**
     * Returns the screenname of this contact.
     *
     * @return the Screenname of this contact.
     */
    public Screenname getScreenname()
    {
        return screenname;
    }


    public void addBuddyListener(BuddyListener listener){}
    public int getAlertActionMask(){return 0;}
    public int getAlertEventMask(){return 0;}
    public String getAlertSound(){return "";}
    public String getBuddyComment(){return "";}
    public BuddyList getBuddyList(){return null;}
    public boolean isActive(){return false;}
    public void removeBuddyListener(BuddyListener listener){}
    public boolean isAwaitingAuthorization(){return isAwaitingAuthorization;}

    public void setAwaitingAuthorization(boolean value)
    {
        this.isAwaitingAuthorization = value;
    }
}
