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
package net.java.sip.communicator.impl.history;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.history.records.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * @author Alexander Pelov
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class HistoryServiceImpl
    implements HistoryService
{
    /**
     * The data directory.
     */
    public static final String DATA_DIRECTORY = "history_ver1.0";

    /**
     * The data file.
     */
    public static final String DATA_FILE = "dbstruct.dat";

    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(HistoryServiceImpl.class);

    // Note: Hashtable is SYNCHRONIZED
    private final Map<HistoryID, History> histories =
        new Hashtable<HistoryID, History>();

    private final FileAccessService fileAccessService;

    private final DocumentBuilder builder;

    private final boolean cacheEnabled;

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

    /**
     * Constructor.
     *
     * @param bundleContext OSGi bundle context
     * @throws Exception if something went wrong during initialization
     */
    public HistoryServiceImpl(BundleContext bundleContext)
        throws Exception
    {
        this.builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.cacheEnabled =
            getConfigurationService(bundleContext).getBoolean(
                CACHE_ENABLED_PROPERTY, false);
        this.fileAccessService = getFileAccessService(bundleContext);
    }

    public Iterator<HistoryID> getExistingIDs()
    {
        List<File> vect = new Vector<File>();
        File histDir;
        try {
            String userSetDataDirectory
                = System.getProperty("HistoryServiceDirectory");

            histDir
                = getFileAccessService().getPrivatePersistentDirectory(
                        (userSetDataDirectory == null)
                            ? DATA_DIRECTORY
                            : userSetDataDirectory, FileCategory.PROFILE);

            findDatFiles(vect, histDir);
        } catch (Exception e)
        {
            logger.error("Error opening directory", e);
        }

        DBStructSerializer structParse = new DBStructSerializer(this);
        for (File f : vect)
        {
            synchronized (this.histories)
            {
                try
                {
                    History hist = structParse.loadHistory(f);
                    if (!this.histories.containsKey(hist.getID()))
                    {
                        this.histories.put(hist.getID(), hist);
                    }
                }
                catch (Exception e)
                {
                    logger.error("Could not load history from file: "
                        + f.getAbsolutePath(), e);
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
                retVal = histories.get(id);
            } else {
                throw new IllegalArgumentException(
                        "No history corresponds to the specified ID.");
            }
        }

        return retVal;
    }

    public History createHistory(
                        HistoryID id,
                        HistoryRecordStructure recordStructure)
            throws IllegalArgumentException,
                   IOException
    {
        History retVal = null;

        synchronized (this.histories)
        {
            if (this.histories.containsKey(id))
            {
                retVal = this.histories.get(id);
                retVal.setHistoryRecordsStructure(recordStructure);
            }
            else
            {
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
        FileInputStream fis = new FileInputStream(file);
        Document doc = builder.parse(fis);
        fis.close();
        return doc;
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

    private void findDatFiles(List<File> vect, File directory)
    {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                findDatFiles(vect, files[i]);
            }
            else if (DATA_FILE.equalsIgnoreCase(files[i].getName()))
            {
                vect.add(files[i]);
            }
        }
    }

    private File createHistoryDirectories(HistoryID id)
        throws IOException
    {
        String[] idComponents = id.getID();

        // escape chars in directory names
        escapeCharacters(idComponents);

        String userSetDataDirectory
            = System.getProperty("HistoryServiceDirectory");

        File dir = new File(userSetDataDirectory != null
            ? userSetDataDirectory
            : DATA_DIRECTORY);

        for (String s : idComponents)
        {
            dir = new File(dir, s);
        }

        File directory = null;
        try
        {
            directory
                = getFileAccessService().getPrivatePersistentDirectory(
                    dir.toString(),
                    FileCategory.PROFILE);
        }
        catch (Exception e)
        {
            IOException ioe
                = new IOException(
                        "Could not create history due to file system error");

            ioe.initCause(e);
            throw ioe;
        }

        if (!directory.exists() && !directory.mkdirs())
        {
            throw new IOException(
                    "Could not create requested history service files:"
                            + directory.getAbsolutePath());
        }

        return directory;
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
        // get the history directory corresponding the given id
        File dir = this.createHistoryDirectories(id);
        if (logger.isTraceEnabled())
            logger.trace("Removing history directory " + dir);
        deleteDirAndContent(dir);

        History history = histories.remove(id);
        if(history == null)
        {
            // well this can be global delete, so lets remove all matching
            // sub-histories
            String[] ids = id.getID();

            Iterator<Map.Entry<HistoryID, History>>
                iter = histories.entrySet().iterator();
            while(iter.hasNext())
            {
                Map.Entry<HistoryID, History> entry = iter.next();
                if(isSubHistory(ids, entry.getKey()))
                {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Clears locally(in memory) cached histories.
     */
    public void purgeLocallyCachedHistories()
    {
        histories.clear();
    }

    /**
     * Checks the ids of the parent, do they exist in the supplied history ids.
     * If it exist the history is sub history of the on with the supplied ids.
     * @param parentIDs the parent ids
     * @param hid the history to check
     * @return whether history is sub one (contained) of the parent.
     */
    private boolean isSubHistory(String[] parentIDs, HistoryID hid)
    {
        String[] hids = hid.getID();

        if(hids.length < parentIDs.length)
            return false;

        for(int i = 0; i < parentIDs.length; i++)
        {
            if(!parentIDs[i].equals(hids[i]))
                return false;
        }
        // everything matches, return true
        return true;
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

    private static ConfigurationService getConfigurationService(
        BundleContext bundleContext)
    {
        ServiceReference serviceReference =
            bundleContext.getServiceReference(ConfigurationService.class
                .getName());
        return (serviceReference == null) ? null
            : (ConfigurationService) bundleContext.getService(serviceReference);
    }

    private static FileAccessService getFileAccessService(
            BundleContext bundleContext)
    {
        return ServiceUtils.getService(bundleContext, FileAccessService.class);
    }

    /**
     * Moves the content of oldId history to the content of the newId.
     * Moves the content from the oldId folder to the newId folder.
     * Old folder must exist.
     *
     * @param oldId old and existing history
     * @param newId the place where content of oldId will be moved
     * @throws java.io.IOException problem moving to newId
     */
    public void moveHistory(HistoryID oldId, HistoryID newId)
        throws IOException
    {
        if(!isHistoryCreated(oldId))// || !isHistoryExisting(newId))
            return;

        File oldDir = this.createHistoryDirectories(oldId);
        File newDir = getDirForHistory(newId);

        // make sure parent path is existing
        newDir.getParentFile().mkdirs();

        if(!oldDir.renameTo(newDir))
        {
            if (logger.isInfoEnabled())
                logger.info("Cannot move history!");
            throw new IOException("Cannot move history!");
        }

        histories.remove(oldId);
    }

    /**
     * Returns the folder for the given history without creating it.
     * @param id the history
     * @return the folder for the history
     */
    private File getDirForHistory(HistoryID id)
    {
        // put together subfolder names.
        String[] dirNames = id.getID();
        StringBuffer dirName = new StringBuffer();
        for (int i = 0; i < dirNames.length; i++)
        {
            if (i > 0)
                dirName.append(File.separatorChar);
            dirName.append(dirNames[i]);
        }

        // get the parent directory
        File histDir = null;
        try
        {
            String userSetDataDirectory
                = System.getProperty("HistoryServiceDirectory");

            histDir
                = getFileAccessService().getPrivatePersistentDirectory(
                        (userSetDataDirectory == null)
                            ? DATA_DIRECTORY
                            : userSetDataDirectory,
                        FileCategory.PROFILE);
        }
        catch (Exception e)
        {
            logger.error("Error opening directory", e);
        }

        return new File(histDir, dirName.toString());
    }

    /**
     * Checks whether a history is created and stored.
     * Exists in the file system.
     * @param id the history to check
     * @return whether a history is created and stored.
     */
    public boolean isHistoryCreated(HistoryID id)
    {
        return getDirForHistory(id).exists();
    }

    /**
     * Enumerates existing histories.
     * @param rawid the start of the HistoryID of all the histories that will be
     * returned.
     * @return list of histories which HistoryID starts with <tt>rawid</tt>.
     * @throws IllegalArgumentException if the <tt>rawid</tt> contains ids
     * which are missing in current history.
     */
    public List<HistoryID> getExistingHistories(
                            String[] rawid)
        throws IllegalArgumentException
    {
        File histDir = null;
        try
        {
            histDir = getFileAccessService()
                .getPrivatePersistentDirectory(
                    DATA_DIRECTORY, FileCategory.PROFILE);
        }
        catch (Exception e)
        {
            logger.error("Error opening directory", e);
        }

        if(histDir == null || !histDir.exists())
            return new ArrayList<HistoryID>();

        StringBuilder folderPath = new StringBuilder();
        for(String id : rawid)
            folderPath.append(id).append(File.separator);

        File srcFolder = new File(histDir, folderPath.toString());

        if(!srcFolder.exists())
            return new ArrayList<HistoryID>();

        TreeMap<File, HistoryID> recentFiles =
            new TreeMap<File, HistoryID>(new Comparator<File>()
            {
                @Override
                public int compare(File o1, File o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

        getExistingFiles(srcFolder, Arrays.asList(rawid), recentFiles);

        // return non duplicate
        List<HistoryID> result = new ArrayList<HistoryID>();
        for(Map.Entry<File, HistoryID> entry : recentFiles.entrySet())
        {
            HistoryID hid = entry.getValue();

            if(result.contains(hid))
                continue;

            result.add(hid);
        }

        return result;
    }

    /**
     * Get existing files in <tt>res</tt> and their corresponding historyIDs.
     * @param sourceFolder the folder to search into.
     * @param rawID the rawID.
     * @param res the result map.
     */
    private void getExistingFiles(
        File sourceFolder, List<String> rawID,
        Map<File, HistoryID> res)
    {
        for(File f : sourceFolder.listFiles())
        {
            if(f.isDirectory())
            {
                List<String> newRawID = new ArrayList<String>(rawID);
                newRawID.add(f.getName());

                getExistingFiles(f, newRawID, res);
            }
            else
            {
                if(f.getName().equals(DATA_FILE))
                    continue;

                res.put(f, HistoryID.createFromRawStrings(
                    rawID.toArray(new String[rawID.size()])));
            }
        }
    }
}
