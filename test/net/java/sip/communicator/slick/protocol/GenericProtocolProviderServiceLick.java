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
package net.java.sip.communicator.slick.protocol;

import junit.framework.*;

import org.osgi.framework.*;

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
