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
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Provides capabilities to a specific <code>JComponent</code> to contain
 * <code>PluginComponent</code>s, track when they are added and removed.
 *
 * @author Lyubomir Marinov
 */
public class PluginContainer
    implements PluginComponentListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>PluginContainer</tt> class and its
     * instances for logging output.
     */
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
     * Adds a specific <tt>Component</tt> to a specific <tt>JComponent</tt>
     * container. Allows extenders to apply custom logic to the exact placement
     * of the specified <tt>Component</tt> in the specified container.
     *
     * @param component the <tt>Component</tt> to be added to the specified
     * <tt>JComponent</tt> container
     * @param container the <tt>JComponent</tt> container to add the specified
     * <tt>Component</tt> to
     * @param preferredIndex the index at which <tt>component</tt> is to be
     * added to <tt>container</tt> if possible or <tt>-1</tt> if there is no
     * preference with respect to the index in question
     */
    protected void addComponentToContainer(
            Component component,
            JComponent container,
            int preferredIndex)
    {
        if ((0 <= preferredIndex)
                && (preferredIndex < getComponentCount(container)))
            container.add(component, preferredIndex);
        else
            container.add(component);
    }

    /**
     * Adds the component of a specific <tt>PluginComponent</tt> to the
     * associated <tt>Container</tt>.
     *
     * @param factory the <tt>PluginComponentFactory</tt> which is to have its
     *          component added to the <tt>Container</tt> associated with this
     *          <tt>PluginContainer</tt>
     */
    private synchronized void addPluginComponent(PluginComponentFactory factory)
    {
        PluginComponent c =
            factory.getPluginComponentInstance(PluginContainer.this);

        if (logger.isInfoEnabled())
            logger.info("Will add plugin component: " + c);

        /*
         * Try to respect positionIndex of PluginComponent to some extent:
         * PluginComponents with positionIndex equal to 0 go at the beginning,
         * these with positionIndex equal to -1 follow them and then go these
         * with positionIndex greater than 0.
         */
        int cIndex = factory.getPositionIndex();
        int index = -1;
        int i = 0;

        for (PluginComponent pluginComponent : pluginComponents)
        {
            if (pluginComponent.equals(c))
                return;

            if (-1 == index)
            {
                int pluginComponentIndex = factory.getPositionIndex();

                if ((0 == cIndex) || (-1 == cIndex))
                {
                    if ((0 != pluginComponentIndex)
                            && (cIndex != pluginComponentIndex))
                        index = i;
                }
                else if (cIndex < pluginComponentIndex)
                    index = i;
            }

            i++;
        }

        int pluginComponentCount = pluginComponents.size();

        if (-1 == index)
            index = pluginComponents.size();

        /*
         * The container may have added Components of its own apart from the
         * ones this PluginContainer has added to it. Since the common case for
         * the additional Components is to have them appear at the beginning,
         * adjust the index so it gets correct in the common case.
         */
        int containerComponentCount = getComponentCount(container);

        addComponentToContainer(
                (Component) c.getComponent(),
                container,
                (containerComponentCount > pluginComponentCount)
                    ? (index + (containerComponentCount - pluginComponentCount))
                    : index);
        pluginComponents.add(index, c);

        container.revalidate();
        container.repaint();
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
        synchronized (this)
        {
            for (PluginComponent pluginComponent : pluginComponents)
                container.remove((Component) pluginComponent.getComponent());
            pluginComponents.clear();
        }
    }

    /**
     * Gets the number of <tt>Component</tt>s in a specific <tt>JComponent</tt>
     * container. For example, returns the result of
     * <tt>getMenuComponentCount()</tt> if <tt>container</tt> is an instance of
     * <tt>JMenu</tt>.
     *
     * @param container the <tt>JComponent</tt> container to get the number of
     * <tt>Component</tt>s of
     * @return the number of <tt>Component</tt>s in the specified
     * <tt>container</tt>
     */
    protected int getComponentCount(JComponent container)
    {
        return
            (container instanceof JMenu)
                ? ((JMenu) container).getMenuComponentCount()
                : container.getComponentCount();
    }

    /**
     * Gets the <tt>PluginComponent</tt>s of this <tt>PluginContainer</tt>.
     *
     * @return an <tt>Iterable</tt> over the <tt>PluginComponent</tt>s of this
     * <tt>PluginContainer</tt>
     */
    public Iterable<PluginComponent> getPluginComponents()
    {
        return pluginComponents;
    }

    /**
     * Adds the <tt>Component</tt>s of the <tt>PluginComponent</tt>s registered
     * in the OSGi <tt>BundleContext</tt> in the associated <tt>Container</tt>.
     */
    private void initPluginComponents()
    {
        GuiActivator.getUIService().addPluginComponentListener(this);

        // Look for PluginComponents registered in the OSGi BundleContext.
        ServiceReference[] serRefs = null;

        try
        {
            serRefs
                = GuiActivator
                    .bundleContext
                        .getServiceReferences(
                            PluginComponentFactory.class.getName(),
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
                PluginComponentFactory factory
                    = (PluginComponentFactory)
                        GuiActivator.bundleContext.getService(serRef);

                addPluginComponent(factory);
            }
        }
    }

    /**
     * Implements
     * {@link PluginComponentListener#pluginComponentAdded(PluginComponentEvent)}.
     *
     * @param event a <tt>PluginComponentEvent</tt> which specifies the
     * <tt>PluginComponent</tt> which has been added
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (factory.getContainer().equals(containerId))
            addPluginComponent(factory);
    }

    /**
     * Implements
     * {@link PluginComponentListener#pluginComponentRemoved(PluginComponentEvent)}.
     *
     * @param event a <tt>PluginComponentEvent</tt> which specifies the
     * <tt>PluginComponent</tt> which has been added
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (factory.getContainer().equals(containerId))
            removePluginComponent(factory);
    }

    /**
     * Removes the component of a specific <code>PluginComponent</code> from
     * this <code>PluginContainer</code>.
     *
     * @param factory
     *            the <code>PluginComponent</code> which is to have its
     *            component removed from this <code>PluginContainer</code>
     */
    private synchronized void removePluginComponent(
        PluginComponentFactory factory)
    {
        Iterator<PluginComponent> iterator = pluginComponents.iterator();
        while(iterator.hasNext())
        {
            PluginComponent c = iterator.next();
            if(c.getParentFactory().equals(factory))
            {
                iterator.remove();
                container.remove((Component)c.getComponent());
            }
        }
    }
}
