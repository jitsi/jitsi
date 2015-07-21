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
package net.java.sip.communicator.slick.history;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;

import org.osgi.framework.*;

public class TestHistoryService extends TestCase {

    private static HistoryRecordStructure recordStructure =
        new HistoryRecordStructure(new String[] { "age", "name_CDATA", "sex" });

    /**
     * The ConfigurationService that we will be testing.
     */
    private HistoryService historyService = null;

    private ServiceReference historyServiceRef = null;

    private History history = null;

    private Random random = new Random();

    public TestHistoryService(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(new TestHistoryService("testCreateDB"));
        suite.addTest(new TestHistoryService("testWriteRecords"));
        suite.addTest(new TestHistoryService("testReadRecords"));
        suite.addTest(new TestHistoryService("testPurgeLocallyStoredHistory"));
        suite.addTest(new TestHistoryService("testCreatingHistoryIDFromFS"));
        suite.addTest(new TestHistoryService("testWriteRecordsWithMaxNumber"));

        return suite;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        BundleContext context = HistoryServiceLick.bc;

        historyServiceRef = context.getServiceReference(HistoryService.class
                .getName());
        this.historyService = (HistoryService) context
                .getService(historyServiceRef);

        HistoryID testID = HistoryID.createFromRawID(new String[] { "test",
                "alltests" });

        this.history = this.historyService.createHistory(testID,
                    recordStructure);
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        BundleContext context = HistoryServiceLick.bc;

        context.ungetService(this.historyServiceRef);

        this.history = null;
        this.historyService = null;
        this.historyServiceRef = null;
    }

    public void testCreateDB()
    {
        ArrayList<String> al = new ArrayList<String>();

        Iterator<HistoryID> i = this.historyService.getExistingIDs();
        while (i.hasNext())
        {
            HistoryID id = i.next();
            String[] components = id.getID();

            if (components.length == 2 && "test".equals(components[0]))
            {
                al.add(components[1]);
            }
        }

        int count = al.size();
        boolean unique = false;
        String lastComp = null;
        while (!unique)
        {
            lastComp = Integer.toHexString(random.nextInt());
            for (int j = 0; j < count; j++)
            {
                if (lastComp.equals(al.get(j)))
                {
                    continue;
                }
            }
            unique = true;
        }

        HistoryID id = HistoryID.createFromRawID(new String[] { "test",
                lastComp });

        try {
            this.historyService.createHistory(id, recordStructure);
        } catch (Exception e)
        {
            fail("Could not create database with id " + id + " with error " + e);
        }

        try
        {
            // after creating, remove it - do not leave content
            this.historyService.purgeLocallyStoredHistory(id);
        }
        catch (Exception ex)
        {
            fail("Cannot delete local history with id " + this.history.getID()
                 + " : " + ex.getMessage());
        }
    }

    public void testWriteRecords()
    {
        HistoryWriter writer = this.history.getWriter();

        try {
            for (int i = 0; i < 202; i++)
            {
                writer.addRecord(new String[] { "" + random.nextInt(),
                                 "name" + i,
                                 i % 2 == 0 ? "m" : "f" });
            }
        } catch (Exception e)
        {
            fail("Could not write records. Reason: " + e);
        }
    }

    public void testReadRecords()
    {
        HistoryReader reader = this.history.getReader();

        QueryResultSet<HistoryRecord> result = reader.findByKeyword("name2", "name");

        assertTrue("Nothing found", result.hasNext());

        while (result.hasNext())
        {
            HistoryRecord record = result.nextRecord();

            String[] vals = record.getPropertyValues();

            try {
                int n = Integer.parseInt(vals[1].substring(4));

                assertEquals(3, vals.length);
                assertEquals(n % 2 == 0 ? "m" : "f", vals[2]);
            } catch (Exception e)
            {
                fail("Bad data! Expected nameXXXX, where XXXX is "
                        + "an integer, but found: " + vals[0]);
            }
        }
    }

    public void testPurgeLocallyStoredHistory()
    {
        try
        {
            this.historyService.purgeLocallyStoredHistory(this.history.getID());
        }
        catch (Exception ex)
        {
            fail("Cannot delete local history with id " + this.history.getID()
                 + " : " + ex.getMessage());
        }
    }

    /**
     * Test of method createFromRawStrings, used when we read history folders
     * from FS and from their names want to recreate history.
     */
    public void testCreatingHistoryIDFromFS()
    {
        testHistoryIDCreate(new String[] { "test1", "alltests1" });

        //test id which has special chars (accounts)
        testHistoryIDCreate(new String[] { "test2", "alltests2",
            "Jabber:mincho.penchev@jit.si@jit.si" });
    }

    private void testHistoryIDCreate(String[] strArr)
    {
        HistoryID testNoSpecialCharsID = HistoryID.createFromRawID(strArr);
        HistoryID testNoSpecialCharsIDFSRead =
            HistoryID.createFromRawStrings(testNoSpecialCharsID.getID());

        assertEquals("Wrong length", testNoSpecialCharsID.getID().length,
            testNoSpecialCharsIDFSRead.getID().length);

        for(int i = 0; i < testNoSpecialCharsID.getID().length; i++)
        {
            /*System.err.println(
                testNoSpecialCharsID.getID()[i] +
                " ? " +
                testNoSpecialCharsIDFSRead.getID()[i]);*/
            assertEquals("Wrong id", testNoSpecialCharsID.getID()[i],
                testNoSpecialCharsIDFSRead.getID()[i]);
        }
    }

    public void testWriteRecordsWithMaxNumber()
    {
        HistoryWriter writer = this.history.getWriter();
        HistoryReader reader = this.history.getReader();

        try
        {

            for (int i = 0; i < 20; i++)
            {
                writer.addRecord(new String[] { "" + i,
                    "name" + i,
                    i % 2 == 0 ? "m" : "f" }, 20);
                synchronized(this)
                {
                    try
                    {
                        wait(100);
                    }
                    catch(Throwable t){}
                }
            }

            QueryResultSet<HistoryRecord> recs = reader.findLast(20);
            int count = 0;
            while(recs.hasNext())
            {
                count++;
                recs.next();
            }

            assertEquals( "Wrong count of messages", 20, count);

            writer.addRecord(new String[] { "" + 21,
                "name" + 21, "f" }, 20);

            recs = reader.findLast(20);
            count = 0;
            boolean foundFirstMessage = false;
            while(recs.hasNext())
            {
                count++;
                HistoryRecord hr = recs.next();

                if(hr.getPropertyValues()[0].equals("0"))
                    foundFirstMessage = true;
            }

            assertEquals( "Wrong count of messages", 20, count);

            assertFalse("Wrong message removed, must be the first one",
                foundFirstMessage);

        } catch (Exception e)
        {
            e.printStackTrace();
            fail("Could not write records. Reason: " + e);
        }
    }

}
