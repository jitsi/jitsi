/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
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
    public List<UIContactDetail> getAdditionalNumbers()
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
                    if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
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
                                    GuiActivator.getResources().getImage(
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
