/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui;

import java.awt.Component;
import java.util.*;

import net.java.sip.communicator.impl.gui.events.ContainerPluginListener;
import net.java.sip.communicator.impl.gui.events.PluginComponentEvent;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.Logger;

public class UIServiceImpl implements UIService {
	
    private static final Logger logger = Logger
        .getLogger(UIServiceImpl.class);
    
    private Map registeredPlugins = new Hashtable();
    
    private Vector containerPluginListeners = new Vector();
    
    private static final List supportedContainers = new ArrayList();
    static{
        supportedContainers.add(UIService.CONTAINER_MAIN_TOOL_BAR);        
        supportedContainers.add(UIService.CONTAINER_CHAT_TOOL_BAR);
        supportedContainers.add(UIService.CONTAINER_CHAT_NEW_TOOL_BAR);
    }
    
    /**
     * Implements addComponent in UIService interface.
     * @see UIService#addComponent(ContainerID, Object)
     */
    public void addComponent(ContainerID containerID, Object component) 
        throws ClassCastException, IllegalArgumentException {
        
        if(!supportedContainers.contains(containerID))
            throw new IllegalArgumentException
                ("The constraint that you specified is not"
                        + " supported by this UIService implementation.");        
        else if(!(component instanceof Component)){
            throw new ClassCastException
                ("The specified plugin is not a valid swing or awt component.");
        }
        else{
            if(registeredPlugins.containsKey(containerID)){
                ((Vector)registeredPlugins.get(containerID)).add(component);
            }
            else{
                Vector plugins = new Vector();
                plugins.add(component);
                registeredPlugins.put(containerID, plugins);
            }
            this.firePluginEvent(component, containerID, 
                    PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
        }
    }

    /**
     * Implements getSupportedContainers in UIService interface.
     * @see UIService#getSupportedContainers()
     */
    public Iterator getSupportedContainers() {        
        return Collections.unmodifiableList(supportedContainers).iterator();
    }

    /**
     * Implements getComponentsForConstraint in UIService interface.
     * @see UIService#getComponentsForContainer(ContainerID)
     */
    public Iterator getComponentsForContainer(ContainerID containerID)
        throws IllegalArgumentException {
        
        Vector plugins = (Vector)this.registeredPlugins.get(containerID);
        
        if(plugins != null)
            return plugins.iterator();
        else
            throw new IllegalArgumentException("The container that you specified is not " 
                    + "supported by this UIService implementation.");
    }

    /**
     * For now this method only invokes addComponent(containerID, component).
     */
    public void addComponent(ContainerID containerID, String constraint, Object component) 
        throws ClassCastException, IllegalArgumentException {
       this.addComponent(containerID, component);
    }

    /**
     * Not yet implemented.
     */
    public Iterator getConstraintsForContainer(ContainerID containerID) {        
        return null;
    }   
    
    /**
     * Creates the corresponding PluginComponentEvent and notifies all
     * <tt>ContainerPluginListener</tt>s that a plugin component is added or
     * removed from the container.
     *
     * @param pluginComponent the plugin component that is added to the
     * container.
     * @param containerID the containerID that corresponds to the container where 
     * the component is added.
     * @param eventID
     *            one of the PLUGIN_COMPONENT_XXX static fields indicating the
     *            nature of the event.
     */
    private void firePluginEvent(Object pluginComponent,  ContainerID containerID,          
            int eventID)
    {
        PluginComponentEvent evt
            = new PluginComponentEvent(pluginComponent, containerID, eventID);

        logger.trace("Will dispatch the following plugin component event: " + evt);

        synchronized (containerPluginListeners)
        {
            Iterator listeners = this.containerPluginListeners.iterator();
        
            while (listeners.hasNext())
            {
                ContainerPluginListener l = (ContainerPluginListener) listeners.next();
                
                switch (evt.getEventID())
                {
                    case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                        l.pluginComponentAdded(evt);
                        break;
                    case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                        l.pluginComponentRemoved(evt);
                        break;
                    default:
                        logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
    }
}
