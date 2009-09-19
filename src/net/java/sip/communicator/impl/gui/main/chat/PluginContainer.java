/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

/**
 * Provides capabilities to a specific <code>JComponent</code> to contain
 * <code>PluginComponent</code>s, track when they are added and removed.
 * 
 * @author Lubomir Marinov
 */
public class PluginContainer
    implements PluginComponentListener
{
    private static final Logger logger
        = Logger.getLogger(PluginContainer.class);

    /**
     * The <code>JComponent</code> which contains the components of the
     * <code>PluginComponent</code>s managed by this instance.
     */
    private final JComponent container;

    /**
     * The container id of the <code>PluginComponent</code> managed by this
     * instance.
     */
    private final Container containerId;

    /**
     * The list of <code>PluginComponent</code> instances which have their
     * components added to this <code>PluginContainer</code>.
     */
    private final java.util.List<PluginComponent> pluginComponents
        = new LinkedList<PluginComponent>();

    /**
     * Initializes a new <code>PluginContainer</code> instance which is to
     * provide capabilities to a specific <code>JComponent</code> container with
     * a specific <code>Container</code> id to contain
     * <code>PluginComponent</code> and track when they are added and removed.
     * 
     * @param container
     *            the <code>JComponent</code> container the new instance is to
     *            provide its capabilities to
     * @param containerId
     *            the <code>Container</code> id of the specified
     *            <code>container</code>
     */
    public PluginContainer(JComponent container, Container containerId)
    {
        this.container = container;
        this.containerId = containerId;

        initPluginComponents();
    }

    /**
     * Adds a specific <code>Component</code> to a specific
     * <code>JComponent</code> container. Allows extenders to apply custom logic
     * to the exact placement of the specified <code>Component</code> in the
     * specified container.
     * 
     * @param component
     *            the <code>Component</code> to be added to the specified
     *            <code>JComponent</code> container
     * @param container
     *            the <code>JComponent</code> container to add the specified
     *            <code>Component</code> to
     */
    protected void addComponentToContainer(
        Component component,
        JComponent container)
    {
        container.add(component);
    }

    /**
     * Adds the component of a specific <code>PluginComponent</code> to this
     * <code>JMenuBar</code>.
     * 
     * @param c
     *            the <code>PluginComponent</code> which is to have its
     *            component added to this <code>PluginContainer</code>
     */
    private void addPluginComponent(PluginComponent c)
    {
        if (pluginComponents.contains(c))
            return;

        addComponentToContainer((Component) c.getComponent(), container);
        pluginComponents.add(c);

        container.revalidate();
        container.repaint();
    }

    public Iterable<PluginComponent> getPluginComponents()
    {
        return pluginComponents;
    }

    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        try
        {
            serRefs
                = GuiActivator
                    .bundleContext
                        .getServiceReferences(
                            PluginComponent.class.getName(),
                            "("
                                + Container.CONTAINER_ID
                                + "="
                                + containerId.getID()
                                + ")");
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                PluginComponent component
                    = (PluginComponent)
                        GuiActivator.bundleContext.getService(serRef);

                addPluginComponent(component);
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        GuiActivator.getUIService().removePluginComponentListener(this);

        /*
         * Explicitly remove the components of the PluginComponent instances
         * because the latter are registered with OSGi and are thus global.
         */
        for (PluginComponent pluginComponent : pluginComponents)
            container.remove((Component) pluginComponent.getComponent());
        pluginComponents.clear();
    }

    /*
     * Implements
     * PluginComponentListener#pluginComponentAdded(PluginComponentEvent).
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(containerId))
            addPluginComponent(c);
    }

    /*
     * Implements
     * PluginComponentListener#pluginComponentRemoved(PluginComponentEvent).
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(containerId))
            removePluginComponent(c);
    }

    /**
     * Removes the component of a specific <code>PluginComponent</code> from
     * this <code>PluginContainer</code>.
     * 
     * @param c
     *            the <code>PluginComponent</code> which is to have its
     *            component removed from this <code>PluginContainer</code>
     */
    private void removePluginComponent(PluginComponent c)
    {
        container.remove((Component) c.getComponent());
        pluginComponents.remove(c);
    }
}
