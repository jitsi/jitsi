/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>SourceContact</tt> for a <tt>MAPI_MAILUSER</tt>.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookMailUserSourceContact
    implements SourceContact
{
    /**
     * The <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>.
     */
    private final List<ContactDetail> contactDetails;

    /**
     * The <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>.
     */
    private final MsOutlookAddrBookContactSourceService contactSource;

    /**
     * The display name of this <tt>SourceContact</tt>.
     */
    private final String displayName;

    /**
     * Initializes a new <tt>MsOutlookMailUserSourceContact</tt> instance.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is creating
     * the new instance
     * @param displayName the display name of the new instance
     * @param contactDetails the <tt>ContactDetail</tt>s of the new instance
     */
    public MsOutlookMailUserSourceContact(
            MsOutlookAddrBookContactSourceService contactSource,
            String displayName,
            List<ContactDetail> contactDetails)
    {
        this.contactSource = contactSource;
        this.displayName = displayName;
        this.contactDetails = contactDetails;
    }

    /**
     * Gets the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>.
     *
     * @return the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>
     * @see SourceContact#getContactDetails()
     */
    public List<ContactDetail> getContactDetails()
    {
        return Collections.unmodifiableList(contactDetails);
    }

    /**
     * Gets the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt> which
     * support a specific <tt>OperationSet</tt>.
     *
     * @param operationSet the <tt>OperationSet</tt> the supporting
     * <tt>ContactDetail</tt>s of which are to be returned
     * @return the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt> which
     * support the specified <tt>operationSet</tt>
     * @see SourceContact#getContactDetails(Class)
     */
    public List<ContactDetail> getContactDetails(
            Class<? extends OperationSet> operationSet)
    {
        List<ContactDetail> contactDetails = new LinkedList<ContactDetail>();

        for (ContactDetail contactDetail : getContactDetails())
        {
            List<Class<? extends OperationSet>> supportedOperationSets
                = contactDetail.getSupportedOperationSets();

            if ((supportedOperationSets != null)
                    && supportedOperationSets.contains(operationSet))
                contactDetails.add(contactDetail);
        }
        return contactDetails;
    }

    /**
     * Gets the <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>.
     *
     * @return the <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>
     * @see SourceContact#getContactSource()
     */
    public MsOutlookAddrBookContactSourceService getContactSource()
    {
        return contactSource;
    }

    /**
     * Gets the display details of this <tt>SourceContact</tt>.
     *
     * @return the display details of this <tt>SourceContact</tt>
     * @see SourceContact#getDisplayDetails()
     */
    public String getDisplayDetails()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the display name of this <tt>SourceContact</tt>.
     *
     * @return the display name of this <tt>SourceContact</tt>
     * @see SourceContact#getDisplayName()
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Gets the image/avatar of this <tt>SourceContact</tt>.
     *
     * @return the image/avatar of this <tt>SourceContact</tt>
     * @see SourceContact#getImage()
     */
    public byte[] getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the preferred <tt>ContactDetail</tt> for a specific
     * <tt>OperationSet</tt>.
     *
     * @param operationSet the <tt>OperationSet</tt> to get the preferred
     * <tt>ContactDetail</tt> for
     * @return the preferred <tt>ContactDetail</tt> for the specified
     * <tt>operationSet</tt>
     * @see SourceContact#getPreferredContactDetail(Class)
     */
    public ContactDetail getPreferredContactDetail(
            Class<? extends OperationSet> operationSet)
    {
        List<ContactDetail> contactDetails = getContactDetails(operationSet);

        return contactDetails.isEmpty() ? null : contactDetails.get(0);
    }
}
