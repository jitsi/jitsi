/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.fileaccess.FileAccessService;
import net.java.sip.communicator.service.history.History;
import net.java.sip.communicator.service.history.HistoryID;
import net.java.sip.communicator.service.history.HistoryService;
import net.java.sip.communicator.service.history.records.HistoryRecordStructure;
import net.java.sip.communicator.util.Logger;

/**
 * @author Alexander Pelov
 */
public class HistoryServiceImpl implements HistoryService {

    public static final String DATA_DIRECTORY = "history_ver1.0";

    public static final String DATA_FILE = "dbstruct.dat";

    /**
     * The logger for this class.
     */
    private static Logger log = Logger.getLogger(HistoryServiceImpl.class);

    private Map histories = new Hashtable(); // Note: Hashtable is
                                                // SYNCHRONIZED

    private Collection loadedFiles = Collections
            .synchronizedCollection(new Vector());

    private ConfigurationService configurationService;

    private FileAccessService fileAccessService;

    private Object syncRoot_FileAccess = new Object();

    private Object syncRoot_Config = new Object();

    private DocumentBuilder builder;

    public HistoryServiceImpl() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.builder = factory.newDocumentBuilder();
    }

    public Iterator getExistingIDs() {
        Vector vect = new Vector();
        File histDir = null;
        try {
            histDir = this.fileAccessService
                    .getPrivatePersistentDirectory(DATA_DIRECTORY);
            findDatFiles(vect, histDir);
        } catch (Exception e) {
            log.error("Error opening directory", e);
        }

        DBStructSerializer structParse = new DBStructSerializer(this);
        int size = vect.size();
        for (int i = 0; i < size; i++) {
            File f = (File) vect.get(i);

            synchronized (this.loadedFiles) {
                if (!this.loadedFiles.contains(f)) {
                    synchronized (this.histories) {
                        try {
                            History hist = structParse.loadHistory(f);
                            if (!this.histories.containsKey(hist.getID())) {
                                this.histories.put(hist.getID(), hist);
                            }
                        } catch (Exception e) {
                            log.error("Could not load history from file: "
                                    + f.getAbsolutePath(), e);
                        }
                    }
                }
            }
        }

        synchronized (this.histories) {
            return this.histories.keySet().iterator();
        }
    }

    public boolean isHistoryExisting(HistoryID id) {
        return this.histories.containsKey(id);
    }

    public History getHistory(HistoryID id) throws IllegalArgumentException {
        History retVal = null;

        synchronized (this.histories) {
            if (histories.containsKey(id)) {
                retVal = (History) histories.get(id);
            } else {
                throw new IllegalArgumentException(
                        "No history corresponds to the specified ID.");
            }
        }

        return retVal;
    }

    public History createHistory(HistoryID id,
            HistoryRecordStructure recordStructure)
            throws IllegalArgumentException, IOException {
        History retVal = null;

        synchronized (this.histories) {
            if (this.histories.containsKey(id)) {
                throw new IllegalArgumentException(
                        "There is already a history with the specified ID.");
            } else {
                File dir = this.createHistoryDirectories(id);
                HistoryImpl history = new HistoryImpl(id, dir, recordStructure,
                        this);

                File dbDatFile = new File(dir, HistoryServiceImpl.DATA_FILE);
                DBStructSerializer dbss = new DBStructSerializer(this);
                dbss.writeHistory(dbDatFile, history);

                this.histories.put(id, history);
                retVal = history;
            }
        }

        return retVal;
    }

    protected FileAccessService getFileAccessService() {
        return this.fileAccessService;
    }

    protected DocumentBuilder getDocumentBuilder() {
        return builder;
    }

    private void findDatFiles(Vector vect, File directory) {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                findDatFiles(vect, files[i]);
            } else if (DATA_FILE.equalsIgnoreCase(files[i].getName())) {
                vect.add(files[i]);
            }
        }
    }

    private File createHistoryDirectories(HistoryID id) throws IOException {
        String[] idComponents = id.getID();
        String[] dirs = new String[idComponents.length + 1];
        dirs[0] = "history";
        System.arraycopy(idComponents, 0, dirs, 1, dirs.length - 1);

        File directory = null;
        try {
            directory = this.fileAccessService
                    .getPrivatePersistentDirectory(dirs);
        } catch (Exception e) {
            throw (IOException) new IOException(
                    "Could not create history due to file system error")
                    .initCause(e);
        }

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(
                    "Could not create requested history service files:"
                            + directory.getAbsolutePath());
        }

        return directory;
    }

    // //////////////////////////////////////////////////////////////////////////
    /**
     * Set the configuration service.
     * 
     * @param configurationService
     */
    public void setConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            this.configurationService = configurationService;
            log.debug("New configuration service registered.");
        }
    }

    /**
     * Remove a configuration service.
     * 
     * @param configurationService
     */
    public void unsetConfigurationService(
            ConfigurationService configurationService) {
        synchronized (this.syncRoot_Config) {
            if (this.configurationService == configurationService) {
                this.configurationService = null;
                log.debug("Configuration service unregistered.");
            }
        }
    }

    /**
     * Set the file access service.
     * 
     * @param fileAccessService
     */
    public void setFileAccessService(FileAccessService fileAccessService) {
        synchronized (this.syncRoot_FileAccess) {
            this.fileAccessService = fileAccessService;
            log.debug("New file access service registered.");
        }
    }

    /**
     * Remove the file access service.
     * 
     * @param fileAccessService
     */
    public void unsetFileAccessService(FileAccessService fileAccessService) {
        synchronized (this.syncRoot_FileAccess) {
            if (this.fileAccessService == fileAccessService) {
                this.fileAccessService = null;
                log.debug("File access service unregistered.");
            }
        }
    }

}
