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

import static org.junit.Assert.*;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import org.junit.*;

public class IrcAccountIDTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testConstructNullHost()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        new IrcAccountID("user", null, "6667", properties);
        fail("Should have failed with IAE.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructNullPort()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        new IrcAccountID("user", "host", null, properties);
        fail("Should have failed with IAE.");
    }

    @Test
    public void testServiceNameMinimal()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals(ProtocolNames.IRC, account.getService());
        assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }

    @Test
    public void testServiceNameWithHost()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        IrcAccountID account =
            new IrcAccountID("user", "localhost", "6667", properties);
        assertEquals("localhost", account.getService());
        assertEquals("user@localhost:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }

    @Test
    public void testServiceNameWithHostAndPort()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals("localhost:6667", account.getService());
        assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }

    @Test
    public void testCorrectConstruction()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals("user", account.getUserID());
        assertEquals("host", account.getHost());
        assertEquals(6667, account.getPort());
    }

    @Test
    public void testEqualsSame()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals(account, account);
    }

    @Test
    public void testEqualsNull()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertNotEquals(null, account);
    }

    @Test
    public void testEqualsOtherClassInstance()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        Assert.assertNotEquals(account, new Object());
    }

    @Test
    public void testEqualsOtherUser()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("other-user", "host", "6667", properties);
        Assert.assertNotEquals(account, account2);
    }

    @Test
    public void testEqualsOtherHost()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("user", "other-host", "6667", properties);
        Assert.assertNotEquals(account, account2);
    }

    @Test
    public void testEqualsOtherPort()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        IrcAccountID account2 =
            new IrcAccountID("user", "host", "6697", properties);
        Assert.assertNotEquals(account, account2);
    }

    @Test
    public void testHashCodeExecutes()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        // only test that it does not throw an exception
        account.hashCode();
    }

    @Test
    public void testAccountDisplayNamePropertySet()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        properties.put(ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME,
            "my-IRC-account-name");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals("my-IRC-account-name", account.getDisplayName());
    }

    @Test
    public void testAccountDisplayNamePropertySetButEmpty()
    {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.IRC);
        properties.put(ProtocolProviderFactory.SERVER_ADDRESS, "localhost");
        properties.put(ProtocolProviderFactory.SERVER_PORT, "6667");
        properties.put(ProtocolProviderFactory.ACCOUNT_DISPLAY_NAME, "");
        IrcAccountID account =
            new IrcAccountID("user", "host", "6667", properties);
        assertEquals("user@host:6667 (" + ProtocolNames.IRC + ")",
            account.getDisplayName());
    }
}
