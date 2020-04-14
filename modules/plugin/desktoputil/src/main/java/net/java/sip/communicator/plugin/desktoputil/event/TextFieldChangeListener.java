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
package net.java.sip.communicator.plugin.desktoputil.event;

/**
 * The <tt>TextFieldChangeListener</tt> listens for any changes in the text
 * contained in a <tt>SIPCommTextField</tt>. It is notified every time a char
 * is inserted or removed from the field.
 *
 * @author Yana Stamcheva
 */
public interface TextFieldChangeListener
{
    /**
     * Indicates that a text has been removed from the text field.
     */
    public void textRemoved();

    /**
     * Indicates that a text has been inserted to the text field.
     */
    public void textInserted();
}
