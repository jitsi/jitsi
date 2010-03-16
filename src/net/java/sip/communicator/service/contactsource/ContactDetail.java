/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactDetail</tt> is a detail of a <tt>SourceContact</tt>
 * corresponding to a specific address (phone number, email, identifier, etc.),
 * which defines the different possible types of communication and the preferred
 * <tt>ProtocolProviderService</tt>s to go through.
 * 
 * <p>
 * Example: A <tt>ContactDetail</tt> could define two types of communication,
 * by declaring two supported operation sets
 * <tt>OperationSetBasicInstantMessaging</tt> to indicate the support of instant
 * messages and <tt>OperationSetBasicTelephony</tt> to indicate the support of
 * telephony. It may then specify a certain <tt>ProtocolProviderService</tt> to
 * go through only for instant messages. This would mean that for sending an
 * instant message to this <tt>ContactDetail</tt> one should obtain an instance
 * of the <tt>OperationSetBasicInstantMessaging</tt> from the specific
 * <tt>ProtocolProviderService</tt> and send a message through it. However when
 * no provider is specified for telephony operations, then one should try to
 * obtain all currently available telephony providers and let the user make
 * their choice.
 *
 * @author Yana Stamcheva
 */
public class ContactDetail
{
    /**
     * The address of this contact detail. This should be the address through
     * which the contact could be reached by one of the supported
     * <tt>OperationSet</tt>s (e.g. by IM, call).
     */
    private final String contactAddress;

    /**
     * A mapping of <tt>OperationSet</tt> classes and preferred protocol
     * providers for them.
     */
    private Map<Class<? extends OperationSet>, ProtocolProviderService>
        preferredProviders;

    /**
     * A list of all supported <tt>OperatioSet</tt> classes.
     */
    private List<Class<? extends OperationSet>> supportedOpSets = null;

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the contact address,
     * corresponding to this detail.
     * @param contactAddress
     */
    public ContactDetail(String contactAddress)
    {
        this.contactAddress = contactAddress;
    }

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the corresponding contact
     * address and a mapping of preferred <tt>ProtocolProviderServices</tt> for
     * a specific <tt>OperationSet</tt>.
     * @param contactAddress the contact address corresponding to this detail
     * @param preferredProviders a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public ContactDetail(String contactAddress,
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                            preferredProviders)
    {
        this(contactAddress);

        this.preferredProviders = preferredProviders;
    }

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the corresponding contact
     * address and a list of all <tt>supportedOpSets</tt>, indicating what are
     * the supporting actions with this contact detail (e.g. sending a message,
     * making a call, etc.)
     * @param contactAddress the address of the contact
     * @param supportedOpSets a list of all <tt>supportedOpSets</tt>, indicating
     * what are the supporting actions with this contact detail (e.g. sending a
     * message, making a call, etc.)
     */
    public ContactDetail(   String contactAddress,
                            List<Class<? extends OperationSet>> supportedOpSets)
    {
        this(contactAddress);

        this.supportedOpSets = supportedOpSets;
    }

    /**
     * Returns the contact address corresponding to this detail.
     * @return the contact address corresponding to this detail
     */
    public String getContactAddress()
    {
        return contactAddress;
    }

    /**
     * Returns the preferred <tt>ProtocolProviderService</tt> when using the
     * given <tt>opSetClass</tt>.
     * @param opSetClass the <tt>OperationSet</tt> class corresponding to a
     * certain action (e.g. sending an instant message, making a call, etc.).
     * @return the preferred <tt>ProtocolProviderService</tt> corresponding to
     * the given <tt>opSetClass</tt>
     */
    public ProtocolProviderService getPreferredProtocolProvider(
        Class<? extends OperationSet> opSetClass)
    {
        return preferredProviders.get(opSetClass);
    }

    /**
     * Returns a list of all supported <tt>OperationSet</tt> classes, which
     * would indicate what are the supported actions by this contact
     * (e.g. write a message, make a call, etc.)
     * @return a list of all supported <tt>OperationSet</tt> classes
     */
    public List<Class<? extends OperationSet>> getSupportedOperationSets()
    {
        return supportedOpSets;
    }

    /**
     * Sets the list of supported <tt>OperationSet</tt> classes. These are meant
     * to indicate what are the supported actions (sending an IM message,
     * making a call, etc.).
     * @param opSets the list of supported <tt>OperationSet</tt> classes
     */
    public void setSupportedOperationSets(
        List<Class<? extends OperationSet>> opSets)
    {
        this.supportedOpSets = opSets;
    }

    /**
     * Sets a mapping of preferred <tt>ProtocolProviderServices</tt> for
     * <tt>OperationSet</tt> classes.
     * @param preferredProviders a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public void setPreferredProtocolProviders(
        Map<Class<? extends OperationSet>, ProtocolProviderService>
            preferredProviders)
    {
        this.preferredProviders = preferredProviders;
    }
}
