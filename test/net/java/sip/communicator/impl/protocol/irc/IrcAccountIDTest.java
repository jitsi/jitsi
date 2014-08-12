package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

public class IrcAccountIDTest
    extends TestCase
{

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructNullHost()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        try
        {
            IrcAccountID account =
                new IrcAccountID("user", null, "6667", properties);
            Assert.fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
            // this works as expected
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void testConstructNullPort()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        try
        {
            IrcAccountID account =
                new IrcAccountID("user", "host", null, properties);
            Assert.fail("Should have failed with IAE.");
        }
        catch (IllegalArgumentException e)
        {
            // this works as expected
        }
    }

    public void testCorrectConstruction()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertEquals("user", account.getUserID());
        Assert.assertEquals("host", account.getHost());
        Assert.assertEquals(6667, account.getPort());
    }

    public void testEqualsSame()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertTrue(account.equals(account));
    }

    public void testEqualsNull()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertFalse(account.equals(null));
    }

    public void testEqualsOtherClassInstance()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertFalse(account.equals(new Object()));
    }

    public void testEqualsOtherUser()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("other-user", "host", "6667", properties);
        Assert.assertFalse(account.equals(account2));
    }

    public void testEqualsOtherHost()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("user", "other-host", "6667", properties);
        Assert.assertFalse(account.equals(account2));
    }

    public void testEqualsOtherPort()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("user", "host", "6697", properties);
        Assert.assertFalse(account.equals(account2));
    }

    public void testHashCodeExecutes()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        // only test that it does not throw an exception
        account.hashCode();
    }
}
