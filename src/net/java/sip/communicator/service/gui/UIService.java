/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.util.Iterator;

/**
 * Plugins using the service in order to add featuress to the user interface
 * may do that using one of the addXxx() methods.
 *
 * @author Yana Stamcheva
 */
public interface UIService
{
    public static final ContainerID CONTAINER_FILE_MENU 
        = new ContainerID("File");
    public static final ContainerID CONTAINER_TOOLS_MENU 
        = new ContainerID("Tools");
    public static final ContainerID CONTAINER_VIEW_MENU 
        = new ContainerID("View");
    public static final ContainerID CONTAINER_HELP_MENU 
        = new ContainerID("Help");
    public static final ContainerID CONTAINER_SETTINGS_MENU 
        = new ContainerID("Settings");
    public static final ContainerID CONTAINER_MAIN_TOOL_BAR 
        = new ContainerID("MainToolBar");
    public static final ContainerID CONTAINER_CHAT_TOOL_BAR 
        = new ContainerID("ChatToolBar");
    public static final ContainerID CONTAINER_CHAT_NEW_TOOL_BAR 
        = new ContainerID("NewChatToolBar");
    public static final ContainerID CONTAINER_RIGHT_BUTTON_MENU 
        = new ContainerID("RightButtonMenu");
    public static final ContainerID CONTAINER_CONFIGURATION_MENU 
        = new ContainerID("ConfigurationMenu");
    public static final ContainerID CONTAINER_CHAT_MENU_BAR 
        = new ContainerID("ChatMenuBar");
    public static final ContainerID CONTAINER_CHAT_FILE_MENU 
        = new ContainerID("ChatFileMenu");
    public static final ContainerID CONTAINER_CHAT_EDIT_MENU 
        = new ContainerID("ChatEditMenu");
    public static final ContainerID CONTAINER_CHAT_SETTINGS_MENU 
        = new ContainerID("ChatSettingsMenu");
    public static final ContainerID CONTAINER_CHAT_HELP_MENU 
        = new ContainerID("ChatHelpMenu");
        
    public static final String START = "Start";
    public static final String END = "End";
    public static final String TOP = "Top";
    public static final String BOTTOM = "Bottom";
    public static final String LEFT = "Left";
    public static final String RIGHT = "Right";
  
    /**
     * Adds the specified UI component to the container given by ContainerID. 
     * The method is meant to be used by plugins or bundles that would like to add 
     * components to the user interface. The <tt>containerID</tt> is used by the 
     * implementation to determine the place where the component should be added. 
     * The <tt>containerID</tt> SHOULD be one of the CONTAINER_XXX constants. It is up
     * to the service implementation to verify that <tt>component</tt> is an
     * instance of a class compatible with the gui library used by it. If this
     * is not the case and adding the requested object would not be possible the
     * implementation MUST through a ClassCastException exception.
     * Implementations of this service MUST understand and know how to handle
     * all ContainerID-s defined by this interface, they MAY also define additional constraints. 
     * In case the addComponent method is called with a <tt>containerID</tt> that the 
     * implementation does not understand it MUST through a java.lang.IllegalArgumentException 
     * <br>
     * @param containerID One of the CONTAINER_XXX ContainerID-s. 
     * @param component The component to be added.
     * @throws ClassCastException if <tt>component</tt> is not an
     * instance of a class supported by the service implementation. An SWT impl
     * would, for example through a ClassCastException if handed a
     * java.awt.Component
     * @throws IllegalArgumentException if the specified <tt>containerID</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all CONTAINER_XXX containerID-s.
     */
    public void addComponent(ContainerID containerID, Object component)
        throws ClassCastException, IllegalArgumentException;
    
    /**
     * Adds the specified UI component to the container given by <tt>containerID</tt> at
     * the position specified by <tt>constraint</tt> String. The method is meant to be used 
     * by plugins or bundles that would like to add components to the user interface. 
     * The <tt>containerID</tt> is used by the implementation to determine the place where 
     * the component should be added. The <tt>containerID</tt> SHOULD be one of the 
     * CONTAINER_XXX constants. The <tt>constraint</tt> String is used to determine the
     * exact position of the component in the container (LEFT, RIGHT, START, etc.). The 
     * <tt>constraint</tt> String SHOULD be one of the START, END, TOP, BOTTOM, etc. 
     * String constants.
     * <br> 
     * It is up to the service implementation to verify that <tt>component</tt> is an
     * instance of a class compatible with the gui library used by it. If this
     * is not the case and adding the requested object would not be possible the
     * implementation MUST through a ClassCastException exception.
     * Implementations of this service MUST understand and know how to handle
     * all ContainerID-s defined by this interface, they MAY also define additional constraints. 
     * In case the addComponent method is called with a <tt>containerID</tt> that the 
     * implementation does not understand it MUST through a java.lang.IllegalArgumentException 
     * <br>
     * @param containerID One of the CONTAINER_XXX ContainerID-s. 
     * @param component The component to be added.
     * @throws ClassCastException if <tt>component</tt> is not an
     * instance of a class supported by the service implementation. An SWT impl
     * would, for example through a ClassCastException if handed a
     * java.awt.Component
     * @throws IllegalArgumentException if the specified <tt>containerID</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all CONTAINER_XXX containerID-s.
     */
    public void addComponent(ContainerID containerID, String constraint, Object component)
        throws ClassCastException, IllegalArgumentException;
    
    /**
     * Returns an iterator to a set containing containerID-s pointing to containers 
     * supported by the current UI implementation. Each containerID in the set 
     * is one of the CONTAINER_XXX constants. The method is meant to be used by plugins 
     * or bundles that would like to add components to the user interface. Before adding 
     * any component they should use this method to obtain all possible places, which could 
     * contain external components, like different menus, toolbars, etc.  
     *       
     * @return Iterator An iterator to a set containing containerID-s representing all
     * containers supported by the current UI implementation.
     */
    public Iterator getSupportedContainers();
    
    /**
     * Returns an iterator to a set containing all constraints supported by the given 
     * <tt>containerID</tt>. Each constraint in the set is one of the START, END, TOP, 
     * BOTTOM, etc. constants. This method is meant to be used to obtain all layout 
     * constraints supported by a given container.
     * 
     * @param containerID The containerID pointing to the desired container.
     * @return Iterator An iterator to a set containing all component constraints
     */
    public Iterator getConstraintsForContainer(ContainerID containerID);
    
    /**
     * Returns an Iterator to a set containing all components added to a given constraint.
     * Meant to be called in the process of initialization of the container, defined by the 
     * given constraint in order to obtain all external components that should be added in it. 
     * 
     * @param containerID One of the containerID-s supported by the current UI implementation.
     * @return An Iterator to a set containing all components added to a given constraint.
     */
    public Iterator getComponentsForContainer(ContainerID containerID)
        throws IllegalArgumentException;
}
