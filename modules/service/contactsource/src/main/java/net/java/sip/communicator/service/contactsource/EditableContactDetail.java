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
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>EditableContactDetail</tt> is a <tt>ContactDetail</tt> that allows
 * editing.
 *
 * @see ContactDetail
 *
 * @author Yana Stamcheva
 */
public abstract class EditableContactDetail
    extends ContactDetail
{
    /**
     * The source contact which contains this contact detail.
     */
    private EditableSourceContact sourceContact = null;

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
     * Returns the source contact that contains this contact detail.
     *
     * @return The source contact that contains this contact detail.
     */
    public EditableSourceContact getSourceContact()
    {
        return this.sourceContact;
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

    /**
     * Sets the source contact that contains this contact detail.
     *
     * @param sourceContact The source contact that contains this contact
     * detail.
     */
    public void setSourceContact(EditableSourceContact sourceContact)
    {
        this.sourceContact = sourceContact;
    }
}
