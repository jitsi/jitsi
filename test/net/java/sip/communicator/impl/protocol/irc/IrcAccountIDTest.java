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

    public void testServiceNameMinimal()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertEquals(ProtocolNames.IRC, account.getService());
        Assert.assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }

    public void testServiceNameWithHost()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        IrcAccountID account =
            new IrcAccountID("user", "localhost", "6667", properties);
        Assert.assertEquals("localhost", account.getService());
        Assert.assertEquals("user@localhost:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }

    public void testServiceNameWithHostAndPort()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertEquals("localhost:6667", account.getService());
        Assert.assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
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

    public void testAccountDisplayNamePropertySet()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        properties.put(ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
            "my-IRC-account-name");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertEquals("my-IRC-account-name", account.getDisplayName());
    }

    public void testAccountDisplayNamePropertySetButEmpty()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        properties.put(ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME, "");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }
}
