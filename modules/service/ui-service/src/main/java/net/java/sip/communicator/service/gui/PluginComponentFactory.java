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

import java.lang.ref.*;
import java.util.*;

/**
 * The <tt>PluginComponentFactory</tt> is the factory that will be used by the
 * <tt>PluginComponents</tt> to register in OSGI bundle context and will be
 * used by containers of the plugins to create Plugin component instances.
 *
 * @author Damian Minkov
 */
public abstract class PluginComponentFactory
{
    /**
     * The container id for this plugin.
     */
    private Container container;

    /**
     * Any special constraints if needed for placing the plugin instances.
     */
    private String constraints;

    /**
     * The position of the plugin, where to be added.
     */
    private int position;

    /**
     * Is it a native component.
     */
    private boolean nativeComponent;

    /**
     * Weak hash map holding plugins if the parent (container) is garbage
     * collected free and the plugin instance that corresponds it.
     */
    private WeakHashMap<Object,WeakReference<PluginComponent>> pluginInstances =
        new WeakHashMap<Object, WeakReference<PluginComponent>>();

    /**
     * Creates a default factory for a <tt>container</tt>.
     * @param container the container id for the plugins to be created.
     */
    public PluginComponentFactory(Container container)
    {
        this(container, null, -1, false);

    }

    /**
     * Creates factory.
     * @param container the container id
     * @param constraints the constraints
     * @param position a position for the plugin component.
     * @param nativeComponent is it native one.
     */
    public PluginComponentFactory(Container container,
                                  String constraints,
                                  int position,
                                  boolean nativeComponent)
    {
        this.container = container;
        this.constraints = constraints;
        this.position = position;
        this.nativeComponent = nativeComponent;
    }

    /**
     * Returns the identifier of the container, where we would like to add
     * our control. All possible container identifiers are defined in the
     * <tt>Container</tt> class. If the <tt>Container</tt> returned by this
     * method is not supported by the current UI implementation the plugin won't
     * be added.
     *
     * @return the container, where we would like to add our control.
     */
    public Container getContainer()
    {
        return container;
    }

    /**
     * Returns the constraints, which will indicate to the container, where this
     * component should be added. All constraints are defined in the Container
     * class and are as follows: START, END, TOP, BOTTOM, LEFT, RIGHT.
     *
     * @return the constraints, which will indicate to the container, where this
     * component should be added.
     */
    public String getConstraints()
    {
        return constraints;
    }

    /**
     * Returns the index position of this component in the container, where it
     * will be added. An index of 0 would mean that this component should be
     * added before all other components. An index of -1 would mean that the
     * position of this component is not important.
     * @return the index position of this component in the container, where it
     * will be added.
     */
    public int getPositionIndex()
    {
        return position;
    }

    /**
     * Returns <code>true</code> to indicate that this component is a native
     * component and <code>false</code> otherwise. This method is meant to be
     * used by containers if a special treatment is needed for native components.
     *
     * @return <code>true</code> to indicate that this component is a native
     * component and <code>false</code> otherwise.
     */
    public boolean isNativeComponent()
    {
        return nativeComponent;
    }

    /**
     * Returns the component that should be added. This method should return a
     * valid AWT, SWT or Swing object in order to appear properly in the user
     * interface.
     *
     * @param parent the parent that will contain this plugin
     * @return the component that should be added.
     */
    public PluginComponent getPluginComponentInstance(Object parent)
    {
        WeakReference<PluginComponent> ref = pluginInstances.get(parent);
        PluginComponent pluginComponent = (ref == null) ? null : ref.get();

        if (pluginComponent == null)
        {
            pluginComponent = getPluginInstance();
            if (pluginComponent != null)
            {
                ref = new WeakReference<PluginComponent>(pluginComponent);
                pluginInstances.put(parent, ref);
            }
        }
        return pluginComponent;
    }

    /**
     * Implementers use it to create plugin component instances.
     * @return the plugin component instance.
     */
    abstract protected PluginComponent getPluginInstance();
}
