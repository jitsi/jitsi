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

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>PluginComponent</tt> is an interface meant to be implemented by
 * all plugins that would like to add a user interface component to a particular
 * container in the graphical user interface (GUI). In order to appear in the
 * GUI all implementations of this interface should be registered through the
 * OSGI bundle context using <tt>PluginComponentFactory</tt>.
 * <p>
 * All components interested in the current contact or group that they're
 * dealing with (i.g. the one selected in the contact list for example), should
 * implement the <tt>setCurrentContact</tt> and
 * <tt>setCurrentContactGroup</tt> methods.
 * <p>
 * <p>
 * All components interested in the current account that they're dealing
 * with (i.g. the one selected in the account list for example), should
 * implement the <tt>setCurrentAccountID</tt> method.
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
     * Returns the component that should be added. This method should return a
     * valid AWT, SWT or Swing object in order to appear properly in the user
     * interface.
     *
     * @return the component that should be added.
     */
    public Object getComponent();

    /**
     * Returns the position of this <tt>PluginComponent</tt> within its
     * <tt>Container</tt>
     * 
     * @return The position of this <tt>PluginComponent</tt> within its
     * <tt>Container</tt>
     */
    public int getPositionIndex();

    /**
     * Sets the current contact. Meant to be used by plugin components that
     * are interested of the current contact. The current contact is the contact
     * for the currently selected chat transport.
     *
     * @param contact the current contact
     */
    public void setCurrentContact(Contact contact);

    /**
     * Sets the current contact. Meant to be used by plugin components that
     * are interested of the current contact. The current contact is the contact
     * for the currently selected chat transport.
     *
     * @param contact the current contact
     * @param resourceName the <tt>ContactResource</tt> name. Some components
     * may be interested in a particular ContactResource of a contact.
     */
    public void setCurrentContact(Contact contact, String resourceName);

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
     * Sets the current AccountID. Meant to be used by plugin components that are
     * interested in the current AccountID. The current AccountID could be that
     * of a currently selected account in the account list. It depends on the
     * container, where this component is meant to be added.
     *
     * @param account the current account.
     */
    public void setCurrentAccountID(AccountID accountID);

    /**
     * Returns the factory that has created the component.
     * @return the parent factory.
     */
    public PluginComponentFactory getParentFactory();
}
