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
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

/**
 * A convenience class used to store combobox complex objects.
 * The <tt>SelectedObject</tt> is used for all account and status combo boxes
 * throughout this gui implementation.
 *
 * @author Yana Stamcheva
 */
public class SelectedObject
{
    private String text;

    private Icon icon;

    private Object object;

    /**
     * Creates an instance of <tt>SelectedObject</tt> by specifying the text,
     * icon and object associated with it.
     *
     * @param text The text.
     * @param icon The icon.
     * @param object The object.
     */
    public SelectedObject(String text, Icon icon, Object object)
    {
        this.text = text;
        this.icon = icon;
        this.object = object;
    }

    /**
     * Creates an instance of <tt>SelectedObject</tt> by specifying the
     * icon and object associated with it.
     *
     * @param icon The icon.
     * @param object The object.
     */
    public SelectedObject(Icon icon, Object object)
    {
        this.icon = icon;
        this.object = object;
    }

    /**
     * Returns the text of this <tt>SelectedObject</tt>.
     * @return the text of this <tt>SelectedObject</tt>.
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns the icon of this <tt>SelectedObject</tt>.
     * @return the icon of this <tt>SelectedObject</tt>.
     */
    public Icon getIcon()
    {
        return icon;
    }

    /**
     * Returns the real object behind this <tt>SelectedObject</tt>.
     * @return the real object behind this <tt>SelectedObject</tt>.
     */
    public Object getObject()
    {
        return object;
    }
}
