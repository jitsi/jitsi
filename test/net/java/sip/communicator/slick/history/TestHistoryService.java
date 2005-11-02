package net.java.sip.communicator.slick.history;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import junit.framework.TestCase;
import net.java.sip.communicator.service.history.History;
import net.java.sip.communicator.service.history.HistoryID;
import net.java.sip.communicator.service.history.HistoryReader;
import net.java.sip.communicator.service.history.HistoryService;
import net.java.sip.communicator.service.history.HistoryWriter;
import net.java.sip.communicator.service.history.QueryResultSet;
import net.java.sip.communicator.service.history.records.HistoryRecord;
import net.java.sip.communicator.service.history.records.HistoryRecordStructure;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class TestHistoryService extends TestCase {
	
	private static HistoryRecordStructure recordStructure = 
		new HistoryRecordStructure(new String[] {"name", "age", "sex"});

    /**
     * The ConfigurationService that we will be testing.
     */
    private HistoryService historyService = null;
    private ServiceReference historyServiceRef = null;
    
    private History history = null;
    
    private Random random = new Random();
    
	public TestHistoryService(String name) throws Exception {
        super(name);
    }
	
	protected void setUp() throws Exception {
        BundleContext context = HistoryServiceLick.bc;

        historyServiceRef = context.getServiceReference(
        	HistoryService.class.getName());
        this.historyService = (HistoryService)context.getService(historyServiceRef);
        
		HistoryID testID = HistoryID.createFromRawID(new String[] {"test", 
				"alltests"});
		
		if(!this.historyService.isHistoryExisting(testID)) {			
			this.history = this.historyService.createHistory(testID, 
					recordStructure);
		} else {
			this.history = this.historyService.getHistory(testID);
		}
	}
	
	protected void tearDown() throws Exception {
        BundleContext context = HistoryServiceLick.bc;
        
        context.ungetService(this.historyServiceRef);
        
        this.history = null;
        this.historyService = null;
        this.historyServiceRef = null;
	}

	public void testCreateDB() {
		ArrayList al = new ArrayList();
		
		Iterator i = this.historyService.getExistingIDs();
		while(i.hasNext()) {
			HistoryID id = (HistoryID)i.next();
			String[] components = id.getID();
			
			if(components.length == 2 && "test".equals(components[0])) {
				al.add(components[1]);
			}
		}
		
		int count = al.size();
		boolean unique = false;
		String lastComp = null;
		while(!unique) {
			lastComp = Integer.toHexString(random.nextInt());
			for(int j = 0; j < count; j++) {
				if(lastComp.equals(al.get(j))) {
					continue;
				}
			}
			unique = true;
		}
		
		HistoryID id = HistoryID.createFromRawID(
				new String[] {"test", lastComp});
		
		try {
			this.historyService.createHistory(id, recordStructure);
		} catch (Exception e) {
			fail("Could not create database with id " + id + " with error " + e);
		}
	}
	
	public void testWriteRecords() {
		HistoryWriter writer = this.history.getWriter();
		
		try {
			for(int i = 0; i < 202; i++) {
				writer.addRecord(new String[] {"name"+i, ""+random.nextInt(), 
						i%2==0 ? "m" : "f"});
			}
		} catch(Exception e) {
			fail("Could not write records. Reason: " + e);
		}
	}
	
	public void testReadRecords() {
		HistoryReader reader = this.history.getReader();
		
		QueryResultSet result = reader.findByKeyword("name2");
		
		assertTrue(result.hasNext());
		
		while(result.hasNext()) {
			HistoryRecord record = result.nextRecord();
			
			String[] vals = record.getPropertyValues();
			
			try {
				int n = Integer.parseInt(vals[0].substring(4));
				
				assertEquals(3, vals.length);
				assertEquals(n%2==0 ? "m" : "f", vals[2]);
			} catch(Exception e) {
				fail("Bad data! Expected nameXXXX, where XXXX is " +
						"an integer, but found: " + vals[0]);
			}
		}
	}
	
}
