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
 * The <tt>FavoriteButton</tt> interface is meant to be used by plugins in order
 * to register their own components in the menu of favorites opened, by clicking
 * the arrow button above the contact list.
 *
 * @author Yana Stamcheva
 */
public interface FavoritesButton
{
    /**
     * Returns the image to be set on the favorites button.
     *
     * @return the image to be set on the favorites button.
     */
    public byte[] getImage();

    /**
     * Returns the text to be set to the favorites button.
     *
     * @return the text to be set to the favorites button.
     */
    public String getText();

    /**
     * This method will be called when one clicks on the button.
     */
    public void actionPerformed();
}
