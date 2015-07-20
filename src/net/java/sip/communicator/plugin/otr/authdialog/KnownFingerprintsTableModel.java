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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * A special {@link Panel} for fingerprints display.
 *
 * @author George Politis
 * @author Yana Stamcheva
 */
public class KnownFingerprintsTableModel
    extends AbstractTableModel
    implements ScOtrKeyManagerListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final int CONTACTNAME_INDEX = 0;

    public static final int VERIFIED_INDEX = 1;

    public static final int FINGERPRINT_INDEX = 2;

    public final LinkedHashMap<Contact, List<String>> allContactsFingerprints =
        new LinkedHashMap<Contact, List<String>>();

    public KnownFingerprintsTableModel()
    {
        // Get the protocolproviders
        ServiceReference[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                OtrActivator.bundleContext
                    .getServiceReferences(
                        ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            return;
        }

        if (protocolProviderRefs == null
            || protocolProviderRefs.length < 1)
            return;

        // Populate contacts.
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider
                = (ProtocolProviderService) OtrActivator
                    .bundleContext
                        .getService(protocolProviderRefs[i]);

            Iterator<MetaContact> metaContacts =
                OtrActivator.getContactListService()
                    .findAllMetaContactsForProvider(provider);
            while (metaContacts.hasNext())
            {
                MetaContact metaContact = metaContacts.next();
                Iterator<Contact> contacts = metaContact.getContacts();
                while (contacts.hasNext())
                {
                    Contact contact = contacts.next();
                    allContactsFingerprints.put(
                        contact,
                        OtrActivator.scOtrKeyManager.getAllRemoteFingerprints(
                            contact));
                }
            }
        }
        OtrActivator.scOtrKeyManager.addListener(this);
    }

    /**
     * Implements AbstractTableModel#getColumnName(int).
     */
    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
        case CONTACTNAME_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_CONTACT");
        case VERIFIED_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_VERIFIED_STATUS");
        case FINGERPRINT_INDEX:
            return OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.configform.FINGERPRINT");
        default:
            return null;
        }
    }

    /**
     * Implements AbstractTableModel#getValueAt(int,int).
     */
    public Object getValueAt(int row, int column)
    {
        Contact contact = getContactFromRow(row);
        String fingerprint = getFingerprintFromRow(row);
        switch (column)
        {
        case CONTACTNAME_INDEX:
            return contact.getDisplayName();
        case VERIFIED_INDEX:
            // TODO: Maybe use a CheckBoxColumn?
            return (OtrActivator.scOtrKeyManager
                        .isVerified(contact, fingerprint))
                ? OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_TRUE")
                : OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_FALSE");
        case FINGERPRINT_INDEX:
            return fingerprint;
        default:
            return null;
        }
    }

    Contact getContactFromRow(int row)
    {
        if (row < 0 || row >= getRowCount())
            return null;

        int index = -1;
        Contact contact = null;
        for (Map.Entry<Contact, List<String>> entry :
                allContactsFingerprints.entrySet())
        {
            boolean found = false;
            contact = entry.getKey();
            List<String> fingerprints = entry.getValue();
            for (String f : fingerprints)
            {
                index++;
                if (index == row)
                {
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        return contact;
    }

    String getFingerprintFromRow(int row)
    {
        if (row < 0 || row >= getRowCount())
            return null;

        int index = -1;
        String fingerprint = null;
        for (Map.Entry<Contact, List<String>> entry :
                allContactsFingerprints.entrySet())
        {
            boolean found = false;
            List<String> fingerprints = entry.getValue();
            for (String f : fingerprints)
            {
                index++;
                fingerprint = f;
                if (index == row)
                {
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

    return fingerprint;
    }

    /**
     * Implements AbstractTableModel#getRowCount().
     */
    public int getRowCount()
    {
        int rowCount = 0;
        for (Map.Entry<Contact, List<String>> entry :
                allContactsFingerprints.entrySet())
            rowCount += entry.getValue().size();
        return rowCount;
    }

    /**
     * Implements AbstractTableModel#getColumnCount().
     */
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public void contactVerificationStatusChanged(OtrContact otrContact)
    {
        Contact contact = otrContact.contact;
        allContactsFingerprints.put(
            contact,
            OtrActivator.scOtrKeyManager.getAllRemoteFingerprints(contact));
        this.fireTableDataChanged();
    }
}
