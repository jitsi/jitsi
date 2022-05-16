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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * SubscriptionEvents indicate creation removal or failure of a given
 * Subscription. Note that in SIP Communicator the terms Subscription and
 * Contact are quite similar: A contact becomes available and it is possible
 * to query its presence status and user information, once a
 * subscription for this contact has been created.
 *
 * @author Emil Ivov
 */
public class SubscriptionEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private int eventID = -1;

    /**
     * Indicates that the SubscriptionEvent instance was triggered by the
     * creation of a new subscription
     */
    public static final int SUBSCRIPTION_CREATED = 1;

    /**
     * Indicates that the SubscriptionEvent instance was triggered by the
     * removal of an existing subscription
     */
    public static final int SUBSCRIPTION_REMOVED = 2;

    /**
     * Indicates that the SubscriptionEvent instance was triggered by the fact
     * that no confirmation of the successful completion of a new subscription
     * has been received.
     */
    public static final int SUBSCRIPTION_FAILED  = 3;

    /**
     * Indicates that the SubscriptionEvent instance was triggered by the fact
     * that the presence of a particular contact in the contact list has been
     * confirmed by the server (resolved).
     */
    public static final int SUBSCRIPTION_RESOLVED  = 4;

    /**
     * Error code unknown
     */
    public static final int ERROR_UNSPECIFIED = -1;


    private ProtocolProviderService sourceProvider = null;
    private ContactGroup  parentGroup = null;
    private int errorCode = ERROR_UNSPECIFIED;
    private String errorReason = null;

    /**
     * Creates a new Subscription event according to the specified parameters.
     * @param source the Contact instance that this subscription pertains to.
     * @param provider the ProtocolProviderService instance where this event
     * occurred
     * @param parentGroup the ContactGroup underwhich the corresponding Contact
     * is located
     * @param eventID one of the SUBSCRIPTION_XXX static fields indicating the
     * nature of the event.
     */
    public SubscriptionEvent( Contact source,
                       ProtocolProviderService provider,
                       ContactGroup parentGroup,
                       int eventID)
    {
        this(source, provider, parentGroup, eventID, ERROR_UNSPECIFIED, null);
    }

    /**
     * Creates a new Subscription event according to the specified parameters.
     * @param source the Contact instance that this subscription pertains to.
     * @param provider the ProtocolProviderService instance where this event
     * occurred
     * @param parentGroup the ContactGroup underwhich the corresponding Contact
     * is located
     * @param eventID one of the SUBSCRIPTION_XXX static fields indicating the
     * nature of the event.
     * @param errorCode the error code
     * @param errorReason the error reason string
     */
    public SubscriptionEvent( Contact source,
                       ProtocolProviderService provider,
                       ContactGroup parentGroup,
                       int eventID,
                       int errorCode,
                       String errorReason)
    {
        super(source);
        this.sourceProvider = provider;
        this.parentGroup = parentGroup;
        this.eventID = eventID;
        this.errorCode = errorCode;
        this.errorReason = errorReason;
    }

    /**
     * Returns the provider that the source contact belongs to.
     * @return the provider that the source contact belongs to.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return sourceProvider;
    }

    /**
     * Returns the provider that the source contact belongs to.
     * @return the provider that the source contact belongs to.
     */
    public Contact getSourceContact()
    {
        return (Contact)getSource();
    }

    /**
     * Returns (if applicable) the group containing the contact that cause this
     * event. In the case of a non persistent presence operation set this
     * field is null.
     * @return the ContactGroup (if there is one) containing the contact that
     * caused the event.
     */
    public ContactGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns a String representation of this ContactPresenceStatusChangeEvent
     *
     * @return  A a String representation of this <tt>SubscriptionEvent</tt>.
     */
    @Override
    public String toString()
    {
        StringBuffer buff
            = new StringBuffer("SubscriptionEvent-[ ContactID=");
        buff.append(getSourceContact().getAddress());
        buff.append(", eventID=").append(getEventID());
        if(getParentGroup() != null)
            buff.append(", ParentGroup=").append(getParentGroup()
                    .getGroupName());
        return buff.toString();
    }

    /**
     * Returns an event id specifying whether the type of this event (e.g.
     * SUBSCRIPTION_CREATED, SUBSCRIPTION_FAILED and etc.)
     * @return one of the SUBSCRIPTION_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * If event is SUBSCRIPTION_FAILED, returns the error code
     * of the failed event
     * @return error code
     */
    public int getErrorCode()
    {
        return errorCode;
    }

    /**
     * If event is SUBSCRIPTION_FAILED, returns the reason of the error
     * for the failed event
     * @return the String reason for the error
     */
    public String getErrorReason()
    {
        return errorReason;
    }
}
