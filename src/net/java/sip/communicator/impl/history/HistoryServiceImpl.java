/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.util.*;

/**
 * @author Alexander Pelov
 * @author Damian Minkov
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

    private boolean cacheEnabled = false;

    /**
     *  Characters and their replacement in created folder names
     */
    private final static String[][] ESCAPE_SEQUENCES = new String[][]
    {
        {"&", "&_amp"},
        {"/", "&_sl"},
        {"\\\\", "&_bs"},   // the char \
        {":", "&_co"},
        {"\\*", "&_as"},    // the char *
        {"\\?", "&_qm"},    // the char ?
        {"\"", "&_pa"},     // the char "
        {"<", "&_lt"},
        {">", "&_gt"},
        {"\\|", "&_pp"}     // the char |
    };

    public HistoryServiceImpl()
        throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        this.builder = factory.newDocumentBuilder();
    }

    public Iterator getExistingIDs()
    {
        Vector vect = new Vector();
        File histDir = null;
        try {
            String userSetDataDirectory = System.getProperty("HistoryServiceDirectory");
            if(userSetDataDirectory != null)
                histDir = this.fileAccessService
                    .getPrivatePersistentDirectory(userSetDataDirectory);
            else
                histDir = this.fileAccessService
                    .getPrivatePersistentDirectory(DATA_DIRECTORY);

            findDatFiles(vect, histDir);
        } catch (Exception e)
        {
            log.error("Error opening directory", e);
        }

        DBStructSerializer structParse = new DBStructSerializer(this);
        int size = vect.size();
        for (int i = 0; i < size; i++)
        {
            File f = (File) vect.get(i);

            synchronized (this.loadedFiles)
            {
                if (!this.loadedFiles.contains(f))
                {
                    synchronized (this.histories)
                    {
                        try {
                            History hist = structParse.loadHistory(f);
                            if (!this.histories.containsKey(hist.getID()))
                            {
                                this.histories.put(hist.getID(), hist);
                            }
                        } catch (Exception e)
                        {
                            log.error("Could not load history from file: "
                                    + f.getAbsolutePath(), e);
                        }
                    }
                }
            }
        }

        synchronized (this.histories)
        {
            return this.histories.keySet().iterator();
        }
    }

    public boolean isHistoryExisting(HistoryID id)
    {
        return this.histories.containsKey(id);
    }

    public History getHistory(HistoryID id)
        throws IllegalArgumentException
    {
        History retVal = null;

        synchronized (this.histories)
        {
            if (histories.containsKey(id))
            {
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

        synchronized (this.histories)
        {
            if (this.histories.containsKey(id))
            {
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

    protected FileAccessService getFileAccessService()
    {
        return this.fileAccessService;
    }

    protected DocumentBuilder getDocumentBuilder()
    {
        return builder;
    }

    /**
     * Parse documents. Synchronized to avoid exception
     * when concurrently parsing with same DocumentBuilder
     * @param file File the file to parse
     * @return Document the result document
     * @throws SAXException exception
     * @throws IOException exception
     */
    protected synchronized Document parse(File file)
        throws SAXException, IOException
    {
        return builder.parse(file);
    }

    /**
     * Parse documents. Synchronized to avoid exception
     * when concurrently parsing with same DocumentBuilder
     * @param in ByteArrayInputStream the stream to parse
     * @return Document the result document
     * @throws SAXException exception
     * @throws IOException exception
     */
    protected synchronized Document parse(ByteArrayInputStream in)
        throws SAXException, IOException
    {
        return builder.parse(in);
    }

    private void findDatFiles(Vector vect, File directory)
    {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                findDatFiles(vect, files[i]);
            } else if (DATA_FILE.equalsIgnoreCase(files[i].getName()))
            {
                vect.add(files[i]);
            }
        }
    }

    private File createHistoryDirectories(HistoryID id)
        throws IOException
    {
        String[] idComponents = id.getID();
        String[] dirs = new String[idComponents.length + 1];

        String userSetDataDirectory = System.getProperty("HistoryServiceDirectory");
        if(userSetDataDirectory != null)
            dirs[0] = userSetDataDirectory;
        else
            dirs[0] = DATA_DIRECTORY;
        
        // escape chars in direcotory names
        escapeCharacters(idComponents);
        
        System.arraycopy(idComponents, 0, dirs, 1, dirs.length - 1);

        File directory = null;
        try {
            directory = this.fileAccessService
                    .getPrivatePersistentDirectory(dirs);
        } catch (Exception e)
        {
            throw (IOException) new IOException(
                    "Could not create history due to file system error")
                    .initCause(e);
        }

        if (!directory.exists() && !directory.mkdirs())
        {
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
     * @param configurationService ConfigurationService
     */
    public void setConfigurationService(
            ConfigurationService configurationService)
    {
        synchronized (this.syncRoot_Config)
        {
            this.configurationService = configurationService;
            log.debug("New configuration service registered.");

            // store some config for further use
            Object isCacheEnabledObj =
                this.configurationService.getProperty(HistoryService.CACHE_ENABLED_PROPERTY);

            if(isCacheEnabledObj != null && isCacheEnabledObj.equals(HistoryService.CACHE_ENABLED))
                cacheEnabled = true;
        }
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService ConfigurationService
     */
    public void unsetConfigurationService(
            ConfigurationService configurationService)
    {
        synchronized (this.syncRoot_Config)
        {
            if (this.configurationService == configurationService)
            {
                this.configurationService = null;
                log.debug("Configuration service unregistered.");
            }
        }
    }

    /**
     * Set the file access service.
     *
     * @param fileAccessService FileAccessService
     */
    public void setFileAccessService(FileAccessService fileAccessService)
    {
        synchronized (this.syncRoot_FileAccess)
        {
            this.fileAccessService = fileAccessService;
            log.debug("New file access service registered.");
        }
    }

    /**
     * Remove the file access service.
     *
     * @param fileAccessService FileAccessService
     */
    public void unsetFileAccessService(FileAccessService fileAccessService)
    {
        synchronized (this.syncRoot_FileAccess)
        {
            if (this.fileAccessService == fileAccessService)
            {
                this.fileAccessService = null;
                log.debug("File access service unregistered.");
            }
        }
    }

    /**
     * Returns whether caching of readed documents is enabled or desibled.
     * @return boolean
     */
    protected boolean isCacheEnabled()
    {
        return cacheEnabled;
    }

    /**
     * Permamently removes local stored History
     *
     * @param id HistoryID
     * @throws IOException
     */
    public void purgeLocallyStoredHistory(HistoryID id)
        throws IOException
    {
        // get the history direcoty coresponding the given id
        File dir = this.createHistoryDirectories(id);
        log.trace("Removing history directory " + dir);
        deleteDirAndContent(dir);
    }

    /**
     * Deletes given directory and its content
     *
     * @param dir File
     * @throws IOException
     */
    private void deleteDirAndContent(File dir)
        throws IOException
    {
        if(!dir.isDirectory())
            return;

        File[] content = dir.listFiles();

        File tmp;
        for (int i = 0; i < content.length; i++)
        {
            tmp = content[i];
            if(tmp.isDirectory())
                deleteDirAndContent(tmp);
            else
                tmp.delete();
        }
        dir.delete();
    }
    
    /**
     * Replacing the characters that we must escape
     * used for the created filename.
     * 
     * @param ids Ids - folder names as we are using 
     *          FileSystem for storing files.
     */
    private void escapeCharacters(String[] ids)
    {
        for (int i = 0; i < ids.length; i++)
        {
            String currId = ids[i];
            
            for (int j = 0; j < ESCAPE_SEQUENCES.length; j++)
            {
                currId = currId.
                    replaceAll(ESCAPE_SEQUENCES[j][0], ESCAPE_SEQUENCES[j][1]);
            }
            ids[i] = currId;
        }
    }
}
