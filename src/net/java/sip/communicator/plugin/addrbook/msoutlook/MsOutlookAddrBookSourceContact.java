/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements a custom <tt>SourceContact</tt> for the Address Book of Microsoft
 * Outlook.
 *
 * @author Vincent Lucas
 */
public class MsOutlookAddrBookSourceContact
    extends GenericSourceContact
    implements EditableSourceContact
{
    /**
     * Initializes a new MsOutlookAddrBookSourceContact instance.
     *
     * @param contactSource The ContactSourceService which is creating the new
     * instance.
     * @param id The outlook entry identifier for contacts.
     * @param displayName The display name of the new instance.
     * @param contactDetails The ContactDetails of the new instance.
     */
    public MsOutlookAddrBookSourceContact(
            ContactSourceService contactSource,
            String id,
            String displayName,
            List<ContactDetail> contactDetails)
    {
        super(contactSource, displayName, contactDetails);

        this.setData(SourceContact.DATA_ID, id);
    }

    /**
     * Returns the string identifier for this source contact.
     *
     * @return The string identifier for this source contact.
     */
    public String getId()
    {
        return (String) this.getData(SourceContact.DATA_ID);
    }

    /**
     * Changes the details list with the supplied one. Called when the outlook
     * database for this source contact has been updated.
     *
     * @param details the details.
     */
    public void setDetails(List<ContactDetail> details)
    {
        synchronized(this.contactDetails)
        {
            contactDetails.clear();
            contactDetails.addAll(details);
        }
    }

    /**
     * Saves all the properties from this source contact into the outlook
     * database.
     */
    public void save()
    {
        synchronized(this.contactDetails)
        {
            MsOutlookAddrBookContactDetail outlookContactDetail;

            for(ContactDetail contactDetail: this.contactDetails)
            {
                if(contactDetail instanceof MsOutlookAddrBookContactDetail)
                {
                    outlookContactDetail
                        = (MsOutlookAddrBookContactDetail) contactDetail;
                    for(Long propId: outlookContactDetail.getOutlookPropId())
                    {
                        MsOutlookAddrBookContactQuery.IMAPIProp_SetPropString(
                                propId.longValue(),
                                contactDetail.getDetail(),
                                this.getId());
                    }
                }
            }
        }
    }

    /**
     * Removes the given <tt>ContactDetail</tt> from the list of details for
     * this <tt>SourceContact</tt>.
     *
     * @param detail the <tt>ContactDetail</tt> to remove
     */
    public void removeContactDetail(ContactDetail detail)
    {
        synchronized(this.contactDetails)
        {
            int i = 0;
            while(i < this.contactDetails.size())
            {
                MsOutlookAddrBookContactDetail contactDetail
                    = ((MsOutlookAddrBookContactDetail)
                            this.contactDetails.get(i));
                if(contactDetail.match(detail))
                {
                    this.removeProperty(contactDetail);
                    this.contactDetails.remove(i);
                }
                else
                {
                    ++i;
                }
            }
        }
    }

    /**
     * Removes the contact detail from the outlook database.
     *
     * @param contactDetail The contact detail to remove.
     */
    public void removeProperty(
            final MsOutlookAddrBookContactDetail contactDetail)
    {
        for(Long propId: contactDetail.getOutlookPropId())
        {
            MsOutlookAddrBookContactQuery.IMAPIProp_DeleteProp(
                    propId.longValue(),
                    this.getId());
        }
    }

    /**
     * Adds a contact detail to the list of contact details.
     *
     * @param detail the <tt>ContactDetail</tt> to add
     */
    public void addContactDetail(ContactDetail detail)
    {
        synchronized(this.contactDetails)
        {
            MsOutlookAddrBookContactDetail addDetail;
            if(!(detail instanceof MsOutlookAddrBookContactDetail))
            {
                long property = MsOutlookAddrBookContactQuery.getProperty(
                        detail.getCategory(),
                        detail.getSubCategories());
                Collection<ContactDetail.SubCategory> subCategories
                        = detail.getSubCategories();
                addDetail = new MsOutlookAddrBookContactDetail(
                        detail.getDetail(),
                        detail.getCategory(),
                        subCategories.toArray(
                            new ContactDetail.SubCategory[
                            subCategories.size()]),
                        property);
            }
            else
            {
                addDetail = (MsOutlookAddrBookContactDetail) detail;
            }

            // Checks if this property already exists.
            for(int i = 0; i < this.contactDetails.size(); ++ i)
            {
                MsOutlookAddrBookContactDetail contactDetail
                    = ((MsOutlookAddrBookContactDetail)
                            this.contactDetails.get(i));
                if(contactDetail.match(addDetail))
                {
                    return;
                }
            }
            this.contactDetails.add(addDetail);
            this.save();
        }
    }

    /**
     * Returns the index of this source contact in its parent.
     *
     * @return the index of this source contact in its parent
     */
    public int getIndex()
    {
        return -1;
    }
}
