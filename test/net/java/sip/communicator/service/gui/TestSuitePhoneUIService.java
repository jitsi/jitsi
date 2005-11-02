/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Runs all PhoneUIService unit tests.
 *
 * @author Emil Ivov
 */
public class TestSuitePhoneUIService
    extends TestCase
{

    public TestSuitePhoneUIService(String s)
    {
        super(s);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        return suite;
    }
}
