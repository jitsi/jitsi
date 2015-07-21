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
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * The BLF source contact.
 * @author Damian Minkov
 */
public class BLFSourceContact
    extends GenericSourceContact
{
    private final OperationSetTelephonyBLF.Line line;

    /**
     * Initializes a new <tt>AddrBookSourceContact</tt> instance.
     *
     * @param contactSource  the <tt>ContactSourceService</tt> which is creating
     *                       the new instance
     */
    public BLFSourceContact(ContactSourceService contactSource,
                            OperationSetTelephonyBLF.Line line)
    {
        super(contactSource,
            line.getName() != null ? line.getName() : line.getAddress(),
            new ArrayList<ContactDetail>());

        this.line = line;
    }

    /**
     * Returns the line displayed.
     * @return
     */
    public OperationSetTelephonyBLF.Line getLine()
    {
        return line;
    }
}
