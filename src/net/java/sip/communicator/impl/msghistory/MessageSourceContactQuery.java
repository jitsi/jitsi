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
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import java.util.*;
import java.util.regex.*;

/**
 * The query which creates source contacts and uses the values stored in
 * <tt>MessageSourceService</tt>.
 *
 * @author Damian Minkov
 */
public class MessageSourceContactQuery
    extends AsyncContactQuery<MessageSourceService>
{
    /**
     * Constructs.
     *
     * @param messageSourceService
     */
    MessageSourceContactQuery(MessageSourceService messageSourceService)
    {
        super(messageSourceService,
            Pattern.compile("",
                Pattern.CASE_INSENSITIVE | Pattern.LITERAL),
            false);
    }

    /**
     * Creates <tt>MessageSourceContact</tt> for all currently cached
     * recent messages in the <tt>MessageSourceService</tt>.
     */
    @Override
    public void run()
    {
        getContactSource().updateRecentMessages();
    }

    /**
     * Updates capabilities from <tt>EventObject</tt> for the found
     * <tt>MessageSourceContact</tt> equals to the <tt>Object</tt> supplied.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     * @param srcObj used to search for <tt>MessageSourceContact</tt>
     * @param eventObj the values used for the update
     */
    public void updateCapabilities(Object srcObj, EventObject eventObj)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                ((MessageSourceContact)msc).initDetails(eventObj);

                break;
            }
        }
    }

    /**
     * Updates capabilities from <tt>Contact</tt> for the found
     * <tt>MessageSourceContact</tt> equals to the <tt>Object</tt> supplied.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     * @param srcObj used to search for <tt>MessageSourceContact</tt>
     * @param contact the values used for the update
     */
    public void updateCapabilities(Object srcObj, Contact contact)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                ((MessageSourceContact)msc).initDetails(false, contact);

                break;
            }
        }
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * changed.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     *
     * @param srcObj the <tt>Object</tt> representing a recent message
     * which has been changed and corresponding <tt>SourceContact</tt>
     * which the registered <tt>ContactQueryListener</tt>s are to be
     * notified about
     */
    public void updateContact(Object srcObj, EventObject eventObject)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                ((MessageSourceContact)msc).update(eventObject);

                super.fireContactChanged(msc);

                break;
            }
        }
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * changed.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     *
     * @param srcObj the <tt>Object</tt> representing a recent message
     * which has been changed and corresponding <tt>SourceContact</tt>
     * which the registered <tt>ContactQueryListener</tt>s are to be
     * notified about
     */
    public void fireContactChanged(Object srcObj)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                super.fireContactChanged(msc);

                break;
            }
        }
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * changed.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     *
     * @param srcObj the <tt>Object</tt> representing a recent message
     * which has been changed and corresponding <tt>SourceContact</tt>
     * which the registered <tt>ContactQueryListener</tt>s are to be
     * notified about
     */
    public void updateContactStatus(Object srcObj,
                                    PresenceStatus status)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                ((MessageSourceContact)msc).setStatus(status);

                super.fireContactChanged(msc);

                break;
            }
        }
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * changed.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     *
     * @param srcObj the <tt>Object</tt> representing a recent message
     * which has been changed and corresponding <tt>SourceContact</tt>
     * which the registered <tt>ContactQueryListener</tt>s are to be
     * notified about
     */
    public void updateContactDisplayName(Object srcObj,
                                         String newName)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc)
                && msc instanceof MessageSourceContact)
            {
                ((MessageSourceContact)msc).setDisplayName(newName);

                super.fireContactChanged(msc);

                break;
            }
        }
    }

    /**
     * Notifies the <tt>ContactQueryListener</tt>s registered with this
     * <tt>ContactQuery</tt> that a <tt>SourceContact</tt> has been
     * removed.
     * Note that Object may not be <tt>MessageSourceContact</tt>, but its
     * equals method can return true for message source contact instances.
     *
     * @param srcObj representing the message and its corresponding
     * <tt>SourceContact</tt> which has been removed and which the registered
     * <tt>ContactQueryListener</tt>s are to be notified about
     */
    public void fireContactRemoved(Object srcObj)
    {
        for(SourceContact msc : getQueryResults())
        {
            if(srcObj.equals(msc))
            {
                super.fireContactRemoved(msc);

                break;
            }
        }
    }

    /**
     * Adds a specific <tt>SourceContact</tt> to the list of
     * <tt>SourceContact</tt>s to be returned by this <tt>ContactQuery</tt> in
     * response to {@link #getQueryResults()}.
     *
     * @param sourceContact the <tt>SourceContact</tt> to be added to the
     * <tt>queryResults</tt> of this <tt>ContactQuery</tt>
     * @return <tt>true</tt> if the <tt>queryResults</tt> of this
     * <tt>ContactQuery</tt> has changed in response to the call
     */
    public boolean addQueryResult(SourceContact sourceContact)
    {
        return super.addQueryResult(sourceContact, false);
    }
}
