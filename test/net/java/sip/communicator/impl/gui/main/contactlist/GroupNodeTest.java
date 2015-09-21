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
package net.java.sip.communicator.impl.gui.main.contactlist;

import junit.framework.*;
import net.java.sip.communicator.impl.gui.main.contactlist.GroupNode.NodeComparator;
import net.java.sip.communicator.service.gui.*;

public class GroupNodeTest
    extends TestCase
{

    public void testNodeComparatorUnknownsAtTheEnd()
    {
        ContactListNode unknown = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return -1;
            }
        };
        ContactListNode node = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return 1;
            }
        };
        NodeComparator comparator = new GroupNode.NodeComparator();
        Assert.assertEquals(comparator.compare(unknown, node),
            -1 * comparator.compare(node, unknown));
        Assert.assertEquals(1, comparator.compare(unknown, node));
        Assert.assertEquals(-1, comparator.compare(node, unknown));
    }

    public void testNodeComparatorNormalNodes()
    {
        ContactListNode node1 = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return 4;
            }
        };
        ContactListNode node2 = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return 7;
            }
        };
        NodeComparator comparator = new GroupNode.NodeComparator();
        Assert.assertEquals(comparator.compare(node1, node2),
            -1 * comparator.compare(node2, node1));
        Assert.assertEquals(-1, comparator.compare(node1, node2));
        Assert.assertEquals(1, comparator.compare(node2, node1));
    }

    public void testNodeComparatorSymmetryForUnknownNodes()
    {
        ContactListNode unknown1 = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return -1;
            }
        };
        ContactListNode unknown2 = new ContactListNode()
        {
            @Override
            public int getSourceIndex()
            {
                return -1;
            }
        };
        NodeComparator comparator = new GroupNode.NodeComparator();
        Assert.assertEquals(comparator.compare(unknown1, unknown2),
            -1 * comparator.compare(unknown2, unknown1));
    }
}
