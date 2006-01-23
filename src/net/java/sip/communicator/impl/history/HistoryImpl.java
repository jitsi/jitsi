/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;

import net.java.sip.communicator.service.history.History;
import net.java.sip.communicator.service.history.HistoryID;
import net.java.sip.communicator.service.history.HistoryReader;
import net.java.sip.communicator.service.history.HistoryWriter;
import net.java.sip.communicator.service.history.records.HistoryRecordStructure;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.xml.XMLUtils;

import org.w3c.dom.Document;

/**
 * @author Alexander Pelov
 */
public class HistoryImpl implements History {
	
	private static Logger log = Logger.getLogger(HistoryImpl.class);
	
	private HistoryID id;
	private HistoryRecordStructure historyRecordStructure;
	private HistoryServiceImpl historyServiceImpl;
	
	private File directory;
	private HistoryReader reader;
	private HistoryWriter writer;
	
	private SortedMap historyDocuments = new TreeMap();
	
	protected HistoryImpl(
			HistoryID id,
			File directory,
			HistoryRecordStructure historyRecordStructure,
			HistoryServiceImpl historyServiceImpl)
	{
		try {
			log.logEntry();

			// TODO: Assert: Assert.assertNonNull(historyServiceImpl, "The historyServiceImpl should be non-null.");
			// TODO: Assert: Assert.assertNonNull(id, "The ID should be non-null.");
			// TODO: Assert: Assert.assertNonNull(historyRecordStructure, "The structure should be non-null.");
			
			this.id = id;
			this.directory = directory;
			this.historyServiceImpl = historyServiceImpl;
			this.historyRecordStructure = historyRecordStructure;
			this.reader = null;
			this.writer = null;
						
			this.reloadDocumentList();
		} finally {
			log.logExit();
		}
	}
	
	public HistoryID getID() {
		return this.id;
	}

	public HistoryRecordStructure getHistoryRecordsStructure() {
		return this.historyRecordStructure;
	}

	public HistoryReader getReader() {
		if(this.reader == null) {
			this.reader = new HistoryReaderImpl(this);
		}
		
		return this.reader;
	}

	public HistoryWriter getWriter() {
		if(this.writer == null) {
			this.writer = new HistoryWriterImpl(this);
		}
		
		return this.writer;
	}
	
	protected HistoryServiceImpl getHistoryServiceImpl() {
		return this.historyServiceImpl;
	}
	
	private void reloadDocumentList() {
		synchronized(this.historyDocuments) {
			this.historyDocuments.clear();
		 
			File[] files = this.directory.listFiles();
			// TODO: Assert: Assert.assertNonNull(files, "The list of files should be non-null.");
			
			for(int i = 0; i < files.length; i++) {
				if(!files[i].isDirectory()) {
					this.historyDocuments.put(files[i].getName(), files[i]);
				}
			}
		}
	}


	protected Document createDocument(String filename) {
		Document retVal = null;
		
		synchronized(this.historyDocuments) {
			if(this.historyDocuments.containsKey(filename)) {
				retVal = getDocumentForFile(filename);
			} else {
				retVal = this.historyServiceImpl.getDocumentBuilder().newDocument();
				retVal.appendChild(retVal.createElement("history"));
				
				this.historyDocuments.put(filename, retVal);
			}
		}
		
		return retVal;
	}

	protected void writeFile(String filename) 
		throws InvalidParameterException, IOException
	{
		File file = new File(this.directory, filename);
		
		synchronized(this.historyDocuments) {
			if(!this.historyDocuments.containsKey(filename)) {
				throw new InvalidParameterException("The requested " +
						"filename does not exist in the document list.");
			}
			
			Object obj = this.historyDocuments.get(filename);
			if(obj instanceof Document) {
				Document doc = (Document)obj;
			
				synchronized(doc) {
					XMLUtils.writeXML(doc, file);
				}
			}
		}
	}

	protected Iterator getFileList() {
		return this.historyDocuments.keySet().iterator();
	}
	
	protected Document getDocumentForFile(String filename) 
		throws InvalidParameterException, RuntimeException
	{
		Document retVal = null;
		
		synchronized(this.historyDocuments) {
			if(!this.historyDocuments.containsKey(filename)) {
				throw new InvalidParameterException("The requested " +
						"filename does not exist in the document list.");
			}
			
			Object obj = this.historyDocuments.get(filename);
			if(obj instanceof Document) {
				// Document already loaded. Use it directly
				retVal = (Document)obj;
			} else if(obj instanceof File) {
				File file = (File)obj;
				
				DocumentBuilder builder = this.historyServiceImpl.getDocumentBuilder();

	            try {
					retVal = builder.parse(file);
				} catch (Exception e) {
					throw new RuntimeException("Error occured while " +
							"parsing XML document.", e);
				}

				// Cache the loaded document for reuse
				this.historyDocuments.put(filename, retVal);
			} else {
				// TODO: Assert: Assert.fail("Internal error - the data type " +
				//		"should be either Document or File.");
			}
		}
		
		return retVal;
	}

}
