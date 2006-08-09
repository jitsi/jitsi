/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

import junit.framework.*;
import java.util.*;
import net.java.sip.communicator.util.*;

/**
 * Performs testing on protocol provider methods.
 * @todo add more detailed docs once the tests are written.
 * @author Emil Ivov
 */
public class TestProtocolProviderServiceSipImpl
    extends TestCase
{
    private static final Logger logger =
        Logger.getLogger(TestProtocolProviderServiceSipImpl.class);

    private SipSlickFixture fixture = null;

    /**
     * Creates a test encapsulator for the method with the specified name.
     * @param name the name of the method this test should run.
     */
    public TestProtocolProviderServiceSipImpl(String name)
    {
        super(name);
    }

    /**
     * Initializes the fixture.
     * @throws Exception if super.setUp() throws one.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * Tears the fixture down.
     * @throws Exception if fixture.tearDown() fails.
     */
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }


    /**
     * Verifies that all operation sets have the type they are declarded to
     * have.
     *
     * @throws java.lang.Exception if a class indicated in one of the keys
     * could not be forName()ed.
     */
    public void testOperationSetTypes() throws Exception
    {
        Map supportedOperationSets
            = fixture.provider1.getSupportedOperationSets();

        //make sure that keys (which are supposed to be class names) correspond
        //what the class of the values recorded against them.
        Iterator setNames = supportedOperationSets.keySet().iterator();
        while (setNames.hasNext())
        {
            String setName = (String) setNames.next();
            Object opSet = supportedOperationSets.get(setName);

            assertTrue(opSet + " was not an instance of "
                       + setName + " as declared"
                       , Class.forName(setName).isInstance(opSet));
        }
    }

}
