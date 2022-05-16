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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>InviteUIContact</tt> is an <tt>UIContact</tt> used in invite dialogs
 * to represent the selected contacts the invite operation.
 *
 * @author Yana Stamcheva
 */
public class InviteUIContact
    extends GenericUIContactImpl
{
    /**
     * The source <tt>UIContact</tt> from which this contact is created. This
     * contact can be seen as a copy of the source contact.
     */
    private final UIContact sourceUIContact;

    /**
     * The backup protocol provider to be used for contact operations, if no
     * other protocol provider has been specified.
     */
    private final ProtocolProviderService backupProvider;

    /**
     * Creates an instance of <tt>InviteUIContact</tt>.
     *
     * @param uiContact the source <tt>UIContact</tt>.
     * @param protocolProvider the backup protocol provider to use if no other
     * protocol provider has been specified in the source ui contact
     */
    public InviteUIContact( UIContact uiContact,
                            ProtocolProviderService protocolProvider)
    {
        super(uiContact.getDescriptor(), null, uiContact.getDisplayName());

        setDisplayDetails(uiContact.getDisplayDetails());

        sourceUIContact = uiContact;
        backupProvider = protocolProvider;
    }

    /**
     * Returns a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> contactDetails
            = sourceUIContact.getContactDetailsForOperationSet(opSetClass);

        if (contactDetails == null)
            return null;

        if (backupProvider == null)
            return contactDetails;

        Iterator<UIContactDetail> contactDetailsIter
            = contactDetails.iterator();

        while (contactDetailsIter.hasNext())
        {
            UIContactDetail contactDetail = contactDetailsIter.next();

            if (contactDetail
                    .getPreferredProtocolProvider(opSetClass) == null)
            {
                contactDetail.addPreferredProtocolProvider( opSetClass,
                                                            backupProvider);
            }

            if (contactDetail
                    .getPreferredProtocol(opSetClass) == null)
            {
                contactDetail.addPreferredProtocol(
                    opSetClass,
                    backupProvider.getProtocolName());
            }
        }

        return contactDetails;
    }

    /**
     * Returns a list of all contained <tt>UIContactDetail</tt>s.
     *
     * @return a list of all contained <tt>UIContactDetail</tt>s
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        return sourceUIContact.getContactDetails();
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    @Override
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        return sourceUIContact.getDefaultContactDetail(opSetClass);
    }

    /**
     * Returns the avatar of this <tt>UIContact</tt>.
     *
     * @param isSelected indicates if the avatar is selected
     * @param width avatar preferred width
     * @param height avatar preferred height
     */
    @Override
    public ImageIcon getScaledAvatar(boolean isSelected, int width, int height)
    {
        if (sourceUIContact instanceof UIContactImpl)
            return ((UIContactImpl) sourceUIContact)
                .getScaledAvatar(isSelected, width, height);

        return null;
    }

    /**
     * Returns the status icon of this contact.
     *
     * @return an <tt>ImageIcon</tt> representing the status of this contact
     */
    @Override
    public ImageIcon getStatusIcon()
    {
        if (sourceUIContact instanceof UIContactImpl)
            return ((UIContactImpl) sourceUIContact).getStatusIcon();

        return null;
    }
}
