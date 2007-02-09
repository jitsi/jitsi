/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
/**
 * The <tt>UIService</tt> offers generic access to the graphical user interface
 * for all modules that would like to interact with the user.
 * <p>
 * Through the <tt>UIService</tt> all modules can add their own components in
 * different menus, toolbars, etc. within the ui. Each <tt>UIService</tt>
 * implementation should export its supported "plugable" containers - a set of
 * <tt>ContainerID</tt>s corresponding to different "places" in the application,
 * where a module can add a component.
 * <p>
 * The <tt>UIService</tt> provides also methods that would allow to other
 * modules to control the visibility, size and position of the main application
 * window. Some of these methods are: setVisible, minimize, maximize, resize,
 * move, etc. 
 * <p>
 * A way to show different types of simple windows is provided to allow other
 * modules to show different simple messages, like warning or error messages.
 * In order to show a simple warning message, a module should invoke the 
 * getPopupDialog method and then one of the showXXX methods, which corresponds
 * best to the required dialog. 
 * <p>
 * Certain application windows within the GUI, like "AddContact" for example,
 * could be also shown from outside the ui. To make one of
 * these windows showable, the <tt>UIService</tt> implementation should attach
 * to it a <tt>WindowID</tt>. A window then could be shown, by
 * invoking <code>getApplicationWindow(WindowID)</code> and then 
 * <code>show</code>. The <tt>WindowID</tt> above should be one of the exported
 * <tt>WindowID</tt>s obtained from <code>getApplicationWindows</code>.
 * <p>
 * Each <code>UIService</code> implementation should implement the method
 * <code>getChatWindow(Contact contact)</code>, which is meant to provide an
 * access to the chat component for the given contact in the form of 
 * <code>ApplicationWindow</code>.
 * 
 * @author Yana Stamcheva
 */
public interface UIService
{
    /*
     * ContainerID-s
     */
    /**
     * Main application window "file menu" container.
     */
    public static final ContainerID CONTAINER_FILE_MENU 
        = new ContainerID("File");
    /**
     * Main application window "tools menu" container.
     */
    public static final ContainerID CONTAINER_TOOLS_MENU 
        = new ContainerID("Tools");
    /**
     * Main application window "view menu" container.
     */
    public static final ContainerID CONTAINER_VIEW_MENU 
        = new ContainerID("View");
    /**
     * Main application window "help menu" container.
     */    
    public static final ContainerID CONTAINER_HELP_MENU 
        = new ContainerID("Help");
    /**
     * Main application window "settings menu" container.
     */
    public static final ContainerID CONTAINER_SETTINGS_MENU 
        = new ContainerID("Settings");
    /**
     * Main application window main toolbar container.
     */
    public static final ContainerID CONTAINER_MAIN_TOOL_BAR 
        = new ContainerID("MainToolBar");
    /**
     * Chat window toolbar container.
     */
    public static final ContainerID CONTAINER_CHAT_TOOL_BAR 
        = new ContainerID("ChatToolBar");
    /**
     * Main application window "right button menu" over a contact container.
     */
    public static final ContainerID CONTAINER_CONTACT_RIGHT_BUTTON_MENU
        = new ContainerID("ContactRightButtonMenu");
    
    /**
     * Main application window "right button menu" over a group container.
     */
    public static final ContainerID CONTAINER_GROUP_RIGHT_BUTTON_MENU
        = new ContainerID("GroupRightButtonMenu");
        
    /**
     * Chat window "menu bar" container.
     */
    public static final ContainerID CONTAINER_CHAT_MENU_BAR 
        = new ContainerID("ChatMenuBar");
    /**
     * Chat window "file menu" container.
     */
    public static final ContainerID CONTAINER_CHAT_FILE_MENU 
        = new ContainerID("ChatFileMenu");
    /**
     * Chat window "edit menu" container.
     */
    public static final ContainerID CONTAINER_CHAT_EDIT_MENU 
        = new ContainerID("ChatEditMenu");
    /**
     * Chat window "settings menu" container.
     */
    public static final ContainerID CONTAINER_CHAT_SETTINGS_MENU 
        = new ContainerID("ChatSettingsMenu");
    /**
     * Chat window "help menu" container.
     */
    public static final ContainerID CONTAINER_CHAT_HELP_MENU 
        = new ContainerID("ChatHelpMenu");
        
    /*
     * Constraints
     */
    /**
     * Indicates the most left/top edge of a container.
     */
    public static final String START = "Start";
    /**
     * Indicates the most right/bottom edge of a container.
     */
    public static final String END = "End";
    /**
     * Indicates the top edge of a container.
     */
    public static final String TOP = "Top";
    /**
     * Indicates the bottom edge of a container.
     */
    public static final String BOTTOM = "Bottom";
    /**
     * Indicates the left edge of a container.
     */
    public static final String LEFT = "Left";
    /**
     * Indicates the right edge of a container.
     */
    public static final String RIGHT = "Right";
  
    /*
     * WindowID-s
     */    
    
    public static final WindowID WINDOW_ADD_CONTACT
        = new WindowID("AddContactWindow");
    
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
     * Returns a common application window given by WindowID. This 
     * could be for example an "Add contact" window or any other window within
     * the application, which could be simply shown without need of additional
     * arguments. The <tt>windowID</tt> SHOULD be one of the WINDOW_XXX obtained
     * by the getApplicationWindows method.
     *  
     * @param windowID One of the WINDOW_XXX WindowID-s.
     * @throws IllegalArgumentException if the specified <tt>windowID</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all WINDOW_XXX ID-s.
     * @return the window to be shown
     * @see #getApplicationWindows()
     */
    public ApplicationWindow getApplicationWindow(WindowID windowID)
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
     * Returns the <tt>ApplicationWindow</tt> corresponding to the component
     * representing the chat for the given contact. Meant to be used from other
     * bundles to allow them to check the visibility of a chat, hide it or show
     * it.
     * @param contact
     * @return The <tt>ApplicationWindow</tt> corresponding to the component
     * representing the chat for the given contact.
     */
    public ApplicationWindow getChatWindow(Contact contact);
    
    /**
     * Returns the <tt>ConfigurationWindow</tt> implementation for this
     * UIService implementation. The <tt>ConfigurationWindow</tt> is a
     * contianer contianing <tt>ConfigurationForm</tt>s. It is meant to be
     * implemented by the UIService implementation to provide a mechanism
     * for adding and removing configuration forms in the GUI. 
     * 
     * @return the <tt>ConfigurationWindow</tt> implementation for this
     * UIService implementation
     */
    public ConfigurationWindow getConfigurationWindow();
    
    /**
     * Returns an iterator over a set of windowID-s. Each WindowID points to
     * a common window in the current UI implementation. Each WindowID in the
     * set is one of the WINDOW_XXX constants. The method is meant to be used
     * by bundles that would like to show common windows like "Add contact" per
     * example. Before showing any window they should use this method to obtain
     * all possible windows, which could be shown for the current ui
     * implementation.  
     *       
     * @return Iterator An iterator to a set containing windowID-s 
     * representing all windows supported by the current UI implementation.
     */
    public Iterator getApplicationWindows();
    
    /**
     * Chechks if the application window with the given <tt>WindowID</tt> is
     * contained in the current UI implementation.
     * 
     * @param windowID One of the WINDOW_XXX WindowID-s. 
     * @return <code>true</code> if the application window with the given 
     * <tt>WindowID</tt> is exported from the current UI implementation,
     * <code>false</code> otherwise.
     */
    public boolean containsApplicationWindow(WindowID windowID);
    
    /**
     * Returns the <tt>AccountRegistrationWizardContainer</tt> for the current
     * UIService implementation. The <tt>AccountRegistrationWizardContainer</tt>
     * is meant to be implemented by the UI service implementation in order to
     * allow other modules to add to the GUI <tt>AccountRegistrationWizard</tt>
     * s. Each of these wizards is made for a given protocol and should provide
     * a sequence of user interface forms through which the user could
     * registrate a new account.
     * 
     * @return Returns the <tt>AccountRegistrationWizardContainer</tt> for the
     * current UIService implementation.
     */
    public AccountRegistrationWizardContainer getAccountRegWizardContainer();
    
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
     * Adds the specified UI component to the container given by ContainerID. 
     * The method is meant to be used by plugins or bundles that would like to
     * add components to the user interface. The <tt>containerID</tt> is used 
     * by the implementation to determine the place where the component should
     * be added. The <tt>containerID</tt> SHOULD be one of the CONTAINER_XXX 
     * constants.
     * <br>
     * The <tt>ContactAwareComponent</tt> is a plugin component that
     * is interested of the current meta contact in the container.
     * <br>
     * Implementations of this service MUST understand and know how to handle
     * all ContainerID-s defined by this interface, they MAY also define
     * additional constraints. In case the addComponent method is called with a
     * <tt>containerID</tt> that the implementation does not understand it MUST
     * through a java.lang.IllegalArgumentException. 
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
    public void addComponent(ContainerID containerID,
        ContactAwareComponent component)
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
     * The <tt>ContactAwareComponent</tt> is a plugin component that
     * is interested of the current meta contact in the container. 
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
                String constraint, ContactAwareComponent component)
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
     * Chechks if the container with the given <tt>ContainerID</tt> is supported
     * from the current UI implementation.
     * 
     * @param containderID One of the CONTAINER_XXX ContainerID-s. 
     * @return <code>true</code> if the contaner with the given 
     * <tt>ContainerID</tt> is supported from the current UI implementation,
     * <code>false</code> otherwise.
     */
    public boolean isContainerSupported(ContainerID containderID);
        
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
