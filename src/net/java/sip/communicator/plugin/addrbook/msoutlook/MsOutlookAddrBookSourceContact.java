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
     * The list of Outlook entry IDs we have already seen for this contact.
     */
    private Vector<String> ids = new Vector<String>(1, 1);

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

        this.ids.add(id);
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
            setDisplayPostalAddress();

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
            this.save();
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

    /**
     * Tells if the id given in parameters corresponds to this contact.
     *
     * @param id The id to compare with.
     * @param level 0 to only look at cached ids. 1 to only look at outlook
     * database ids.
     *
     * @return True if the id given in parameters corresponds to this contact.
     * False otherwise.
     */
    public boolean match(String id, int level)
    {
        boolean res = false;
        switch(level)
        {
            case 0:
                res = this.ids.contains(id);
                break;
            case 1:
                String localId = this.getId();
                res =
                    MsOutlookAddrBookContactQuery.compareEntryIds(id, localId);
                if(res && !this.ids.contains(id))
                {
                    this.ids.add(id);
                }
                break;
        }
        return res;
    }

    /**
     * Generates and stores the string representation of the home and work
     * postall addresses.
     */
    private void setDisplayPostalAddress()
    {
        synchronized(this)
        {
            MsOutlookAddrBookContactDetail detail;

            // Setting the display work postal address.
            boolean firstLineCR = false;
            boolean secondLineCR = false;
            String workAddress = "";
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_BUSINESS_ADDRESS_STREET]);
            if(detail != null)
            {
                workAddress += detail.getDetail();
                firstLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_BUSINESS_ADDRESS_CITY]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    workAddress += "\r";
                    firstLineCR = false;
                }
                workAddress += detail.getDetail();
                workAddress += " ";
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.
                        PR_BUSINESS_ADDRESS_STATE_OR_PROVINCE]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    workAddress += "\r";
                    firstLineCR = false;
                }
                workAddress += detail.getDetail();
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.
                        PR_BUSINESS_ADDRESS_POSTAL_CODE]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    workAddress += "\r";
                    firstLineCR = false;
                }
                workAddress += detail.getDetail();
                workAddress += " ";
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_BUSINESS_ADDRESS_COUNTRY]);
            if(detail != null)
            {
                if(secondLineCR)
                {
                    workAddress += "\r";
                    secondLineCR = false;
                }
                workAddress += detail.getDetail();
            }

            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.dispidWorkAddress]);
            if(detail != null)
            {
                // set address.
                detail.setDetail(workAddress);
            }
            else if(workAddress.length() > 0)
            {
                detail = new MsOutlookAddrBookContactDetail(
                        workAddress,
                        MsOutlookAddrBookContactQuery.getCategory(
                            MsOutlookAddrBookContactQuery.dispidWorkAddress),
                        MsOutlookAddrBookContactQuery.getSubCategories(
                            MsOutlookAddrBookContactQuery.dispidWorkAddress),
                        MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                            MsOutlookAddrBookContactQuery.dispidWorkAddress]);
                this.contactDetails.add(detail);
            }

            // Setting the display home postal address.
            firstLineCR = false;
            secondLineCR = false;
            String homeAddress = "";
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_HOME_ADDRESS_STREET]);
            if(detail != null)
            {
                homeAddress += detail.getDetail();
                firstLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_HOME_ADDRESS_CITY]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    homeAddress += "\r";
                    firstLineCR = false;
                }
                homeAddress += detail.getDetail();
                homeAddress += " ";
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.
                        PR_HOME_ADDRESS_STATE_OR_PROVINCE]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    homeAddress += "\r";
                    firstLineCR = false;
                }
                homeAddress += detail.getDetail();
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_HOME_ADDRESS_POSTAL_CODE]);
            if(detail != null)
            {
                if(firstLineCR)
                {
                    homeAddress += "\r";
                    firstLineCR = false;
                }
                homeAddress += detail.getDetail();
                homeAddress += " ";
                secondLineCR = true;
            }
            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.PR_HOME_ADDRESS_COUNTRY]);
            if(detail != null)
            {
                if(secondLineCR)
                {
                    homeAddress += "\r";
                    secondLineCR = false;
                }
                homeAddress += detail.getDetail();
            }

            detail = findDetail(
                    MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                    MsOutlookAddrBookContactQuery.dispidHomeAddress]);
            if(detail != null)
            {
                // set address.
                detail.setDetail(homeAddress);
            }
            else if(homeAddress.length() > 0)
            {
                detail = new MsOutlookAddrBookContactDetail(
                        homeAddress,
                        MsOutlookAddrBookContactQuery.getCategory(
                            MsOutlookAddrBookContactQuery.dispidHomeAddress),
                        MsOutlookAddrBookContactQuery.getSubCategories(
                            MsOutlookAddrBookContactQuery.dispidHomeAddress),
                        MsOutlookAddrBookContactQuery.MAPI_MAILUSER_PROP_IDS[
                            MsOutlookAddrBookContactQuery.dispidHomeAddress]);
                this.contactDetails.add(detail);
            }
        }
    }


    /**
     * Finds the detail corresponding to the given property id.
     *
     * @param detailPropId The detail identifier.
     *
     * @return The detail corresponding to the given property id. Null if not
     * found.
     */
    private MsOutlookAddrBookContactDetail findDetail(long detailPropId)
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
                        if(propId.longValue() == detailPropId)
                        {
                            return outlookContactDetail;
                        }
                    }
                }
            }
        }
        return null;
    }
}
