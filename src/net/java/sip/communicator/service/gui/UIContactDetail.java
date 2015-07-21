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
package net.java.sip.communicator.service.gui;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>UIContactDetail</tt> corresponds to a particular contact detail,
 * phone number, IM identifier, email, etc. which has it's preferred mode of
 * transport <tt>ProtocolProviderService</tt>.
 *
 * @author Yana Stamcheva
 */
public abstract class UIContactDetail
{
    /**
     * The prefix to be used when calling this contact detail.
     */
    private String prefix;

    /**
     * The address of this detail.
     */
    private final String address;

    /**
     * The display name of this detail.
     */
    private final String displayName;

    /**
     * The <tt>ProtocolProviderService</tt> corresponding to this detail.
     */
    private Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                    preferredProviders;

    /**
     * The protocol to be used for this contact detail if no protocol provider
     * is set.
     */
    private Map<Class<? extends OperationSet>, String> preferredProtocols;

    /**
     * The collection of labels associated with this detail.
     */
    private final Collection<String> labels;

    /**
     * The category of the underlying contact detail.
     */
    private final String category;

    /**
     * The underlying object that this class is wrapping
     */
    private final Object descriptor;

    /**
     * Creates a <tt>UIContactDetail</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     *
     * @param address the contact address
     * @param displayName the contact display name
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetail(
        String address,
        String displayName,
        Object descriptor)
    {
        this(   address,
                displayName,
                null,
                null,
                null,
                null,
                descriptor);
    }

    /**
     * Creates a <tt>UIContactDetail</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     *
     * @param address the contact address
     * @param displayName the contact display name
     * @param category the category of the underlying contact detail
     * @param labels the collection of labels associated with this detail
     * @param preferredProviders the preferred protocol provider
     * @param preferredProtocols the preferred protocol if no protocol provider
     * is set
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetail(
        String address,
        String displayName,
        String category,
        Collection<String> labels,
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                            preferredProviders,
        Map<Class<? extends OperationSet>, String> preferredProtocols,
        Object descriptor)
    {
        this.address = address;
        this.displayName = displayName;
        this.category = category;
        this.labels = labels;
        this.preferredProviders = preferredProviders;
        this.preferredProtocols = preferredProtocols;
        this.descriptor = descriptor;
    }

    /**
     * Returns the display name of this detail.
     * @return the display name of this detail
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the address of this detail.
     * @return the address of this detail
     */
    public String getAddress()
    {
        if (prefix != null && prefix.trim().length() >= 0)
            return prefix + address;

        return address;
    }

    /**
     * Returns the category of the underlying detail.
     *
     * @return the category of the underlying detail
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Returns an iterator over the collection of labels associated with this
     * detail.
     *
     * @return an iterator over the collection of labels associated with this
     * detail
     */
    public Iterator<String> getLabels()
    {
        if (labels != null)
            return labels.iterator();

        return null;
    }

    /**
     * Returns the protocol provider preferred for contacting this detail for
     * the given <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for provider
     * @return the protocol provider preferred for contacting this detail
     */
    public ProtocolProviderService getPreferredProtocolProvider(
        Class<? extends OperationSet> opSetClass)
    {
        if (preferredProviders != null)
            return preferredProviders.get(opSetClass);
        return null;
    }

    /**
     * Adds a preferred protocol provider for a given OperationSet class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for protocol
     * @param protocolProvider the preferred protocol provider to add
     */
    public void addPreferredProtocolProvider(
                                    Class<? extends OperationSet> opSetClass,
                                    ProtocolProviderService protocolProvider)
    {
        if (preferredProviders == null)
            preferredProviders = new HashMap<   Class<? extends OperationSet>,
                                                ProtocolProviderService>();

        preferredProviders.put(opSetClass, protocolProvider);
    }

    /**
     * Returns the name of the protocol preferred for contacting this detail for
     * the given <tt>OperationSet</tt> class if no preferred protocol provider
     * is set.
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for protocol
     * @return the name of the protocol preferred for contacting this detail
     */
    public String getPreferredProtocol(Class<? extends OperationSet> opSetClass)
    {
        if (preferredProtocols != null)
            return preferredProtocols.get(opSetClass);
        return null;
    }

    /**
     * Adds a preferred protocol for a given OperationSet class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class for which we're looking
     * for protocol
     * @param protocol the preferred protocol to add
     */
    public void addPreferredProtocol(
                                    Class<? extends OperationSet> opSetClass,
                                    String protocol)
    {
        if (preferredProtocols == null)
            preferredProtocols = new HashMap<   Class<? extends OperationSet>,
                                                String>();

        preferredProtocols.put(opSetClass, protocol);
    }

    /**
     * Returns the prefix to be used when calling this contact detail.
     *
     * @return the prefix to be used when calling this contact detail
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Sets the prefix to be used when calling this contact detail.
     *
     * @param prefix the prefix to be used when calling this contact detail
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * Returns the underlying object that this class is wrapping
     *
     * @return the underlying object that this class is wrapping
     */
    public Object getDescriptor()
    {
        return descriptor;
    }

    /**
     * Returns the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence.
     * @return the <tt>PresenceStatus</tt> of this <tt>ContactDetail</tt> or
     * null if the detail doesn't support presence
     */
    public abstract PresenceStatus getPresenceStatus();
}
