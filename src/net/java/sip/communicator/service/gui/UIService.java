/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.protocol.*;

/**
 * The UIService provides an interface towards towards that part of the
 * SIP Communicator that interacts with users and offers them a way to make,
 * accept, manage or handup calls.
 *
 * In order for the UIService to support a communications protocol, a
 * Provider implementation of the specified protocol needs to be registered with
 * the service.
 *
 * Plugins using the service in order to add featuress to the user interface
 * may do that using one of the addXxx() methods.
 *
 *
 * @author Emil Ivov
 */
public interface UIService
{
    public static final String COMPONENT_CONSTRAINT_MENU_FILE = "File";
    public static final String COMPONENT_CONSTRAINT_MENU_VIEW = "View";
    public static final String COMPONENT_CONSTRAINT_MENU_TOOLS = "Tools";
    public static final String COMPONENT_CONSTRAINT_MENU_SETTINGS = "Settings";
    public static final String COMPONENT_CONSTRAINT_MENU_HELP = "Help";
    public static final String COMPONENT_CONSTRAINT_MENU_CALL = "Call";
    public static final String COMPONENT_CONSTRAINT_MENU_ACCOUNT = "Account";
    public static final String COMPONENT_CONSTRAINT_MENU_CONTACT = "Contact";
    public static final String COMPONENT_CONSTRAINT_MENU_BAR = "Menu Bar";

    public static final String UI_LIB_SWING = "Swing";
    public static final String UI_LIB_SWT   = "SWT";
    public static final String UI_LIB_AWT   = "AWT";

    /**
     * Registers the specified telephony provider with the user interface. The
     * PhoneUIService implementation will automatically add itself as a state
     * listener of the specified provider and react in accordance with states
     * coming from that listener. It will also deliver all relevant user
     * call control requests to that provider.
     * @param provider the provider to register.
     */
    public void registerProvider(ProtocolProviderService provider);

    /**
     * Specifies whether or not the phone ui should be visible (In case for
     * example we'd only like a sys tray icon or a contact list to show).
     * @param visible a boolean specifying whether the phone ui should be visible
     */
    public void setVisible(boolean visible);

    //services offered to bundles/plugins that would like to interact with the
    //gui.
    /**
     * Returns an array of Call objects containing the current set of ongoing
     * calls. One could obtain specific call participants through by querying
     * Call methods
     * @return an array of Call objects reflecting on-going calls.
     */
    public Call[] getActiveCalls();

    /**
     * Returns the name of the library used to implement the service. In case
     * the implementation is using Swing, SWT, or pure AWT it MUST return one of
     * the UI_LIB_XXX constants.  The method may be used by plugins that would
     * like to retrieve ui components
     * @return String
     */
    public String getUiLibName();

    /**
     * Returns an array of UI lib names indicating that the implementation is
     * able to handle components registered by external plugins/bungles in case
     * they are implemented using one of the returned lib names. The lib names
     * returned by this method should be one or more of the UI_LIB_XXX constants,
     * but callers of this method must properly handle unknown Strings which might
     * be returned by implementations of future versions of this service.
     *
     * @return an array containing one or more UI_LIB_XXX constants.
     */
    public String[] getSupportedUiLibNames();

    /**
     * Adds the specified menuItem to the specified parent menu. The parent String
     * MUST be one of the MENU_XXX constants. It is up to the service
     * implementation to verify that "menuItem" is an instance of a class
     * compatible with the gui library used by it. If this is not the case and
     * adding the requested object would not be possible the implementation
     * MUST through an exception.
     *
     * @param parent one of the MENU_XXX string constants indicating the parent
     *        menu that this menuItem should be added to.
     * @param menuItem the item to add.
     * @throws ClassCastException if the menuItem is an
     * instance of a class not supported by the service implementation.
     * @throws IllegalArgumentException if the specified parent is not
     * recognized by the implementation (note that implementations MUST properly
     * handle all MENU_XXX strings as eventual parents even if they do not
     * correspond to a menu with the same title and may be organized at the will
     * of the implementor).
     */
    public void addMenuItem(String parent, Object menuItem)
        throws ClassCastException, IllegalArgumentException;

    /**
     * Adds the specified UI component to the user interface according to the
     * provided string constraint. The method is meant to be used by plugins or
     * bundles that would like to add components to the user interface. The
     * <tt>constraint</tt> string is used by the implementation to determine
     * the place where the component should be added. The <tt>constraint</tt>
     * String SHOULD be one of the COMPONENT_CONSTRAINT_XXX constants. It is up
     * to the service implementation to verify that <tt>component</tt> is an
     * instance of a class compatible with the gui library used by it. If this
     * is not the case and adding the requested object would not be possible the
     * implementation MUST through a ClassCastException exception.
     * Implementations of this service MUST understant and know how to handle
     * all COMPONENT_CONSTRAINT_XXX Strings defined by this interface, they
     * MAY also define additional constraints. In case the addComponent method
     * is called with a <tt>constraint</tt> that the implementation does
     * not understand it MUST through a java.lang.IllegalArgumentException <br>
     * <br>
     * @param component the component we'd like to add
     * @param constraint a String (possibly one of the COMPONENT_CONSTRAINT_XXX
     * strings) indicating the place where the component should be added.
     * @throws ClassCastException if <tt>component</tt> is not an
     * instance of a class supported by the service implementation. An SWT impl
     * would, for example through a ClassCastException if handed a
     * java.awt.Component
     * @throws IllegalArgumentException if the specified <tt>constraint</tt>
     * is not recognized by the implementation (note that implementations
     * MUST properly handle all COMPONENT_CONSTRAINT_XXX strings.
     */
    public void addComponent(Object component, String constraint)
        throws ClassCastException, IllegalArgumentException;

    public void addUserActionListener();

    //========================== CONFIG ======================================
    //maybe add a method to show the config dialog

    //========================== AuthenticationService =======================
    //these should probably go to a different service
    public void requestAuthentication(String realm, String userName,
                                      char[] password);
    public String getAuthenticationUserName();

    //get main frame (for dialogs)
}
