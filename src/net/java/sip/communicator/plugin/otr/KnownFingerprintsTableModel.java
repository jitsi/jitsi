/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.util.*;

import javax.swing.table.*;

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
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    public static final int CONTACTNAME_INDEX = 0;

    public static final int VERIFIED_INDEX = 1;

    public static final int FINGERPRINT_INDEX = 2;

    public final java.util.List<Contact> allContacts = new Vector<Contact>();

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

        // Get the metacontactlist service.
        ServiceReference ref =
            OtrActivator.bundleContext
                .getServiceReference(MetaContactListService.class
                    .getName());

        MetaContactListService service
            = (MetaContactListService) OtrActivator
                .bundleContext.getService(ref);

        // Populate contacts.
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider
                = (ProtocolProviderService) OtrActivator
                    .bundleContext
                        .getService(protocolProviderRefs[i]);

            Iterator<MetaContact> metaContacts =
                service.findAllMetaContactsForProvider(provider);
            while (metaContacts.hasNext())
            {
                MetaContact metaContact = metaContacts.next();
                Iterator<Contact> contacts = metaContact.getContacts();
                while (contacts.hasNext())
                {
                    allContacts.add(contacts.next());
                }
            }
        }
    }

    /**
     * Implements AbstractTableModel#getColumnName(int).
     */
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
        if (row < 0)
            return null;

        Contact contact = allContacts.get(row);
        switch (column)
        {
        case CONTACTNAME_INDEX:
            return contact.getDisplayName();
        case VERIFIED_INDEX:
            // TODO: Maybe use a CheckBoxColumn?
            return (OtrActivator.scOtrKeyManager
                        .isVerified(contact))
                ? OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_TRUE")
                : OtrActivator.resourceService.getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_FALSE");
        case FINGERPRINT_INDEX:
            return OtrActivator.scOtrKeyManager
                    .getRemoteFingerprint(contact);
        default:
            return null;
        }
    }

    /**
     * Implements AbstractTableModel#getRowCount().
     */
    public int getRowCount()
    {
        return allContacts.size();
    }

    /**
     * Implements AbstractTableModel#getColumnCount().
     */
    public int getColumnCount()
    {
        return 3;
    }
}
