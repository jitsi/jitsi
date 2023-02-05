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
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.util.*;

import net.java.sip.communicator.plugin.jabberaccregwizz.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>GoogleTalkAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lubomir Marinov
 */
public class GoogleTalkAccRegWizzActivator
    extends JabberAccRegWizzActivator
{
    /**
     * Starts this bundle.
     */
    @Override
    public void init(BundleContext context)
    {
        UIService uiService = getService(UIService.class);

        GoogleTalkAccountRegistrationWizard wizard =
            new GoogleTalkAccountRegistrationWizard(uiService
                .getAccountRegWizardContainer());

        Hashtable<String, String> containerFilter = new Hashtable<>();
        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                GoogleTalkAccountRegistrationWizard.PROTOCOL);

        context.registerService(
            AccountRegistrationWizard.class,
            wizard,
            containerFilter);
    }
}
