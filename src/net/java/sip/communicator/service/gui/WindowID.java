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
 * The <tt>WindowID</tt> wraps a string which is meant to point to an
 * application dialog, like per example a "Configuration" dialog or
 * "Add contact" dialog.
 *
 * @author Yana Stamcheva
 */
public class WindowID{

    private String dialogName;

    /**
     * Creates a new WindowID.
     * @param dialogName the name of the dialog
     */
    public WindowID(String dialogName){
        this.dialogName = dialogName;
    }

    /**
     * Get the ID.
     *
     * @return the ID
     */
    public String getID(){
        return this.dialogName;
    }
}
