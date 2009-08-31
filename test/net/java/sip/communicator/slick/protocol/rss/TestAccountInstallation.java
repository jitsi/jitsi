/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;
import junit.framework.*;
import java.util.*;

/**
 * Installs a test account and verifies it is available after installation.
 *
 * @author Mihai Balan
 */
public class TestAccountInstallation
    extends TestCase
{

    /**
     * Creates a test with the specified method name.
     * @param name the name of the method tu execute.
     */
    public TestAccountInstallation(String name)
    {
        super(name);
    }

    /**
     * JUnit setup method.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * JUnit cleanup method.
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Installs and account and verifies whether the installation succedded.
     */
    public void testInstallAccount()
    {
        Hashtable<String, String> accountProperties = new Hashtable<String, String>();

        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL + "="
            + ProtocolNames.RSS + ")";

        accountProperties.put(ProtocolProviderFactory.USER_ID, "RSS");

        try {
            serRefs = RssSlickFixture.bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        } catch (InvalidSyntaxException ise)
        {
            //shouldn't happen as the filter is static
            fail(osgiFilter + "is not a valid filter");
        }

        //couldn't find a provider factory service.
        assertTrue("Failed to find a provider factory service or protocol RSS",
            serRefs != null && serRefs.length > 0);

        ProtocolProviderFactory rssProviderFactory = (ProtocolProviderFactory)
            RssSlickFixture.bc.getService(serRefs[0]);

        //there shouldn't be any account installed
        assertTrue("There was an account already registered with the account "
                + "manager",
            rssProviderFactory.getRegisteredAccounts().size() == 0);

        try {
            rssProviderFactory.installAccount(null, accountProperties);
            fail("Installing an account with a null account id must result in a"
                + " NullPointerException!");
        } catch(NullPointerException npe)
        {
            //that's ought to happen
        }

        rssProviderFactory.installAccount("RSS", accountProperties);

        //try to install the same account twice and check for exceptions
        try {
            rssProviderFactory.installAccount("RSS", accountProperties);
            fail("An IllegalStateException must be thrown when trying to "
                + "install a duplicate account.");
        } catch(IllegalStateException ise)
        {
            //that's ought to happen
        }

        assertTrue("Newly installed account is not in the account manager's "
            + "registered accounts!",
            rssProviderFactory.getRegisteredAccounts().size() == 1);

        osgiFilter = "(&(" + ProtocolProviderFactory.PROTOCOL + "="
            + ProtocolNames.RSS + ")"
            + "(" + ProtocolProviderFactory.USER_ID + "=RSS))";

        try {
            serRefs = RssSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(), osgiFilter);
        } catch(InvalidSyntaxException ise)
        {
            fail(osgiFilter + " is not a valid filter");
        }

        assertTrue("A protocol provider was apparently not installed as"
                + " requested",
            serRefs != null && serRefs.length > 0);

        Object rssProtocolProvider =
            RssSlickFixture.bc.getService(serRefs[0]);

        assertTrue("The installed protocol provider does not implement the"
            + "protocol provider service.",
            rssProtocolProvider instanceof ProtocolProviderService);
    }
}
