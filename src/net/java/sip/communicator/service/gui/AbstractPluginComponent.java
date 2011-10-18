/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     * The container in which the component of this plug-in is to be added.
     */
    private final Container container;

    /**
     * Initializes a new <code>AbstractPluginComponent</code> which is to be
     * added to a specific <code>Container</code>.
     * 
     * @param container the container in which the component of the new plug-in
     * is to be added
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
