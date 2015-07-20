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
package net.java.sip.communicator.slick.contactlist;

/**
 * The class contains a contact list file string as well as fields referencing
 * parts of its contents in order to allow for ease of testing. It also offers
 * methods for writing and reading a contact list file.
 * <p>
 * All persistent data in these contact lists is set to match the concatenation
 * of the contact/group uid and the account-id. This is because we don't really
 * need persistent data in mock contacts but we would like to see that it is
 * properly parsed by the metacontactlistservice and that it is handed to the
 * provider.
 *
 * @author Emil Ivov
 */
public class ContactListFileIO
{
//    MockProvider mockP1 = new MockProvider("mockP1");
//    MockContactGroup mockP1Grp1 = new MockContactGroup("MockP1.Grp1", mockP1);
//    MockContactGroup subMockP1Grp = new MockContactGroup("Mock1.SubProtoGroup", mockP1);
//    MockContact emilP1 = new MockContact("emil@MockP1", mockP1);
//    MockContact subEmilP1 = new MockContact("subemil@MockP1", mockP1);
//
//    MockProvider mockP2 = new MockProvider("mockP2");
//    MockContactGroup mockP2Grp1 = new MockContactGroup("MockP2.Grp1", mockP2);
//    MockContact emilP2 = new MockContact("emil@MockP2", mockP2);
//
//    ContactListFileIO()
//    {
//        initMockContacts();
//    }
//
//    /**
//     * Initialize the contacts of the two test providers so that they could
//     * be directly comparable to what has been parsed from the file.
//     */
//    private void initMockContacts()
//    {
//        //init mock provider 1
//        subMockP1Grp.addContact(subEmilP1);
//        mockP1Grp1.addContact(emilP1);
//        mockP1Grp1.addSubgroup(subMockP1Grp);
//
//        MockPersistentPresenceOperationSet mockPresOpSetP1
//            = (MockPersistentPresenceOperationSet)mockP1
//                .getSupportedOperationSets().get(
//                    OperationSetPresence.class.getName());
//        mockPresOpSetP1.addMockGroup(mockP1Grp1);
//
//        //init mock provider 2
//        mockP2Grp1.addContact(emilP2);
//
//        MockPersistentPresenceOperationSet mockPresOpSetP2
//            = (MockPersistentPresenceOperationSet)mockP2
//                .getSupportedOperationSets().get(
//                    OperationSetPresence.class.getName());
//        mockPresOpSetP2.addMockGroup(mockP2Grp1);
//
//
//    }
//
//    public static final String contactListContents =
//"<sip-communicator>\n" +
//"  <meta-contactlist>\n" +
//"     <group name=\"MetaGrpN1\" uid=\"mgp1'sun iqueid ent ifi er\">\n"+
//"\n" +
//"      <!-- Associated protocol groups -->\n" +
//"      <proto-groups>\n" +
//"        <proto-group uid=\"MockP1.Grp1\"\n" +
//"                     account-id=\"sip-communicator-mock:usr@MockP1\">\n" +
//"                     <persistent-data>MockP1.Grp1sip-communicator-mock:usr@MockP1</persistent-data>\n"+
//"        </proto-group>\n" +
//"        <proto-group uid=\"MockP2.Grp1\"\n" +
//"                     account-id=\"sip-communicator-mock:usr@MockP2\">\n" +
//"                     <persistent-data>MockP2.Grp1sip-communicator-mock:usr@MockP2</persistent-data>\n"+
//"        </proto-group>\n" +
//"      </proto-groups>\n" +
//"    </group>\n" +
//"\n" +
//"      <!-- Child contacts for this gorup-->\n" +
//"      <child-contacts>\n" +
//"        <!-- META CONTACT -->\n" +
//"        <meta-contact uid=\"thisisalongstringrepresentingametacontactuid\">\n" +
//"          <display-name>Emil Ivov</display-name>\n" +
//"\n" +
//"          <contact address=\"emil@MockP1\"\n" +
//"                   account-id=\"sip-communicator-mock:usr@MockP1\"\n" +
//"                   parent-proto-group-uid=\"MockP1.Grp1\">\n" +
//"                   <persistent-data>emil@MockP1sip-communicator-mock:usr@MockP1</persistent-data>\n" +
//"          </contact>\n" +
//"\n" +
//"          <contact address=\"emil@MockP2\"\n" +
//"                   account-id=\"sip-communicator-mock:usr@MockP2\"\n" +
//"                   parent-proto-group-uid=\"MockP2.Grp1\">\n" +
//"                   <persistent-data>emil@MockP2sip-communicator-mock:usr@MockP2</persistent-data>\n" +
//"          </contact>\n" +
//"\n" +
//"        </meta-contact>\n" +
//"\n" +
//"      </child-contacts>\n" +
//"      <!-- Subgroups for this gorup-->\n" +
//"      <subgroups>\n" +
//"        <group name=\"SubMetaGroup\" uid=\"taraliantsimaraliantsi\">\n" +
//"          <proto-groups>\n" +
//"            <proto-group uid=\"Mock1.SubProtoGroup\"\n" +
//"                         account-id=\"sip-communicator-mock:usr@MockP1\"\n" +
//"                         parent-proto-group-uid=\"MockP1.Grp1\">" + "\n" +
//"                         <persistent-data>Mock1.SubProtoGroupsip-communicator-mock:usr@MockP1</persistent-data>\n" +
//"            </proto-group>\n" +
//"          </proto-groups>\n" +
//"          <child-contacts>\n" +
//"            <contact address=\"subemil@MockP1\"" + "\n" +
//"                     account-id=\"sip-communicator-mock:MockUser@MockService\"" + "\n" +
//"                     proto-group-uid=\"Mock1.SubProtoGroup\">" + "\n" +
//"                     <persistent-data>subemil@MockP1sip-communicator-mock:usr@MockP1</persistent-data>\n" +
//"            </contact>\n" +
//"          </child-contacts>" + "\n" +
//"        </group>" + "\n" +
//"      </subgroups>" + "\n" +
//"  </meta-contactlist>\n" +
//"</sip-communicator>\n";
//
}
//
