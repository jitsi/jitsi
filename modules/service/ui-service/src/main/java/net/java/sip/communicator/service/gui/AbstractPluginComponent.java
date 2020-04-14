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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Provides an abstract base implementation of <code>PluginComponent</code> in
 * order to take care of the implementation boilerplate and let implementers
 * focus on the specifics of their plug-in.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractPluginComponent
    implements PluginComponent
{
    /**
     * The parent factory.
     */
    private final PluginComponentFactory parentFactory;

    /**
     * The container in which the component of this plug-in is to be added.
     */
    private final Container container;

    /**
     * Initializes a new <code>AbstractPluginComponent</code> which is to be
     * added to a specific <code>Container</code>.
     *
     * @param container the container in which the component of the new plug-in
     * is to be added
     * @param parentFactory the parent <tt>PluginComponentFactory</tt> that is
     * creating this plugin component.
     */
    protected AbstractPluginComponent(Container container,
                                      PluginComponentFactory parentFactory)
    {
        this.container = container;
        this.parentFactory = parentFactory;
    }

    /*
     * Implements PluginComponent#getConstraints().
     */
    public String getConstraints()
    {
        return null;
    }

    /*
     * Implements PluginComponent#getContainer().
     */
    public Container getContainer()
    {
        return container;
    }

    /**
     * Implements {@link PluginComponent#getPositionIndex()}. Returns
     * <tt>-1</tt> which indicates that the position of this
     * <tt>AbstractPluginComponent</tt> within its <tt>Container</tt> is of no
     * importance.
     *
     * @return <tt>-1</tt> which indicates that the position of this
     * <tt>AbstractPluginComponent</tt> within its <tt>Container</tt> is of no
     * importance
     * @see PluginComponent#getPositionIndex()
     */
    public int getPositionIndex()
    {
        return -1;
    }

    /*
     * Implements PluginComponent#isNativeComponent().
     */
    public boolean isNativeComponent()
    {
        return false;
    }

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     */
    public void setCurrentContact(Contact contact)
    {
    }

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     */
    public void setCurrentContact(Contact contact, String resourceName)
    {
        setCurrentContact(contact);
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    public void setCurrentContact(MetaContact metaContact)
    {
    }

    /*
     * Implements PluginComponent#setCurrentContactGroup(MetaContactGroup).
     */
    public void setCurrentContactGroup(MetaContactGroup metaGroup)
    {
    }

    /*
     * Implements PluginComponent#setCurrentAccountID(AccountID).
     */
    public void setCurrentAccountID(AccountID accountID)
    {
    }

    /**
     * Returns the factory that has created the component.
     * @return the parent factory.
     */
    public PluginComponentFactory getParentFactory()
    {
        return parentFactory;
    }
}
