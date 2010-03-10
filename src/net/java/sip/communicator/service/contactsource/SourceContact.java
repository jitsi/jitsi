/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

/**
 * The <tt>SourceContact</tt> is the result contact of a search in the
 * source. It should be identifier by a display name, an image if available
 * and a telephony string, which would allow to call this contact through the
 * preferred telephony provider defined in the <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 */
public interface SourceContact
{
    /**
     * Returns the display name of this search contact. This is a user-friendly
     * name that could be shown in the user interface.
     *
     * @return the display name of this search contact
     */
    public String getDisplayName();

    /**
     * Returns the display details of this search contact. This could be any
     * important information that should be shown to the user.
     *
     * @return the display details of the search contact
     */
    public String getDisplayDetails();

    /**
     * Returns contact details.
     * @return contact details
     */
    public Map<String, String> getContactDetails();

    /**
     * An image (or avatar) corresponding to this search contact. If such is
     * not available this method will return null.
     *
     * @return the byte array of the image or null if no image is available
     */
    public byte[] getImage();

    /**
     * Returns a string, through which this contact could be reached using the
     * preferred telephony provider defined in the <tt>ContactSource</tt>.
     *
     * @return the telephony string corresponding to this contact
     */
    public String getTelephonyString();
}
