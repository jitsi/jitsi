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
package net.java.sip.communicator.plugin.desktoputil;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.call.*;

import java.util.*;

/**
 * Utility class that is obtained per metacontact. Used to check is a telephony
 * service, video calls and desktop sharing are enabled per contact from the
 * metacontact, or globally for the metacontct.
 * @author Damian Minkov
 */
public class UIPhoneUtil
    extends MetaContactPhoneUtil
{
    /**
     * Creates utility instance for <tt>metaContact</tt>.
     *
     * @param metaContact the metacontact checked in the utility.
     */
    protected UIPhoneUtil(MetaContact metaContact)
    {
        super(metaContact);
    }

    /**
     * Obtains an instance of this utility class for the given
     * <tt>metaContact</tt>.
     *
     * @param metaContact the <tt>MetaContact</tt>, for which the instance of
     * this utility class would be created
     * @return UIPhoneUtil for the given <tt>metaContact</tt>.
     */
    public static UIPhoneUtil getPhoneUtil(MetaContact metaContact)
    {
        return new UIPhoneUtil(metaContact);
    }

    /**
     * Searches for additional phone numbers found in contact information
     *
     * @return additional phone numbers found in contact information;
     */
    public List<UIContactDetail> getAdditionalMobileNumbers()
    {
        return getAdditionalNumbers(true);
    }

    /**
     * Searches for additional phone numbers found in contact information
     *
     * @return additional phone numbers found in contact information;
     */
    public List<UIContactDetail> getAdditionalNumbers()
    {
        return getAdditionalNumbers(false);
    }

    /**
     * Searches for additional phone numbers found in contact information
     *
     * @return additional phone numbers found in contact information;
     */
    private List<UIContactDetail> getAdditionalNumbers(boolean onlyMobile)
    {
        List<UIContactDetail> telephonyContacts
            = new ArrayList<UIContactDetail>();

        Iterator<Contact> contacts = getMetaContact().getContacts();

        while(contacts.hasNext())
        {
            Contact contact = contacts.next();
            OperationSetServerStoredContactInfo infoOpSet =
                contact.getProtocolProvider().getOperationSet(
                    OperationSetServerStoredContactInfo.class);
            Iterator<GenericDetail> details;
            ArrayList<String> phones = new ArrayList<String>();

            if(infoOpSet != null)
            {
                details = infoOpSet.getAllDetailsForContact(contact);

                while(details.hasNext())
                {
                    GenericDetail d = details.next();

                    boolean process = false;

                    if(onlyMobile)
                    {
                        if(d instanceof MobilePhoneDetail)
                            process = true;
                    }
                    else if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
                    {
                        process = true;
                    }

                    if(process)
                    {
                        PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                        if(pnd.getNumber() != null &&
                            pnd.getNumber().length() > 0)
                        {
                            // skip phones which were already added
                            if(phones.contains(pnd.getNumber()))
                                continue;

                            phones.add(pnd.getNumber());

                            UIContactDetail cd =
                                new UIContactDetailImpl(
                                    pnd.getNumber(),
                                    pnd.getNumber() +
                                    " (" + getLocalizedPhoneNumber(d) + ")",
                                    null,
                                    new ArrayList<String>(),
                                    DesktopUtilActivator.getResources()
                                        .getImage(
                                            "service.gui.icons.EXTERNAL_PHONE"),
                                    null,
                                    null,
                                    pnd)
                            {
                                @Override
                                public PresenceStatus getPresenceStatus()
                                {
                                    return null;
                                }
                            };
                            telephonyContacts.add(cd);
                        }
                    }
                }
            }
        }

        return telephonyContacts;
    }
}
