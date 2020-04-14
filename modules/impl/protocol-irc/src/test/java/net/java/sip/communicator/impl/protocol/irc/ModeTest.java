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

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;
import org.junit.*;

public class ModeTest
{
    @Test
    public void testGetSymbol()
    {
        assertEquals('o', Mode.OPERATOR.getSymbol());
    }

    @Test
    public void testGetRole()
    {
        assertTrue(Mode.OPERATOR.getRole() instanceof ChatRoomMemberRole);
    }

    @Test
    public void testGetBySymbol() throws UnknownModeException
    {
        Assert.assertSame(Mode.OPERATOR, Mode.bySymbol('o'));
    }

    @Test(expected = UnknownModeException.class)
    public void testGetBySymbolNonExisting() throws UnknownModeException
    {
        Mode.bySymbol('&');
        Assert.fail("Expected UnknownModeException");
    }
}
