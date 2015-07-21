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

import net.java.sip.communicator.service.contactsource.*;

/**
 * The editable detail, change get changed and in addressbook.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactDetail
extends EditableContactDetail
{
    /**
     * The property index for this detail.
     */
    private final int property;

    /**
     * The id of the detail.
     */
    private String id;

    private String subPropertyLabel;

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
     * @param id The id of the detail.
     */
    public MacOSXAddrBookContactDetail(
            int property,
            String contactDetailValue,
            Category category,
            SubCategory[] subCategories,
            String subPropertyLabel,
            String id)
    {
        super(contactDetailValue, category, subCategories);
        this.property = property;
        this.subPropertyLabel = subPropertyLabel;
        this.id = id;
    }

    /**
     * Whether the value for the category are multiline.
     * @param category
     * @return
     */
    public static boolean isMultiline(Category category)
    {
        switch(category)
        {
            case Personal:
                return false;
            case Organization:
                return false;
            case Email:
                return true;
            case InstantMessaging:
                return true;
            case Phone:
                return true;
            case Address:
                return true;
            default:
                return false;
        }
    }

    /**
     * Sets the given detail value.
     *
     * @param value the new value of the detail
     */
    @Override
    public void setDetail(String value)
    {
        //let's save in addressbook
        if(isMultiline(getCategory()))
        {
            // get others
            EditableSourceContact sourceContact = getSourceContact();
            if(sourceContact != null
                    && sourceContact instanceof MacOSXAddrBookSourceContact)
            {
                List<ContactDetail> details =
                    ((MacOSXAddrBookSourceContact) sourceContact)
                        .getContactDetails(getCategory());

                boolean isAddress =
                    property == MacOSXAddrBookContactQuery.kABAddressProperty;
                boolean isHomeAddress = containsSubCategory(SubCategory.Home);
                // For an address, we must check that the current detail is the
                // modified one. For all other properties than address, this
                // boolean must always be true.
                boolean isModifiedAddressOrGenericDetail;

                // first add existing one
                List<String> values = new ArrayList<String>();
                for(ContactDetail cd : details)
                {
                    isModifiedAddressOrGenericDetail = true;
                    if(isAddress)
                    {
                        // lets check home and work details
                        if((isHomeAddress
                                    && !cd.containsSubCategory(SubCategory.Home)
                           )
                                || (!isHomeAddress
                                    && !cd.containsSubCategory(SubCategory.Work)
                           ))
                        {
                            isModifiedAddressOrGenericDetail = false;
                        }
                    }

                    String det = cd.getDetail();

                    for(SubCategory sub : cd.getSubCategories())
                    {
                        String label
                            = MacOSXAddrBookContactQuery.
                                getLabel(property, sub, subPropertyLabel);

                        if(label != null)
                        {
                            if(getSubCategories().contains(sub)
                                    && isModifiedAddressOrGenericDetail)
                                values.add(value);
                            else
                                values.add(det);

                            values.add(label);

                            // For an address adds a third item for the tuple:
                            // value, label, sub-property label.
                            if(isAddress
                                    && cd instanceof MacOSXAddrBookContactDetail
                              )
                            {
                                values.add(
                                        ((MacOSXAddrBookContactDetail) cd)
                                        .getSubPropertyLabel());
                            }
                        }
                    }
                }

                // now the real edit
                MacOSXAddrBookContactQuery.setProperty(
                        id,
                        MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                            property],
                        subPropertyLabel,
                        values.toArray(new Object[values.size()]));
            }
        }
        else
        {
            MacOSXAddrBookContactQuery.setProperty(
                    id,
                    MacOSXAddrBookContactQuery.ABPERSON_PROPERTIES[
                        property],
                    null,
                    value);
        }

        super.setDetail(value);
    }

    /**
     * Returns the sub property.
     * @return
     */
    public String getSubPropertyLabel()
    {
        return subPropertyLabel;
    }

    /**
     * Returns the property index for this detail.
     *
     * @return The property index for this detail.
     */
    public final int getProperty()
    {
        return this.property;
    }
}
