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
package net.java.sip.communicator.plugin.addrbook.macosx;

import java.util.*;
//import java.util.regex.*;

//import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactsource.ContactDetail.*;
//import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Our editable source contact, we store changes in the addressbook.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookSourceContact
    extends GenericSourceContact
    implements EditableSourceContact
{
    /**
     * The <tt>Logger</tt> used by the <tt>MacOSXAddrBookSourceContact</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MacOSXAddrBookSourceContact.class);

    /**
     * Boolean used to temporarily lock the access to a single modification
     * source (jitsi or contact).  i.e. it can be useful if Jitsi modifies a
     * batch of details and do not want to receive contact update notification
     * which can produce concurrent changes of the details.
     */
    private Boolean locked = Boolean.FALSE;

    /**
     * Initializes a new <tt>AddrBookSourceContact</tt> instance.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is creating
     * the new instance
     * @param displayName the display name of the new instance
     * @param contactDetails the <tt>ContactDetail</tt>s of the new instance
     */
    public MacOSXAddrBookSourceContact(
            ContactSourceService contactSource,
            String displayName,
            List<ContactDetail> contactDetails)
    {
        super(contactSource, displayName, contactDetails);

        // let's save the parent so we can reuse it later when editing
        // the detail
        for(ContactDetail cd : contactDetails)
        {
            if(cd instanceof MacOSXAddrBookContactDetail)
            {
                ((MacOSXAddrBookContactDetail)cd).setSourceContact(this);
            }
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
            String id = (String)getData(SourceContact.DATA_ID);

            if(id == null)
            {
                logger.warn("No id or wrong ContactDetail " + detail);
                return;
            }

            String subProperty = null;
            int property = MacOSXAddrBookContactQuery.getProperty(
                    detail.getCategory(),
                    detail.getSubCategories());

            if(MacOSXAddrBookContactDetail.isMultiline(detail.getCategory()))
            {
                if(detail instanceof MacOSXAddrBookContactDetail)
                {
                    subProperty = ((MacOSXAddrBookContactDetail)detail)
                        .getSubPropertyLabel();
                }

                if(subProperty == null)
                {
                    if(property == MacOSXAddrBookContactQuery
                            .kABAddressProperty)
                    {
                        if(detail.containsSubCategory(SubCategory.Home))
                            subProperty = MacOSXAddrBookContactQuery
                                .kABAddressHomeLabel();
                        else
                            subProperty = MacOSXAddrBookContactQuery
                                .kABAddressWorkLabel();
                    }
                    else if(property == MacOSXAddrBookContactQuery
                            .kABAIMInstantProperty
                            || property == MacOSXAddrBookContactQuery
                                .kABICQInstantProperty
                            || property == MacOSXAddrBookContactQuery
                                .kABJabberInstantProperty
                            || property == MacOSXAddrBookContactQuery
                                .kABMSNInstantProperty
                            || property == MacOSXAddrBookContactQuery
                                .kABYahooInstantProperty)
                    {
                        subProperty = MacOSXAddrBookContactQuery
                            .kABAddressWorkLabel();



                    }
                }

                List<String> values
                    = getValues(detail, property, subProperty, true);

                MacOSXAddrBookContactQuery.setProperty(
                        id,
                        MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                            property],
                        subProperty,
                        values.toArray(new Object[values.size()]));
            }
            else
            {
                 MacOSXAddrBookContactQuery.setProperty(
                         id,
                         MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                             property],
                         null,
                         detail.getDetail());
            }

            // make sure we add AddressBookContactDetail
            Collection<SubCategory> subCategories
                = detail.getSubCategories();

            MacOSXAddrBookContactDetail contactDetail
                = new MacOSXAddrBookContactDetail(
                        property,
                        detail.getDetail(),
                        detail.getCategory(),
                        subCategories.toArray(
                            new SubCategory[
                            subCategories.size()]),
                        subProperty,
                        id);
            contactDetail.setSourceContact(this);

            // Add the detail at the right index : group multiline properties
            // together , such as home/work address fields.
            boolean added = false;
            int index = 0;
            for(ContactDetail cd: contactDetails)
            {
                if(cd instanceof MacOSXAddrBookContactDetail)
                {
                    MacOSXAddrBookContactDetail macOSXcd
                        = (MacOSXAddrBookContactDetail) cd;
                    if(!added
                            && contactDetail.getProperty()
                                == macOSXcd.getProperty()
                            && (contactDetail.getSubPropertyLabel() == null
                                || contactDetail.getSubPropertyLabel().equals(
                                    macOSXcd.getSubPropertyLabel())))
                    {
                        added = true;
                    }
                }
                if(!added)
                    ++index;
            }

            contactDetails.add(index, contactDetail);
        }
    }

    /**
     * Returns the list of values that will be saved.
     * @param detail the current modified detail
     * @param property the property we change
     * @param subProperty the subproperty that is changed
     * @param addDetail should we add <tt>detail</tt> to the list of values.
     * @return the list of values to be saved.
     */
    private List<String> getValues(ContactDetail detail,
                                   int property,
                                   String subProperty,
                                   boolean addDetail)
    {
        // first add existing one
        List<String> values = new ArrayList<String>();

        List<ContactDetail> details =
            getContactDetails(detail.getCategory());

        boolean isIM =
            (property == MacOSXAddrBookContactQuery.kABICQInstantProperty
             || property == MacOSXAddrBookContactQuery.kABAIMInstantProperty
             || property == MacOSXAddrBookContactQuery.kABYahooInstantProperty
             || property == MacOSXAddrBookContactQuery.kABMSNInstantProperty
             || property == MacOSXAddrBookContactQuery.kABJabberInstantProperty
            );

        boolean isAddress
            = property == MacOSXAddrBookContactQuery.kABAddressProperty;
        boolean isHomeAddress =
            detail.containsSubCategory(SubCategory.Home);
        int lastHomeIndex = 0;
        int lastWorkIndex = 0;

        for(ContactDetail cd : details)
        {
            // if the detail exists do not added, in case of add there is
            // sense the detail to be added twice. In case of remove
            // we miss the detail
            if(cd.equals(detail))
                continue;

            String det = cd.getDetail();

            for(SubCategory sub : cd.getSubCategories())
            {
                // if its an im property check also if the detail
                // is the same subcategory (which is icq, yahoo, ...)
                if(isIM && !detail.getSubCategories().contains(sub))
                    continue;

                String label =
                    MacOSXAddrBookContactQuery.
                        getLabel(property, sub, subProperty);
                if(label != null)
                {
                    values.add(det);
                    values.add(label);

                    // For an address adds a third item for the tuple:
                    // value, label, sub-property label.
                    if(isAddress
                            && cd instanceof MacOSXAddrBookContactDetail)
                    {
                        String subPropertyLabel
                            = ((MacOSXAddrBookContactDetail) cd)
                                .getSubPropertyLabel();
                        values.add(subPropertyLabel);

                        if(subPropertyLabel.equals(
                                    MacOSXAddrBookContactQuery
                                        .kABAddressHomeLabel()))
                        {
                            lastHomeIndex = values.size();
                        }
                        else if(subPropertyLabel.equals(
                                    MacOSXAddrBookContactQuery
                                        .kABAddressWorkLabel()))
                        {
                            lastWorkIndex = values.size();
                        }
                    }
                }
            }
        }

        if(addDetail)
        {
            // now the new value to add
            for(SubCategory sub : detail.getSubCategories())
            {
                String label =
                    MacOSXAddrBookContactQuery.
                        getLabel(property, sub, subProperty);
                if(label != null)
                {
                    // For an address adds a third item for the tuple:
                    // value, label, sub-property label.
                    if(isAddress)
                    {
                        String subPropertyLabel = "";
                        int index = values.size();
                        if(isHomeAddress)
                        {
                            subPropertyLabel
                                = MacOSXAddrBookContactQuery
                                    .kABAddressHomeLabel();
                            index = lastHomeIndex;
                            if(lastWorkIndex > lastHomeIndex)
                            {
                                lastWorkIndex += 3;
                            }
                            lastHomeIndex += 3;
                        }
                        else
                        {
                            subPropertyLabel
                                = MacOSXAddrBookContactQuery
                                    .kABAddressWorkLabel();
                            index = lastWorkIndex;
                            if(lastHomeIndex > lastWorkIndex)
                            {
                                lastHomeIndex += 3;
                            }
                            lastWorkIndex += 3;
                        }
                        values.add(index, detail.getDetail());
                        values.add(index + 1, label);
                        values.add(index + 2, subPropertyLabel);
                    }
                    else
                    {
                        values.add(detail.getDetail());
                        values.add(label);
                    }
                }
                else
                    logger.warn("Missing label fo prop:" + property
                        + " and sub:" + sub);
            }
        }

        return values;
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
            //remove the detail from the addressbook
            String id = (String)getData(SourceContact.DATA_ID);
            if(id != null && detail instanceof MacOSXAddrBookContactDetail)
            {
                if(MacOSXAddrBookContactDetail.isMultiline(
                            detail.getCategory()))
                {
                    String subProperty = null;

                    if(detail instanceof MacOSXAddrBookContactDetail)
                    {
                        subProperty = ((MacOSXAddrBookContactDetail)detail)
                            .getSubPropertyLabel();
                    }

                    List<String> values =
                        getValues(
                            detail,
                            ((MacOSXAddrBookContactDetail)detail)
                                .getProperty(),
                            subProperty,
                            false);

                    MacOSXAddrBookContactQuery.setProperty(
                        id,
                        MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                            ((MacOSXAddrBookContactDetail) detail)
                                .getProperty()],
                        subProperty,
                        values.toArray(new Object[values.size()]));
                }
                else
                    MacOSXAddrBookContactQuery.removeProperty(
                            id,
                            MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                                ((MacOSXAddrBookContactDetail) detail)
                                .getProperty()]);
            }
            else
                logger.warn("No id or wrong ContactDetail " + detail);

            contactDetails.remove(detail);
        }
    }

    /**
     * Changes the details list with the supplied one.
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
     * Function called by the native part (contact) when this contact has been
     * updated.
     */
    public void updated()
    {
        synchronized(this)
        {
            waitUnlock();

            String id = (String)getData(SourceContact.DATA_ID);
            ContactSourceService sourceService = getContactSource();
            if(id != null
                    && sourceService instanceof
                        MacOSXAddrBookContactSourceService)
            {
                MacOSXAddrBookContactSourceService macOSXSourceService
                    = (MacOSXAddrBookContactSourceService) sourceService;
                MacOSXAddrBookContactQuery macOSXContactQuery
                    = macOSXSourceService.getLatestQuery();
                if(macOSXContactQuery != null)
                {
                    long contactPointer
                        = MacOSXAddrBookContactQuery.getContactPointer(id);
                    macOSXContactQuery.updated(contactPointer);
                }
            }
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
            // Once we have set all the details, then notify the UI that the
            // contact has been updated.
            this.updated();
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
