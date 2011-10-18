/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol;

import org.osgi.framework.*;

import junit.framework.*;

/**
 * The generic protocol provider SLICK implements a set of tests that any
 * protocol provider implementation should pass. It does not contain any
 * protocol specific code and only uses generic calls to methods defined by
 * the ProtocolProviderService.
 * <p>
 * Protocol specific tests are implemented from some protocols in
 * slick.protocol.<protocol_name> subpackages.
 * <p>
 * Tests in this SLICK use accounts defined in the accounts xml file and
 * create two instances of the tested provider making sure that they can see
 * each other and communicate properly.
 * <p>
 * @author Emil Ivov
 */
public class GenericProtocolProviderServiceLick
    extends TestCase
    implements BundleActivator
{

    /**
     * Registers generic protocol test suits for currently registered protocol
     * providers.
     *
     * @param context A currently valid bundle context
     */
    public void start(BundleContext context)
    {

    }

    /**
     * Prepares the SLICK for shutdown.
     *
     * @param context a valid OSGI bundle context.
     */
    public void stop(BundleContext context)
    {

    }
}
