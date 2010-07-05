package net.java.sip.communicator.plugin.securityconfig.chat;

import java.awt.*;
import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.securityconfig.*;
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
                SecurityConfigActivator.bundleContext
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
            SecurityConfigActivator.bundleContext
                .getServiceReference(MetaContactListService.class
                    .getName());

        MetaContactListService service
            = (MetaContactListService) SecurityConfigActivator
                .bundleContext.getService(ref);

        // Populate contacts.
        for (int i = 0; i < protocolProviderRefs.length; i++)
        {
            ProtocolProviderService provider
                = (ProtocolProviderService) SecurityConfigActivator
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
            return SecurityConfigActivator.getResources()
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_CONTACT");
        case VERIFIED_INDEX:
            return SecurityConfigActivator.getResources()
                .getI18NString(
                    "plugin.otr.configform.COLUMN_NAME_VERIFIED_STATUS");
        case FINGERPRINT_INDEX:
            return SecurityConfigActivator.getResources()
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
            return (SecurityConfigActivator.getOtrKeyManagerService()
                        .isVerified(contact))
                ? SecurityConfigActivator.getResources().getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_TRUE")
                : SecurityConfigActivator.getResources().getI18NString(
                    "plugin.otr.configform.COLUMN_VALUE_VERIFIED_FALSE");
        case FINGERPRINT_INDEX:
            return SecurityConfigActivator.getOtrKeyManagerService()
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
