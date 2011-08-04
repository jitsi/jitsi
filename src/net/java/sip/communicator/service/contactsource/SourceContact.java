/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

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
     * Returns the parent <tt>ContactSourceService</tt> from which this contact
     * came from.
     * @return the parent <tt>ContactSourceService</tt> from which this contact
     * came from
     */
    public ContactSourceService getContactSource();

    /**
     * Returns the display details of this search contact. This could be any
     * important information that should be shown to the user.
     *
     * @return the display details of the search contact
     */
    public String getDisplayDetails();

    /**
     * Returns a list of available contact details.
     * @return a list of available contact details
     */
    public List<ContactDetail> getContactDetails();

    /**
     * Returns a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    public List<ContactDetail> getContactDetails(
                                    Class<? extends OperationSet> operationSet);

    /**
     * Returns a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category.
     * @param category the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category
     * 
     * @throws OperationNotSupportedException if categories aren't supported
     * for call history records
     */
    public List<ContactDetail> getContactDetails(String category)
        throws OperationNotSupportedException;

    /**
     * Returns the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class, for which we would
     * like to obtain a <tt>ContactDetail</tt>
     * @return the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class
     */
    public ContactDetail getPreferredContactDetail(
                                    Class<? extends OperationSet> operationSet);

    /**
     * An image (or avatar) corresponding to this search contact. If such is
     * not available this method will return null.
     *
     * @return the byte array of the image or null if no image is available
     */
    public byte[] getImage();
}