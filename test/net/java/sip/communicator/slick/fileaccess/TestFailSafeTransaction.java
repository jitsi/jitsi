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

package net.java.sip.communicator.slick.fileaccess;

import java.io.*;

import junit.framework.*;

import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

/**
 * Tests for the fail safe transactions
 *
 * @author Benoit Pradelle
 */
public class TestFailSafeTransaction
    extends TestCase
{
    /**
     * The Service that we will be testing.
     */
    private FileAccessService fileAccessService = null;

    /**
     * Test data to write in the original file
     */
    private static final String origData = "this is a test for the fail safe "
        + "transaction ability in SIP Communicator";

    /**
     * Test data to add to the file
     */
    private static final String addedData = " which is the greatest IM client "
        + "in the world !";

    /**
     * Test data to never write in the file
     */
    private static final String wrongData = "all the file is damaged now !";

    /**
     * The base for the name of the temp file
     */
    private static String tempName = "wzsxedcrfv" + System.currentTimeMillis();

    /**
     * Standart constructor.
     *
     * @param name
     */
    public TestFailSafeTransaction(String name)
    {
        super(name);
        BundleContext context = FileAccessServiceLick.bc;
        ServiceReference ref = context
                .getServiceReference(FileAccessService.class.getName());
        this.fileAccessService = (FileAccessService) context.getService(ref);
    }
    /**
     * Tests the commit operation
     */
    public void testCommit() {
        try {
            // setup a temp file
            File temp = File.createTempFile(tempName + "a", null);
            FileOutputStream out = new FileOutputStream(temp);

            out.write(origData.getBytes());

            // write a modification during a transaction
            FailSafeTransaction trans = this.fileAccessService
                                            .createFailSafeTransaction(temp);
            trans.beginTransaction();

            out.write(addedData.getBytes());

            trans.commit();

            out.close();

            // test if the two writes are ok
            // file length
            assertEquals("the file hasn't the right size after a commit",
                    temp.length(),
                    origData.length() + addedData.length());

            FileInputStream in = new FileInputStream(temp);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
            String content = new String(buffer);

            // file content
            assertEquals("the file content isn't correct",
                    origData + addedData,
                    content);

            temp.delete();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests the rollback operation
     */
    public void testRollback() {
        try {
            // setup a temp file
            File temp = File.createTempFile(tempName + "b", null);
            FileOutputStream out = new FileOutputStream(temp);
            byte[] origDataBytes = origData.getBytes();

            out.write(origDataBytes);
            out.flush();

            // write a modification during a transaction
            FailSafeTransaction trans = this.fileAccessService
                                            .createFailSafeTransaction(temp);
            trans.beginTransaction();

            out.write(wrongData.getBytes());
            out.flush();

            trans.rollback();

            out.close();

            // test if the two writes are ok
            // file length
            assertEquals("the file hasn't the right size after a commit",
                    temp.length(),
                    origDataBytes.length);

            FileInputStream in = new FileInputStream(temp);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
            String content = new String(buffer);

            // file content
            assertEquals("the file content isn't correct",
                    origData,
                    content);

            temp.delete();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests if the file is commited when we start a new transaction
     */
    public void testCommitOnReOpen() {
        try {
            // setup a temp file
            File temp = File.createTempFile(tempName + "c", null);
            FileOutputStream out = new FileOutputStream(temp);

            out.write(origData.getBytes());

            // write a modification during a transaction
            FailSafeTransaction trans = this.fileAccessService
                                            .createFailSafeTransaction(temp);
            trans.beginTransaction();

            out.write(addedData.getBytes());

            // this transaction isn't closed, it should commit the changes
            trans.beginTransaction();

            // just to be sure to clean everything
            // the rollback must rollback nothing
            trans.rollback();

            out.close();

            // test if the two writes are ok
            // file length
            assertEquals("the file hasn't the right size after a commit",
                    temp.length(),
                    origData.length() + addedData.length());

            FileInputStream in = new FileInputStream(temp);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
            String content = new String(buffer);

            // file content
            assertEquals("the file content isn't correct",
                    origData + addedData,
                    content);

            temp.delete();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests if the file is rollback-ed if the transaction is never closed
     */
    public void testRollbackOnFailure() {
        try {
            // setup a temp file
            File temp = File.createTempFile(tempName + "d", null);
            FileOutputStream out = new FileOutputStream(temp);
            byte[] origDataBytes = origData.getBytes();

            out.write(origDataBytes);
            out.flush();

            // write a modification during a transaction
            FailSafeTransaction trans = this.fileAccessService
                                            .createFailSafeTransaction(temp);
            FailSafeTransaction trans2 = this.fileAccessService
                                            .createFailSafeTransaction(temp);
            trans.beginTransaction();

            out.write(wrongData.getBytes());
            out.flush();

            // we suppose here that SC crashed without closing properly the
            // transaction. When it restarts, the modification must have been
            // rollback-ed

            trans2.restoreFile();

            out.close();

            // test if the two writes are ok
            // file length
            assertEquals("the file hasn't the right size after a commit",
                    temp.length(),
                    origDataBytes.length);

            FileInputStream in = new FileInputStream(temp);
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
            String content = new String(buffer);

            // file content
            assertEquals("the file content isn't correct",
                    origData,
                    content);

            temp.delete();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
