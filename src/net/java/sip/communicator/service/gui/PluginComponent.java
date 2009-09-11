/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>PluginComponent</tt> is an interface meant to be implemented by
 * all plugins that would like to add a user interface component to a particular
 * container in the graphical user interface (GUI). In order to appear in the
 * GUI all implementations of this interface should be registered through the
 * OSGI bundle context.
 * <p>
 * All components interested in the current contact or group that they're
 * dealing with (i.g. the one selected in the contact list for example), should
 * implement the <tt>setCurrentContact</tt> and
 * <tt>setCurrentContactGroup</tt> methods.
 * <p>
 * Note that <tt>getComponent</tt> should return a valid AWT, SWT or Swing
 * control in order to appear properly in the GUI.
 * 
 * @author Yana Stamcheva
 */
public interface PluginComponent
{
    /**
     * Returns the name of this plugin component. This name could be used as a
     * label when the component is added to a container, which requires a title.
     * A container that could request a name is for example a tabbed pane.
     * 
     * @return the name of this plugin component
     */
    public String getName();

    /**
     * Returns the identifier of the container, where we would like to add
     * our control. All possible container identifiers are defined in the
     * <tt>Container</tt> class. If the <tt>Container</tt> returned by this
     * method is not supported by the current UI implementation the plugin won't
     * be added.
     * 
     * @return the container, where we would like to add our control.
     */
    public Container getContainer();

    /**
     * Returns the constraints, which will indicate to the container, where this
     * component should be added. All constraints are defined in the Container
     * class and are as follows: START, END, TOP, BOTTOM, LEFT, RIGHT.
     * 
     * @return the constraints, which will indicate to the container, where this
     * component should be added.
     */
    public String getConstraints();

    /**
     * Returns the index position of this component in the container, where it
     * will be added. An index of 0 would mean that this component should be
     * added before all other components. An index of -1 would mean that the
     * position of this component is not important.
     * @return the index position of this component in the container, where it
     * will be added.
     */
    public int getPositionIndex();

    /**
     * Returns the component that should be added. This method should return a
     * valid AWT, SWT or Swing object in order to appear properly in the user
     * interface.
     * 
     * @return the component that should be added.
     */
    public Object getComponent();

    /**
     * Sets the current contact. Meant to be used by plugin components that
     * are interested of the current contact. The current contact is the contact 
     * for the currently selected chat transport.
     * 
     * @param contact the current contact
     */
    public void setCurrentContact(Contact contact);
    
    /**
     * Sets the current meta contact. Meant to be used by plugin components that
     * are interested of the current contact. The current contact could be the
     * contact currently selected in the contact list or the contact for the
     * currently selected chat, etc. It depends on the container, where this
     * component is meant to be added.
     * 
     * @param metaContact the current meta contact
     */
    public void setCurrentContact(MetaContact metaContact);

    /**
     * Sets the current meta group. Meant to be used by plugin components that
     * are interested of the current meta group. The current group is always
     * the currently selected group in the contact list. If the group passed
     * here is null, this means that no group is selected.
     * 
     * @param metaGroup the current meta contact group
     */
    public void setCurrentContactGroup(MetaContactGroup metaGroup);

    /**
     * Returns <code>true</code> to indicate that this component is a native
     * component and <code>false</code> otherwise. This method is meant to be
     * used by containers if a special treatment is needed for native components.
     * 
     * @return <code>true</code> to indicate that this component is a native
     * component and <code>false</code> otherwise.
     */
    public boolean isNativeComponent();
}
