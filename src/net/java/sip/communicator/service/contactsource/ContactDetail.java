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
     * A mapping of <tt>OperationSet</tt> classes and preferred protocol name
     * for them.
     */
    private Map<Class<? extends OperationSet>, String> preferredProtocols;

    /**
     * A list of all supported <tt>OperatioSet</tt> classes.
     */
    private List<Class<? extends OperationSet>> supportedOpSets = null;

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the contact address,
     * corresponding to this detail.
     * @param contactAddress the contact address corresponding to this detail
     */
    public ContactDetail(String contactAddress)
    {
        this.contactAddress = contactAddress;
    }

    /**
     * Sets a mapping of preferred <tt>ProtocolProviderServices</tt> for
     * a specific <tt>OperationSet</tt>.
     * @param preferredProviders a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public void setPreferredProviders(
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                            preferredProviders)
    {
        this.preferredProviders = preferredProviders;
    }

    /**
     * Sets a mapping of a preferred <tt>preferredProtocol</tt> for a specific
     * <tt>OperationSet</tt>. The preferred protocols are meant to be set by
     * contact source implementations that don't have a specific protocol
     * providers to suggest, but are able to propose just the name of the
     * protocol to be used for a specific operation. If both - preferred
     * provider and preferred protocol are set, then the preferred protocol
     * provider should be prioritized.
     *
     * @param preferredProtocols a mapping of preferred
     * <tt>ProtocolProviderService</tt>s for specific <tt>OperationSet</tt>
     * classes
     */
    public void setPreferredProtocols(
        Map<Class<? extends OperationSet>, String> preferredProtocols)
    {
        this.preferredProtocols = preferredProtocols;
    }

    /**
     * Creates a <tt>ContactDetail</tt> by specifying the corresponding contact
     * address and a list of all <tt>supportedOpSets</tt>, indicating what are
     * the supporting actions with this contact detail (e.g. sending a message,
     * making a call, etc.)
     * @param supportedOpSets a list of all <tt>supportedOpSets</tt>, indicating
     * what are the supporting actions with this contact detail (e.g. sending a
     * message, making a call, etc.)
     */
    public void setSupportedOpSets(
                            List<Class<? extends OperationSet>> supportedOpSets)
    {
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
        if (preferredProviders != null && preferredProviders.size() > 0)
            return preferredProviders.get(opSetClass);

        return null;
    }

    /**
     * Returns the name of the preferred protocol for the operation given by
     * the <tt>opSetClass</tt>. The preferred protocols are meant to be set by
     * contact source implementations that don't have a specific protocol
     * providers to suggest, but are able to propose just the name of the
     * protocol to be used for a specific operation. If both - preferred
     * provider and preferred protocol are set, then the preferred protocol
     * provider should be prioritized.
     *
     * @param opSetClass the <tt>OperationSet</tt> class corresponding to a
     * certain action (e.g. sending an instant message, making a call, etc.).
     * @return the name of the preferred protocol for the operation given by
     * the <tt>opSetClass</tt>
     */
    public String getPreferredProtocol(Class<? extends OperationSet> opSetClass)
    {
        if (preferredProtocols != null && preferredProtocols.size() > 0)
            return preferredProtocols.get(opSetClass);

        return null;
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
}
