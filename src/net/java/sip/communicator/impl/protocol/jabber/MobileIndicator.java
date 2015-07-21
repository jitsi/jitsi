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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.extensions.caps.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import org.jivesoftware.smack.util.*;

import java.util.*;

/**
 * Handles all the logic about mobile indicator for contacts.
 * Has to modes, the first is searching for particular string in the beginning
 * of the contact resource and if found and this is the highest priority then
 * the contact in on mobile.
 * The second one and the default one is searching for strings in the node
 * from the contact caps and if found and this is the most connected device
 * then the contact is a mobile one.
 *
 * @author Damian Minkov
 */
public class MobileIndicator
    implements RegistrationStateChangeListener,
               UserCapsNodeListener
{
    /**
     * The parent provider.
     */
    private final ProtocolProviderServiceJabberImpl parentProvider;

    /**
     * Whether we are using the default method for checking for
     * mobile indicator.
     */
    private boolean isCapsMobileIndicator = true;

    /**
     * The strings that we will check.
     */
    private final String[] checkStrings;

    /**
     * A reference to the ServerStoredContactListImpl instance.
     */
    private final ServerStoredContactListJabberImpl ssclCallback;

    /**
     * The account property to activate the mode for checking the resource
     * names, the strings to check whether a resource starts with can be
     * entered separated by comas.
     */
    private static final String MOBILE_INDICATOR_RESOURCE_ACC_PROP =
        "MOBILE_INDICATOR_RESOURCE";

    /**
     * The account property to activate the mode for checking the contact
     * caps, the strings to check whether a caps contains with can be
     * entered separated by comas.
     */
    private static final String MOBILE_INDICATOR_CAPS_ACC_PROP =
        "MOBILE_INDICATOR_CAPS";

    /**
     * Construct Mobile indicator.
     * @param parentProvider the parent provider.
     * @param ssclCallback the callback for the contact list to obtain contacts.
     */
    public MobileIndicator(ProtocolProviderServiceJabberImpl parentProvider,
                           ServerStoredContactListJabberImpl ssclCallback)
    {
        this.parentProvider = parentProvider;
        this.ssclCallback = ssclCallback;

        String indicatorResource = parentProvider.getAccountID()
            .getAccountProperties().get(MOBILE_INDICATOR_RESOURCE_ACC_PROP);
        if(indicatorResource != null
            && indicatorResource.length() > 0)
        {
            isCapsMobileIndicator = false;
            checkStrings = indicatorResource.split(",");
        }
        else
        {
            String indicatorCaps = parentProvider.getAccountID()
                .getAccountProperties().get(MOBILE_INDICATOR_CAPS_ACC_PROP);
            if(indicatorCaps == null
                || indicatorCaps.length() == 0)
            {
                indicatorCaps = "android";
            }

            checkStrings = indicatorCaps.split(",");

            this.parentProvider.addRegistrationStateChangeListener(this);
        }
    }

    /**
     * Called when resources have been updated for a contact, on
     * presence changed.
     * @param contact the contact
     */
    public void resourcesUpdated(ContactJabberImpl contact)
    {
        if(isCapsMobileIndicator)
        {
            // we update it also here, cause sometimes caps update comes
            // before presence changed and contacts are still offline
            // and we dispatch wrong initial mobile indicator
            updateMobileIndicatorUsingCaps(contact.getAddress());
            return;
        }

        // checks resource starts with String and is current highest priority
        int highestPriority = Integer.MIN_VALUE;
        List<ContactResource> highestPriorityResources =
            new ArrayList<ContactResource>();

        Collection<ContactResource> resources = contact.getResources();

        // sometimes volatile contacts do not have resources
        if(resources == null)
            return;

        for(ContactResource res : resources)
        {
            if(!res.getPresenceStatus().isOnline())
                continue;

            int prio = res.getPriority();

            if(prio >= highestPriority)
            {
                if(highestPriority != prio)
                    highestPriorityResources.clear();

                highestPriority = prio;

                highestPriorityResources.add(res);
            }
        }

        // check whether all are mobile
        boolean allMobile = false;
        for(ContactResource res : highestPriorityResources)
        {
            if(res.isMobile())
                allMobile = true;
            else
            {
                allMobile = false;
                break;
            }
        }

        if(highestPriorityResources.size() > 0)
            contact.setMobile(allMobile);
        else
            contact.setMobile(false);
    }

    /**
     * Checks a resource whether it is mobile or not, by checking the
     * cache.
     * @param resourceName the resource name to check.
     * @param fullJid the jid to check.
     * @return whether resource with that name is mobile or not.
     */
    boolean isMobileResource(String resourceName, String fullJid)
    {
        if(isCapsMobileIndicator)
        {
            EntityCapsManager capsManager  = ssclCallback.getParentProvider()
                .getDiscoveryManager().getCapsManager();

            EntityCapsManager.Caps caps = capsManager.getCapsByUser(fullJid);

            if(caps != null && containsStrings(caps.node, checkStrings))
                return true;
            else
                return false;
        }

        if(startsWithStrings(resourceName, checkStrings))
            return true;
        else
            return false;
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if(evt.getNewState() == RegistrationState.REGISTERED)
        {
            this.parentProvider.getDiscoveryManager()
                    .getCapsManager().addUserCapsNodeListener(this);
        }
    }

    /**
     * Caps for user has been changed.
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    @Override
    public void userCapsNodeAdded(String user, String node, boolean online)
    {
        updateMobileIndicatorUsingCaps(user);
    }

    /**
     * Caps for user has been changed.
     * @param user the user (full JID)
     * @param node the entity caps node#ver
     * @param online indicates if the user for which we're notified is online
     */
    @Override
    public void userCapsNodeRemoved(String user, String node, boolean online)
    {
        updateMobileIndicatorUsingCaps(user);
    }

    /**
     * Update mobile indicator for contact, searching in contact caps.
     * @param user the contact address with or without resource.
     */
    private void updateMobileIndicatorUsingCaps(String user)
    {
        ContactJabberImpl contact =
            ssclCallback.findContactById(StringUtils.parseBareAddress(user));

        if(contact == null)
            return;

        // 1. Find most connected resources and if all are mobile
        int currentMostConnectedStatus = 0;
        List<ContactResource> mostAvailableResources =
            new ArrayList<ContactResource>();

        for(Map.Entry<String, ContactResourceJabberImpl> resEntry
                : contact.getResourcesMap().entrySet())
        {
            ContactResourceJabberImpl res = resEntry.getValue();
            if(!res.getPresenceStatus().isOnline())
                continue;

            // update the mobile indicator of connected resource,
            // as caps have been updated
            boolean oldIndicator = res.isMobile();
            res.setMobile(isMobileResource(
                res.getResourceName(), res.getFullJid()));

            if(oldIndicator != res.isMobile())
            {
                contact.fireContactResourceEvent(
                    new ContactResourceEvent(
                        contact, res, ContactResourceEvent.RESOURCE_MODIFIED));
            }

            int status = res.getPresenceStatus().getStatus();

            if(status > currentMostConnectedStatus)
            {
                if(currentMostConnectedStatus != status)
                    mostAvailableResources.clear();

                currentMostConnectedStatus = status;

                mostAvailableResources.add(res);
            }
        }

        // check whether all are mobile
        boolean allMobile = false;
        for(ContactResource res : mostAvailableResources)
        {
            if(res.isMobile())
                allMobile = true;
            else
            {
                allMobile = false;
                break;
            }
        }

        if(mostAvailableResources.size() > 0)
            contact.setMobile(allMobile);
        else
            contact.setMobile(false);
    }

    /**
     * Checks whether <tt>value</tt> starts
     * one of the <tt>checkStrs</> Strings.
     * @param value the value to check
     * @param checkStrs an array of strings we are searching for.
     * @return <tt>true</tt> if <tt>value</tt> starts one of the Strings.
     */
    private static boolean startsWithStrings(String value, String[] checkStrs)
    {
        for(String str : checkStrs)
        {
            if(str.length() > 0 && value.startsWith(str))
                return true;
        }

        return false;
    }

    /**
     * Checks whether <tt>value</tt> contains
     * one of the <tt>checkStrs</> Strings.
     * @param value the value to check
     * @param checkStrs an array of strings we are searching for.
     * @return <tt>true</tt> if <tt>value</tt> contains one of the Strings.
     */
    private static boolean containsStrings(String value, String[] checkStrs)
    {
        for(String str : checkStrs)
        {
            if(str.length() > 0 && value.contains(str))
                return true;
        }

        return false;
    }
}
