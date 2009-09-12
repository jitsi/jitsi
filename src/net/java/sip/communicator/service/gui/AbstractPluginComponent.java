/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Provides an abstract base implementation of <code>PluginComponent</code> in
 * order to take case of the implementation boilerplate and let implementers
 * focus on the specifics of their plugin.
 * 
 * @author Lubomir Marinov
 */
public abstract class AbstractPluginComponent
    implements PluginComponent
{

    /**
     * The container in which the component of this plugin is to be added.
     */
    private final Container container;

    /**
     * Initializes a new <code>AbstractPluginComponent</code> which is to be
     * added to a specific <code>Container</code>.
     * 
     * @param container
     *            the container in which the component of the new plugin is to
     *            be added
     */
    protected AbstractPluginComponent(Container container)
    {
        this.container = container;
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

    /*
     * Implements PluginComponent#getPositionIndex().
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
}
