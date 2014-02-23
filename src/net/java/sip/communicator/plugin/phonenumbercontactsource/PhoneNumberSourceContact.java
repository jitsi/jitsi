/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * The <tt>PhoneNumberSourceContact</tt> extends the
 * <tt>GenericSourceContact</tt> and represents a contact in the
 * <tt>PhoneNumberContactSource</tt>.
 *
 * @author Yana Stamcheva
 */
public class PhoneNumberSourceContact
    extends SortedGenericSourceContact
{
    /**
     * The display details of this contact.
     */
    private String displayDetails;

    /**
     * The protocol contact we wrap.
     */
    private Contact contact;

    /**
     * Creates an instance of <tt>PhoneNumberSourceContact</tt>.
     *
     * @param parentQuery the parent contact query
     * @param contactSource the parent contact source
     * @param contact the protocol contact corresponding to this source contact
     * information about the phone number corresponding to this source contact
     * @param contactDetails the list of <tt>ContactDetail</tt>-s
     * @param detailDisplayName the display name of the phone number detail
     */
    public PhoneNumberSourceContact(ContactQuery parentQuery,
                                    PhoneNumberContactSource contactSource,
                                    Contact contact,
                                    List<ContactDetail> contactDetails,
                                    String detailDisplayName)
    {
        super(  parentQuery,
                contactSource,
                contact.getDisplayName(),
                contactDetails);

        this.contact = contact;
        displayDetails = detailDisplayName;
        setPresenceStatus(contact.getPresenceStatus());
        setImage(contact.getImage());
    }

    /**
     * Returns the display details of this search contact. This could be any
     * important information that should be shown to the user.
     *
     * @return the display details of the search contact
     */
    @Override
    public String getDisplayDetails()
    {
        return displayDetails;
    }

    /**
     * Compares object display names.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;

        if(o == null)
            return false;

        if(o == null || getClass() != o.getClass())
            return false;

        PhoneNumberSourceContact that = (PhoneNumberSourceContact) o;

        String displayName = getDisplayName();
        if(displayName != null ?
                !displayName.equals(that.getDisplayName())
                : that.getDisplayName() != null)
            return false;

        return true;
    }

    /**
     * Returns the protocol contact used.
     * @return the protocol contact used.
     */
    public Contact getContact()
    {
        return contact;
    }
}
