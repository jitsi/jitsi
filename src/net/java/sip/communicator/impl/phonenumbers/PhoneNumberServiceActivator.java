/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.phonenumbers;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * Activates PhoneNumberI18nService implementation.
 *
 * @author Damian Minkov
 */
public class PhoneNumberServiceActivator
    implements BundleActivator
{
    /**
     * Our logging.
     */
    private static Logger logger
        = Logger.getLogger(PhoneNumberServiceActivator.class);

    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        bundleContext.registerService(
            PhoneNumberI18nService.class.getName(),
            new PhoneNumberI18nServiceImpl(),
            null);

        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[REGISTERED]");
    }

    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {}
}
