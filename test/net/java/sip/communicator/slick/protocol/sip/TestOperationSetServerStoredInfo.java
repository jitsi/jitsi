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
package net.java.sip.communicator.slick.protocol.sip;

import gov.nist.javax.sip.address.*;

import java.net.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Base64; // disambiguation

/**
 * Testing of the user and account info. Tests for reading, adding, removing,
 * replacing and error handling.
 *
 * @author Grigorii Balutsel
 */
public class TestOperationSetServerStoredInfo extends TestCase
{
    /**
     * Fixture for testing.
     */
    private SipSlickFixture fixture = new SipSlickFixture();

    /**
     * Account Info Operation set for testing.
     */
    private OperationSetServerStoredAccountInfo
            opSetServerStoredAccountInfo = null;

    /**
     * XCAP client for testing.
     */
    private XCapClient xCapClient;

    /**
     * Creates tests under specific name.
     *
     * @param name the tests name.
     */
    public TestOperationSetServerStoredInfo(String name)
    {
        super(name);
    }

    /**
     * Get a reference to the account info operation sets.
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
        Map<String, OperationSet> supportedOperationSets =
                fixture.provider1.getSupportedOperationSets();
        if (supportedOperationSets == null || supportedOperationSets.size() < 1)
        {
            throw new NullPointerException(
                    "No OperationSet implementations are supported by this " +
                            "SIP implementation.");
        }
        opSetServerStoredAccountInfo = (OperationSetServerStoredAccountInfo)
                supportedOperationSets.get(
                        OperationSetServerStoredAccountInfo.class.getName());
        if (opSetServerStoredAccountInfo == null)
        {
            throw new NullPointerException(
                    "No implementation for Account Info was found");
        }
        if (!opSetServerStoredAccountInfo
                .isDetailClassSupported(ServerStoredDetails.ImageDetail.class))
        {
            throw new NullPointerException(
                    "OperationSet does't support avatars");
        }

        // Connect to the XCAP server
        xCapClient = createXCapClient();
        if (!xCapClient.isConnected())
        {
            throw new NullPointerException("XCAP client is not connected");
        }
        if (!xCapClient.isPresContentSupported())
        {
            throw new NullPointerException(
                    "XCAP server doesn't support pres-content");
        }

        // Clean details
        List<ServerStoredDetails.GenericDetail> details =
                new ArrayList<ServerStoredDetails.GenericDetail>();
        Iterator<ServerStoredDetails.GenericDetail> detailIterator =
                opSetServerStoredAccountInfo.getAllAvailableDetails();
        while (detailIterator.hasNext())
        {
            details.add(detailIterator.next());
        }
        for (ServerStoredDetails.GenericDetail detail : details)
        {
            opSetServerStoredAccountInfo.removeDetail(detail);
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        fixture.tearDown();
        xCapClient.disconnect();
    }

    /**
     * Creates a test suite containing tests of this class in a specific order.
     * We'll first execute tests beginning with the "test" prefix and then go to
     * ordered tests. We first execute tests for reading info, then writing.
     * Then the ordered tests - error handling and finaly for removing details
     *
     * @return Test a testsuite containing all tests to execute.
     */
    public static Test suite()
    {
        if(System.getProperty(SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX +
            SipProtocolProviderServiceLick.XCAP_SERVER_PROPERTY_NAME) != null)
        {
            return new TestSuite(TestOperationSetServerStoredInfo.class);
        }
        return new TestSuite();
    }

    private XCapClient createXCapClient()
            throws Exception
    {
        String userName = System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX +
                        ProtocolProviderFactory.USER_ID);
        String password = System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX +
                        ProtocolProviderFactory.PASSWORD);
        String xCapServerUri = System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX +
                        SipProtocolProviderServiceLick
                                .XCAP_SERVER_PROPERTY_NAME);
        XCapClient xCapClient = new XCapClientImpl();
        xCapClient.connect(new URI(xCapServerUri),
                ((ProtocolProviderServiceSipImpl) fixture.provider1).
                        parseAddressString(userName),
                ((SipUri)(((ProtocolProviderServiceSipImpl) fixture.provider1).
                        parseAddressString(userName))).getUser(),
                password);
        return xCapClient;
    }

    /**
     * Tests reading info. Puts the image to the server by using XCAP client and
     * then gets it by using Sip Communicator interfaces.
     *
     * @throws Exception if there is some error during test.
     */
    public void testReadInfo() throws Exception
    {
        // Add image
        byte[] imageContent =
                TestOperationSetServerStoredInfoData.IMAGE_CONTENT_1;
        ServerStoredDetails.ImageDetail imageDetail1 =
                new ServerStoredDetails.ImageDetail(null, imageContent);
        opSetServerStoredAccountInfo.addDetail(imageDetail1);
        // Get saved image
        Iterator<ServerStoredDetails.GenericDetail> storedDetails =
                opSetServerStoredAccountInfo
                        .getDetails(ServerStoredDetails.ImageDetail.class);
        assertNotNull("Stored details cannot be null", storedDetails);
        assertTrue("Stored details doesn't have ImageDetail",
                storedDetails.hasNext());
        ServerStoredDetails.GenericDetail storedImageDetail =
                storedDetails.next();
        assertTrue("Stored details is not ImageDetail",
                storedImageDetail instanceof ServerStoredDetails.ImageDetail);
        byte[] savedContent =
                ((ServerStoredDetails.ImageDetail) storedImageDetail).getBytes();
        assertEquals(
                "The ImageDetail we set is not set or not read properly",
                imageContent.length,
                savedContent.length);
        for (int i = 0; i < imageContent.length; i++)
        {
            assertSame("The ImageDetail we set has not the same content",
                    imageContent[i], savedContent[i]);
        }
        // Get pres-content from the server
        ContentType presContent = xCapClient.getPresContent(
                ServerStoredContactListSipImpl.PRES_CONTENT_IMAGE_NAME);
        assertNotNull("Pres-content cannot be null", presContent);
        assertNotNull("Pres-content data cannot be null",
                presContent.getData());
        assertNotNull("Pres-content data value cannot be null",
                presContent.getData().getValue());
        byte[] serverContent = Base64.decode(presContent.getData().getValue());
        assertEquals(
                "The ImageDetail we set is not set or not read properly",
                imageContent.length, serverContent.length);
        for (int i = 0; i < imageContent.length; i++)
        {
            assertSame("The ImageDetail we set has not the same content",
                    imageContent[i], serverContent[i]);
        }
        // Create pres-content
//        ContentType presContent = new ContentType();
//        ContentType.MimeType mimeType = new ContentType.MimeType();
//        mimeType.setValue(TestOperationSetServerStoredInfoData.IMAGE_TYPE);
//        presContent.setMimeType(mimeType);
//        ContentType.EncodingType encoding = new ContentType.EncodingType();
//        encoding.setValue("base64");
//        presContent.setEncoding(encoding);
//        ContentType.DataType data = new ContentType.DataType();
//        data.setValue(encodedImageContent);
//        presContent.setData(data);
        // Put pres-content to the server
//        xCapClient.putPresContent(presContent,
//                ProtocolProviderServiceSipImpl.PRES_CONTENT_IMAGE_NAME);
    }

    /**
     * Tests writing info. Puts the image to the server by using Sip
     * Communicator interfaces and then gets it by using XCAP client.
     *
     * @throws Exception if there is some error during test.
     */
    public void testWriteInfo() throws Exception
    {
        byte[] imageContent =
                TestOperationSetServerStoredInfoData.IMAGE_CONTENT_1;
        ServerStoredDetails.ImageDetail imageDetail =
                new ServerStoredDetails.ImageDetail(null, imageContent);
        opSetServerStoredAccountInfo.addDetail(imageDetail);
        // Get pres-content
        ContentType presContent = xCapClient.getPresContent(
                ServerStoredContactListSipImpl.PRES_CONTENT_IMAGE_NAME);
        assertNotNull("Pres-content cannot be null", presContent);
        assertNotNull("Pres-content data cannot be null",
                presContent.getData());
        assertNotNull("Pres-content data value cannot be null",
                presContent.getData().getValue());
        byte[] serverContent = Base64.decode(presContent.getData().getValue());
        assertEquals(
                "The ImageDetail we set is not set or not read properly",
                imageContent.length, serverContent.length);
        for (int i = 0; i < imageContent.length; i++)
        {
            assertSame("The ImageDetail we set has not the same content",
                    imageContent[i], serverContent[i]);
        }
        // Remove saved image
        opSetServerStoredAccountInfo.removeDetail(imageDetail);
    }

    /**
     * Tests deleting info. Puts the image to the server by using Sip
     * Communicator interfaces, deletes it and then gets it by using XCAP
     * client.
     *
     * @throws Exception if there is some error during test.
     */
    public void testRemoveInfo() throws Exception
    {
        byte[] imageContent =
                TestOperationSetServerStoredInfoData.IMAGE_CONTENT_1;
        ServerStoredDetails.ImageDetail imageDetail =
                new ServerStoredDetails.ImageDetail(null, imageContent);
        opSetServerStoredAccountInfo.addDetail(imageDetail);
        // Remove saved image
        boolean removeResult =
                opSetServerStoredAccountInfo.removeDetail(imageDetail);
        assertTrue("The result of remove operation cannot be false",
                removeResult);
        // Get saved image
        Iterator<ServerStoredDetails.GenericDetail> storedDetails =
                opSetServerStoredAccountInfo
                        .getDetails(ServerStoredDetails.ImageDetail.class);
        assertNotNull("Stored details cannot be null", storedDetails);
        assertFalse("Stored details cannot have ImageDetail",
                storedDetails.hasNext());
        // Get pres-content
        ContentType presContent = xCapClient.getPresContent(
                ServerStoredContactListSipImpl.PRES_CONTENT_IMAGE_NAME);
        assertNull("Pres-content cannot be not null", presContent);
    }

    /**
     * Tests replacing info. Puts the image to the server by using Sip
     * Communicator interfaces, replace it and then gets it by using XCAP
     * client.
     *
     * @throws Exception if there is some error during test.
     */
    public void testReplaceInfo() throws Exception
    {
        byte[] imageContent1 =
                TestOperationSetServerStoredInfoData.IMAGE_CONTENT_1;
        byte[] imageContent2 =
                TestOperationSetServerStoredInfoData.IMAGE_CONTENT_2;
        ServerStoredDetails.ImageDetail imageDetail1 =
                new ServerStoredDetails.ImageDetail(null, imageContent1);
        ServerStoredDetails.ImageDetail imageDetail2 =
                new ServerStoredDetails.ImageDetail(null, imageContent2);
        opSetServerStoredAccountInfo.addDetail(imageDetail1);
        boolean replaceResult = opSetServerStoredAccountInfo
                .replaceDetail(imageDetail1, imageDetail2);
        assertTrue("The result of replace operation cannot be false",
                replaceResult);
        // Get saved image
        Iterator<ServerStoredDetails.GenericDetail> storedDetails =
                opSetServerStoredAccountInfo
                        .getDetails(ServerStoredDetails.ImageDetail.class);
        assertNotNull("Stored details cannot be null", storedDetails);
        assertTrue("Stored details doesn't have ImageDetail",
                storedDetails.hasNext());
        ServerStoredDetails.GenericDetail imageDetail =
                storedDetails.next();
        assertTrue("Stored details is not ImageDetail",
                imageDetail instanceof ServerStoredDetails.ImageDetail);
        byte[] savedContent =
                ((ServerStoredDetails.ImageDetail) imageDetail).getBytes();
        assertEquals(
                "The ImageDetail we set is not set or not read properly",
                imageContent2.length,
                savedContent.length);
        for (int i = 0; i < imageContent2.length; i++)
        {
            assertSame("The ImageDetail we set has not the same content",
                    imageContent2[i], savedContent[i]);
        }
        // Get pres-content
        ContentType presContent = xCapClient.getPresContent(
                ServerStoredContactListSipImpl.PRES_CONTENT_IMAGE_NAME);
        assertNotNull("Pres-content cannot be null", presContent);
        assertNotNull("Pres-content data cannot be null",
                presContent.getData());
        assertNotNull("Pres-content data value cannot be null",
                presContent.getData().getValue());
        byte[] serverContent = Base64.decode(presContent.getData().getValue());
        assertEquals(
                "The ImageDetail we set is not set or not read properly",
                imageContent2.length, serverContent.length);
        for (int i = 0; i < imageContent2.length; i++)
        {
            assertSame("The ImageDetail we set has not the same content",
                    imageContent2[i], serverContent[i]);
        }
        // Remove saved image
        opSetServerStoredAccountInfo.removeDetail(imageDetail2);
    }
}
