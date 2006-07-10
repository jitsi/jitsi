/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.util.Iterator;

import net.java.sip.communicator.service.protocol.Contact;
/**
 * The <tt>UIService</tt> offers generic access to the graphical user interface
 * for all modules that would like to interact with the user.
 * <p>
 * Through the <tt>UIService</tt> all modules can add their own components in
 * different menus, toolbars, etc. within the ui. Each <tt>UIService</tt>
 * implementation should export its supported "plugable" containers - a set of
 * <tt>ContainerID</tt>s corresponding to different "places", where a module
 * can add a component.
 * <p>
 * The <tt>UIService</tt> provides also methods that would allow to other
 * modules to control the visibility, size and position of the main application
 * window. Some of these methods are: setVisible, minimize, maximize, resize,
 * move, etc. 
 * <p>
 * A way to show different types of simple dialogs is provided to allow other
 * modules to show different simple messages, like warning or error messages.
 * In order to show a simple warning message, a module should invoke the 
 * getPopupDialog method and then one of the showXXX methods, which corresponds
 * best to the required dialog. 
 * <p>
 * Certain application dialogs within the GUI, like "Configuration" or 
 * "AddContact" dialogs, could be also shown from outside the ui. To make one of
 * these dialogs showable, the <tt>UIService</tt> implementation should attach to
 * it a <tt>DialogID</tt> and export it. A dialog then could be shown, by
 * invoking <code>getApplicationDialog(DialogID)</code> and then 
 * <code>show</code>. The <tt>DialogID</tt> above should be one of the exported
 * <tt>DialogID</tt>s obtained from <code>getExportedDialogs</code>.
 * <p>
 * Each <code>UIService</code> implementation should implement the method
 * <code>getChatDialog(Contact contact)</code>, which is meant to provide an
 * access to the chat component for the given contact in the form of 
 * <code>ExportedDialog</code>.
 * 
 * @author Yana Stamcheva
 */
public interface UIService
{
    /*
     * ContainerID-s
     */
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
        
    /*
     * Constraints
     */
    public static final String START = "Start";
    public static final String END = "End";
    public static final String TOP = "Top";
    public static final String BOTTOM = "Bottom";
    public static final String LEFT = "Left";
    public static final String RIGHT = "Right";
  
    /*
     * DialogID-s
     */
    public static final DialogID DIALOG_CONFIGURATION
        = new DialogID("MainConfigurationDialog");
    
    public static final DialogID DIALOG_CONFIGURATION1
        = new DialogID("Configuration1Dialog");
    
    public static final DialogID DIALOG_CONFIGURATION2
        = new DialogID("Configuration2Dialog");
    
    public static final DialogID DIALOG_CONFIGURATION3
        = new DialogID("Configuration3Dialog");
    
    public static final DialogID DIALOG_ADD_CONTACT
        = new DialogID("AddContactDialog"); 
    
    /**
     * Returns TRUE if the application is visible and FALSE otherwise.
     * This method is meant to be used by the systray service in order to
     * detect the visibility of the application.
     * 
     * @return <code>true</code> if the application is visible and
     * <code>false</code> otherwise.
     * 
     * @see #setVisible(boolean)
     */
    public boolean isVisible();
    
    /**
     * Shows or hides the main application window depending on the value of
     * parameter <code>visible</code>. Meant to be used by the systray when it
     * needs to show or hide the application.
     * 
     * @param visible  if <code>true</code>, shows the main application window;
     * otherwise, hides the main application window.
     * 
     * @see #isVisible()
     */
    public void setVisible(boolean visible);
    
    /**
     * Minimizes the main application window.
     */
    public void minimize();
    
    /**
     * Mawimizes the main application window.
     */
    public void maximize();
    
    /**
     * Restores the main application window.
     */
    public void restore();
    
    /**
     * Resizes the main application window with the given width and height.
     * 
     * @param width The new width.
     * @param height The new height.
     */
    public void resize(int width, int height);
    
    /**
     * Moves the main application window to the given coordinates.
     * 
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    public void move(int x, int y);
        
    /**
     * Returns a common application dialog given by DialogID. This 
     * could be per example a "Configuration" dialog, "Add contact" dialog
     * or any other dialog within the application, which could be simply shown
     * without need of additional arguments. The <tt>dialogID</tt> SHOULD be
     * one of the DIALOG_XXX obtained by the getExportedDialogs method.
     *  
     * @param dialogID One of the DIALOG_XXX DialogID-s.
     * @throws IllegalArgumentException if the specified <tt>dialogID</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all DIALOG_XXX ID-s.
     * @return the dialog to be shown
     * @see #getExportedDialogs()
     */
    public ExportedDialog getApplicationDialog(DialogID dialogID)
        throws IllegalArgumentException;
    
    /**
     * Returns a configurable popup dialog, that could be used to show either
     * a warning message, error message, information message, etc. or to prompt
     * user for simple one field input or to question the user.
     *  
     * @return a <code>PopupDialog</code>.
     * @see PopupDialog
     */
    public PopupDialog getPopupDialog();
         
    /**
     * Returns the <code>ExportedDialog</code> corresponding to the component
     * representing the chat for the given contact. Meant to be used from other
     * bundles to allow them to check the visibility of a chat, hide it or show
     * it.
     * @param contact
     * @return The <code>ExportedDialog</code> corresponding to the component
     * representing the chat for the given contact.
     */
    public ExportedDialog getChatDialog(Contact contact);
    
    /**
     * Returns an iterator over a set of dialogID-s. Each DialogID points to
     * a common dialog in the current UI implementation that could be shown
     * using the <code>showApplicationDialog</code> method. Each 
     * DialogID in the set is one of the DIALOG_XXX constants.
     * The method is meant to be used by bundles that would like to show common
     * dialogs like "Configuration" or "Add contact" dialog. Before showing any
     * dialog they should use this method to obtain all possible dialogs, which
     * could be shown for the current ui implementation.  
     *       
     * @return Iterator An iterator to a set containing containerID-s 
     * representing all containers supported by the current UI implementation.
     */
    public Iterator getExportedDialogs();
    
    /**
     * Adds the specified UI component to the container given by ContainerID. 
     * The method is meant to be used by plugins or bundles that would like to
     * add components to the user interface. The <tt>containerID</tt> is used 
     * by the implementation to determine the place where the component should
     * be added. The <tt>containerID</tt> SHOULD be one of the CONTAINER_XXX 
     * constants. It is up to the service implementation to verify that
     * <tt>component</tt> is an instance of a class compatible with the gui
     * library used by it. If this is not the case and adding the requested
     * object would not be possible the implementation MUST through a
     * ClassCastException exception. Implementations of this service MUST
     * understand and know how to handle all ContainerID-s defined by this
     * interface, they MAY also define additional constraints. In case the
     * addComponent method is called with a <tt>containerID</tt> that the
     * implementation does not understand it MUST through a
     * java.lang.IllegalArgumentException. 
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
     * Adds the specified UI component to the container given by
     * <tt>containerID</tt> at the position specified by <tt>constraint</tt>
     * String. The method is meant to be used by plugins or bundles that would
     * like to add components to the user interface. The <tt>containerID</tt>
     * is used by the implementation to determine the place where the component
     * should be added. The <tt>containerID</tt> SHOULD be one of the
     * CONTAINER_XXX constants. The <tt>constraint</tt> String is used to
     * determine the exact position of the component in the container (LEFT,
     * RIGHT, START, etc.). The <tt>constraint</tt> String SHOULD be one of the 
     * START, END, TOP, BOTTOM, etc. String constants.
     * <br> 
     * It is up to the service implementation to verify that <tt>component</tt>
     * is an instance of a class compatible with the gui library used by it. If
     * this is not the case and adding the requested object would not be 
     * possible the implementation MUST through a ClassCastException exception.
     * Implementations of this service MUST understand and know how to handle
     * all ContainerID-s defined by this interface, they MAY also define
     * additional constraints. In case the addComponent method is called with a
     * <tt>containerID</tt> that the implementation does not understand it MUST
     * through a java.lang.IllegalArgumentException 
     * <br>
     * @param containerID One of the CONTAINER_XXX ContainerID-s.
     * @param constraint One of the START, END, BOTTOM, etc. String constants. 
     * @param component The component to be added.
     * @throws ClassCastException if <tt>component</tt> is not an
     * instance of a class supported by the service implementation. An SWT impl
     * would, for example through a ClassCastException if handed a
     * java.awt.Component
     * @throws IllegalArgumentException if the specified <tt>containerID</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all CONTAINER_XXX containerID-s.
     */
    public void addComponent(ContainerID containerID, 
                String constraint, Object component)
        throws ClassCastException, IllegalArgumentException;
    
    /**
     * Returns an iterator over a set containing containerID-s pointing to
     * containers supported by the current UI implementation. Each containerID
     * in the set is one of the CONTAINER_XXX constants. The method is meant to
     * be used by plugins or bundles that would like to add components to the 
     * user interface. Before adding any component they should use this method
     * to obtain all possible places, which could contain external components,
     * like different menus, toolbars, etc.  
     *       
     * @return Iterator An iterator to a set containing containerID-s 
     * representing all containers supported by the current UI implementation.
     */
    public Iterator getSupportedContainers();
    
    /**
     * Returns an iterator over a set of all constraints supported by the
     * given <tt>containerID</tt>. Each constraint in the set is one of the
     * START, END, TOP, BOTTOM, etc. constants. This method is meant to be used
     * to obtain all layout constraints supported by a given container.
     * 
     * @param containerID The containerID pointing to the desired container.
     * @return Iterator An iterator to a set containing all component
     * constraints
     */
    public Iterator getConstraintsForContainer(ContainerID containerID);
    
    /**
     * Returns an Iterator over a set of all components added to a given
     * constraint. Meant to be called in the process of initialization of the
     * container, defined by the given constraint in order to obtain all
     * external components that should be added in it.
     * 
     * @param containerID One of the containerID-s supported by the current UI
     * implementation.
     * @return An Iterator to a set containing all components added to a given
     * constraint.
     */
    public Iterator getComponentsForContainer(ContainerID containerID)
        throws IllegalArgumentException;
}
