/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>EditableContactDetail</tt> is a <tt>ContactDetail</tt> that allows 
 * editing.
 *
 * @see ContactDetail
 *
 * @author Yana Stamcheva
 */
public class EditableContactDetail
    extends ContactDetail
{
    /**
     * Creates a <tt>ContactDetail</tt> by specifying the contact address,
     * corresponding to this detail.
     * @param contactDetailValue the contact detail value corresponding to this
     * detail
     */
    public EditableContactDetail(String contactDetailValue)
    {
        super(contactDetailValue);
    }

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param category
     * @param subCategories the set of sub categories with which the new
     * <tt>ContactDetail</tt> instance is to be labeled.
     */
    public EditableContactDetail(
            String contactDetailValue,
            ContactDetail.Category category,
            ContactDetail.SubCategory[] subCategories)
    {
        super(contactDetailValue, category, subCategories);
    }

    /**
     * Sets the given detail value.
     *
     * @param value the new value of the detail
     */
    public void setDetail(String value)
    {
        contactDetailValue = value;
    }
}
