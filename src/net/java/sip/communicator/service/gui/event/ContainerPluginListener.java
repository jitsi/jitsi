package net.java.sip.communicator.service.gui.event;

import java.util.EventListener;

public interface ContainerPluginListener 
    extends EventListener {

    /**
     * Indicates that a plugin component has been successfully added
     * to the container.
     * @param evt the PluginComponentEvent containing the corresponding plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event);
    
    /**
     * Indicates that a plugin component has been successfully removed
     * from the container.
     * @param evt the PluginComponentEvent containing the corresponding plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event);
}
