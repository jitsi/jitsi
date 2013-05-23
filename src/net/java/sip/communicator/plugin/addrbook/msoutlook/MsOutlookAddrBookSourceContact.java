/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.util.*;

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
     * The <tt>Logger</tt> used by the <tt>MsOutlookAddrBookSourceContact</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MsOutlookAddrBookSourceContact.class);

    /**
     * Boolean used to temporarily lock the access to a single modification
     * source (jitsi or contact).  i.e. it can be useful if Jitsi modifies a
     * batch of details and do not want to receive contact update notification
     * which can produce concurrent changes of the details.
     */
    private Boolean locked = Boolean.FALSE;

    /**
     * Initializes a new MsOutlookAddrBookSourceContact instance.
     *
     * @param contactSource The ContactSourceService which is creating the new
     * instance.
     * @param id The outlook entry identifier for contacts.
     * @param displayName The display name of the new instance.
     * @param organization The organization name of the new instance.
     * @param contactDetails The ContactDetails of the new instance.
     */
    public MsOutlookAddrBookSourceContact(
            ContactSourceService contactSource,
            String id,
            String displayName,
            String organization,
            List<ContactDetail> contactDetails)
    {
        super(contactSource, displayName, contactDetails);

        this.setData(SourceContact.DATA_ID, id);
        this.setDisplayDetails(organization);
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
        synchronized(this)
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
        synchronized(this)
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
        synchronized(this)
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
        synchronized(this)
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
     * Sets the display name for this contact.
     *
     * @param displayName The new display name for this contact.
     */
    @Override
    public void setDisplayName(String displayName)
    {
        if(displayName != null && !displayName.equals(this.getDisplayName()))
        {
            // Be sure that the new determined display named is saved under all
            // the requireed properties.
            long[] displayNamePropIdList =
            {
                0x3001, // PR_DISPLAY_NAME
                0x0037, // PR_SUBJECT
                0x803F, // Do not know, but set by the MFCMAPI application.
                0x0E1D // PR_NORMALIZED_SUBJECT
            };

            for(int i = 0; i < displayNamePropIdList.length; ++i)
            {
                MsOutlookAddrBookContactQuery.IMAPIProp_SetPropString(
                        displayNamePropIdList[i],
                        displayName,
                        this.getId());
            }
        }

        super.setDisplayName(displayName);
    }

    /**
     * Function called by the native part (msoutlook) when this contact has been
     * updated.
     */
    public void updated()
    {
        // Synchronize before the GetProps in order to let other operation (i.e.
        // save) to write/change all desired values (and not override new saved
        // values iwth old ones).
        synchronized(this)
        {
            waitUnlock();

            Object[] props = null;
            try
            {
                props = MsOutlookAddrBookContactQuery.IMAPIProp_GetProps(
                        this.getId(),
                        MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS,
                        MsOutlookAddrBookContactQuery.MAPI_UNICODE);
            }
            catch (MsOutlookMAPIHResultException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                            MsOutlookAddrBookContactQuery.class.getSimpleName()
                            + "#IMAPIProp_GetProps(long, long[], long)",
                            e);
                }
            }

            // let's update the the details
            List<ContactDetail> contactDetails
                = MsOutlookAddrBookContactQuery.getContactDetails(props);
            this.setDetails(contactDetails);

            String displayName
                = MsOutlookAddrBookContactQuery.getDisplayName(props);
            this.setDisplayName(displayName);
            String organization
                = MsOutlookAddrBookContactQuery.getOrganization(props);
            this.setDisplayDetails(organization);
        }
    }

    /**
     * Locks this object before adding or removing several contact details.
     */
    public void lock()
    {
        synchronized(this)
        {
            locked = Boolean.TRUE;
        }
    }

    /**
     * Unlocks this object before after or removing several contact details.
     */
    public void unlock()
    {
        synchronized(this)
        {
            locked = Boolean.FALSE;
            notify();
        }
    }

    /**
     * Waits to be unlocked. This object must be synchronized before calling
     * this function.
     */
    private void waitUnlock()
    {
        boolean continueToWait = this.locked;

        while(continueToWait)
        {
            try
            {
                wait();
                continueToWait = false;
            }
            catch(InterruptedException ie)
            {
                // Nothing to do, we will wait until the notify.
            }
        }
    }

    /**
     * Returns the index of this source contact in its parent.
     *
     * @return the index of this source contact in its parent
     */
    @Override
    public int getIndex()
    {
        return -1;
    }
}
