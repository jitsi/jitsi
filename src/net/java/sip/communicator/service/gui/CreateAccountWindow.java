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
 *
 * @author Yana Stamcheva
 */
public interface CreateAccountWindow
{
    /**
     * Shows or hides this create account window.
     *
     * @param visible <tt>true</tt> to show this window, <tt>false</tt> -
     * otherwise
     */
    public void setVisible(boolean visible);

    /**
     * Sets the selected wizard.
     *
     * @param wizard the wizard to select
     * @param isCreatedForm indicates if the selected wizard should be opened
     * in create account mode
     */
    public void setSelectedWizard(  AccountRegistrationWizard wizard,
                                    boolean isCreateAccount);
}
