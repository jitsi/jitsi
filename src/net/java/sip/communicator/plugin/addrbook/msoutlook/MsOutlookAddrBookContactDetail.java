/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import net.java.sip.communicator.service.contactsource.*;

import java.util.*;

/**
 * Implements a custom <tt>ContactDetail</tt> for the Address Book of Microsoft
 * Outlook.
 *
 * @author Vincent Lucas
 */
public class MsOutlookAddrBookContactDetail
    extends EditableContactDetail
{
    /**
     * The list of codes used by outlook to identify the property corresponding
     * to this contact detail.
     */
    private Vector<Long> outlookPropId;

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to represent a
     * specific contact address and which is to be optionally labeled with a
     * specific set of labels.
     *
     * @param contactDetailValue the contact detail value to be represented by
     * the new <tt>ContactDetail</tt> instance
     * @param category The category of this contact detail.
     * @param subCategories the set of sub categories with which the new
     * <tt>ContactDetail</tt> instance is to be labeled.
     * @param outlookPropId The identifier of the outlook property used to
     * get/set this contact detail.
     */
    public MsOutlookAddrBookContactDetail(
            String contactDetailValue,
            Category category,
            SubCategory[] subCategories,
            long outlookPropId)
    {
        super(contactDetailValue, category, subCategories);

        this.outlookPropId = new Vector<Long>(1, 1);
        this.outlookPropId.add(new Long(outlookPropId));
    }

    /**
     * If the given contact detail is similar to the current one (same category
     * and same detail value), then return true. False otherwise.
     *
     * @param contactDetail The contact detail to compare with.
     *
     * @return True, if the given contact detail is similar to the current one
     * (same category and same detail value). False otherwise.
     */
    public boolean match(ContactDetail contactDetail)
    {
        return (contactDetail != null
                && this.getCategory() == contactDetail.getCategory()
                && this.getDetail().equals(contactDetail.getDetail()));
    }

    /**
     * Returns the list of outlook properties corresponding to this contact
     * detail.
     *
     * @return The list of outlook properties corresponding to this contact
     * detail.
     */
    public Vector<Long> getOutlookPropId()
    {
        return this.outlookPropId;
    }

    /**
     * Sets the given detail value.
     *
     * @param value the new value of the detail
     */
    @Override
    public void setDetail(String value)
    {
        super.setDetail(value);

        EditableSourceContact sourceContact = this.getSourceContact();
        if(sourceContact != null
                && sourceContact instanceof MsOutlookAddrBookSourceContact)
        {
            ((MsOutlookAddrBookSourceContact) sourceContact).save();
        }
    }
}
