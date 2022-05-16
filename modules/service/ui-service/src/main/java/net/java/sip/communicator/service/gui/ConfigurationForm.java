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

/**
 * The <tt>ConfigurationForm</tt> interface is meant to be implemented by all
 * bundles that want to add their own specific configuration forms in the UI.
 * Each <tt>ConfigurationForm</tt> implementation could be added to the UI
 * by invoking the <code>ConfigurationDialog.addConfigurationForm</code> method.
 * <p>
 * The <tt>ConfigurationDialog</tt> for the current ui implementation could
 * be obtained by invoking <code>UIService.getConfigurationDialog</code> method.
 *
 * @author Yana Stamcheva
 */
public interface ConfigurationForm
{
    /**
     * The name of a property representing the type of the configuration form.
     */
    public static final String FORM_TYPE = "FORM_TYPE";

    /**
     * The security configuration form type.
     */
    public static final String SECURITY_TYPE = "SECURITY_TYPE";

    /**
     * The general configuration form type.
     */
    public static final String GENERAL_TYPE = "GENERAL_TYPE";

    /**
     * The advanced configuration form type.
     */
    public static final String ADVANCED_TYPE = "ADVANCED_TYPE";

    /**
     * The advanced contact source form type.
     */
    public static final String CONTACT_SOURCE_TYPE = "CONTACT_SOURCE_TYPE";

    /**
     * Returns the title of this configuration form.
     * @return the title of this configuration form
     */
    public String getTitle();

    /**
     * Returns the icon of this configuration form. It depends on the
     * UI implementation, how this icon will be used and where it will be
     * placed.
     *
     * @return the icon of this configuration form
     */
    public byte[] getIcon();

    /**
     * Returns the containing form. This should be a container with all the
     * fields, buttons, etc.
     * <p>
     * Note that it's very important to return here an object that is compatible
     * with the current UI implementation library.
     * @return the containing form
     */
    public Object getForm();

    /**
     * Returns the index of this configuration form in the configuration window.
     * This index is used to put configuration forms in the desired order.
     * <p>
     * 0 is the first position
     * -1 means that the form will be put at the end
     * </p>
     * @return the index of this configuration form in the configuration window.
     */
    public int getIndex();

    /**
     * Indicates if this is an advanced configuration form.
     * @return <tt>true</tt> if this is an advanced configuration form,
     * otherwise it returns <tt>false</tt>
     */
    public boolean isAdvanced();
}
