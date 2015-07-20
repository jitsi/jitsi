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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

/**
 * The <tt>JabberAccountCreationFormService</tt> is meant to be implemented by
 * specific account registration implementations, which contain an account
 * create form.
 *
 * @author Yana Stamcheva
 */
public interface JabberAccountCreationFormService
{
    /**
     * Creates an account for a specific server.
     * @return the new account
     */
    public NewAccount createAccount();

    /**
     * Returns the form, which would be used by the user to create a new
     * account.
     * @return the component of the form
     */
    public Component getForm();

    /**
     * Clears all the data previously entered in the form.
     */
    public void clear();
}
