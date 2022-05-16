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
package net.java.sip.communicator.impl.phonenumbers;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.osgi.DependentActivator;
import org.jitsi.service.configuration.ConfigurationService;
import org.osgi.framework.*;

/**
 * Activates PhoneNumberI18nService implementation.
 *
 * @author Damian Minkov
 */
public class PhoneNumberServiceActivator extends DependentActivator
{
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PhoneNumberServiceActivator.class);

    public PhoneNumberServiceActivator()
    {
        super(ConfigurationService.class);
    }

    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        bundleContext.registerService(
            PhoneNumberI18nService.class,
            new PhoneNumberI18nServiceImpl(
                getService(ConfigurationService.class)),
            null);

        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[REGISTERED]");
    }
}
