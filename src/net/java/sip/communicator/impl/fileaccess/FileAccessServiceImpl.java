/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.fileaccess;

import java.io.File;
import java.io.IOException;

import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.fileaccess.FileAccessService;
import net.java.sip.communicator.util.Assert;
import net.java.sip.communicator.util.Logger;

/**
 * Default FileAccessService implementation.
 * 
 * @author Alexander Pelov
 */
public class FileAccessServiceImpl implements FileAccessService {

	/**
	 * The logger for this class.
	 */
	private static Logger logger = Logger
			.getLogger(FileAccessServiceImpl.class);

	/**
	 * The file prefix for all temp files.
	 */
	public static final String TEMP_FILE_PREFIX = "SIPCOMM";

	/**
	 * The file suffix for all temp files.
	 */
	public static final String TEMP_FILE_SUFFIX = "TEMP";

	/**
	 * List of available configuration services.
	 */
    private ConfigurationService configurationService = null;
    
    /**
     * An synchronization object.
     * 
     * A lock should be obtained whenever the configuration service is 
     * accessed.
     */
    private Object syncRoot = new Object();

    /**
     * Set the configuration service.
     * 
     * @param configurationService
     */
    public void setConfigurationService(
    		ConfigurationService configurationService)
    {    	
    	synchronized(this.syncRoot) {
        	this.configurationService = configurationService;
        	logger.debug("New configuration service registered.");
		}
    }
    
    /**
     * Remove a configuration service.
     * 
     * @param configurationService
     */
    public void unsetConfigurationService(
    		ConfigurationService configurationService)
    {
    	synchronized(this.syncRoot) {
    		if(this.configurationService == configurationService) {
    			this.configurationService = null;
            	logger.debug("Configuration service unregistered.");
    		}
		}
    }

	public File getTemporaryFile() throws IOException {
		File retVal = null;

		try {
			logger.logEntry();

			retVal = TempFileManager.createTempFile(TEMP_FILE_PREFIX,
					TEMP_FILE_SUFFIX);
		} finally {
			logger.logExit();
		}

		return retVal;
	}
    
    public File getTemporaryDirectory() throws IOException {
        File file = getTemporaryFile();
        
        if(!file.delete()) {
            throw new IOException("Could not create temporary directory, " +
                    "because: could not delete temporary file.");
        }
        if(!file.mkdirs()) {
            throw new IOException("Could not create temporary directory");
        }
        
        return file;
    }
    
	/**
	 * @throws IllegalStateException
	 *             Thrown if the configuration service is not set
	 */
	public File getPrivatePersistentFile(String fileName) throws Exception {
		Assert.assertNonNull(fileName, "Parameter fileName should be non-null");
		
		File file = null;

		try {
			logger.logEntry();

			
            String fullPath = getFullPath(fileName);
			file = this.accessibleFile(fullPath, fileName);

			if(file == null) {
				throw new SecurityException("Insufficient rights to access " +
						"this file in current user's home directory: "
						+ file.getAbsolutePath());
			}
		} finally {
			logger.logExit();
		}

		return file;
	}
    
    public File getPrivatePersistentDirectory(String dirName) 
        throws Exception
    {
		Assert.assertNonNull(dirName, "Parameter dirName should be non-null");
    	
        String fullPath = getFullPath(dirName);
        File dir = new File(fullPath, dirName);
    	
        if(dir.exists()) {
        	if(!dir.isDirectory()) {
	        	throw new RuntimeException("Could not create directory " +
	        			"because: A file exists with this name:" + 
	        			dir.getAbsolutePath());
            }
        } else {
            if(!dir.mkdirs()) {
                throw new IOException("Could not create directory");
            }
        }
        
        return dir;
    }
    
    public File getPrivatePersistentDirectory(String[] dirNames) throws Exception {
		Assert.assertNonNull(dirNames, "Parameter dirNames should be non-null");
		Assert.assertTrue(dirNames.length > 0, "dirNames.length should be > 0");
		
    	StringBuffer dirName = new StringBuffer();
    	for(int i = 0; i < dirNames.length; i++) {
    		if(i > 0) {
    			dirName.append(File.separatorChar);
    		}
    		dirName.append(dirNames[i]);
    	}
    	
    	return getPrivatePersistentDirectory(dirName.toString());
    }

    private String getFullPath(String fileName) {
        Assert.assertNonNull(fileName, "The filename should be non-null.");
        
    	String userhome = null;
    	String sipSubdir = null;
    	
    	// Obtain configuration service lock
    	synchronized(this.syncRoot) {
            Assert.assertNonNull(this.configurationService, 
    			"The configurationService should be non-null.");

            userhome = this.configurationService.getString(FileAccessService.CONFPROPERTYKEY_USER_HOME);
            sipSubdir = this.configurationService.getString(FileAccessService.CONFPROPERTYKEY_SIP_DIRECTORY);
		}
    	

        if(userhome == null) {
            userhome = System.getProperty(FileAccessService.SYSPROPERTYKEY_USER_HOME);
            if(userhome == null) {
                throw new IllegalStateException("No user home directory specified in system's environment");
            }
        }
        if(sipSubdir == null) {
            sipSubdir = FileAccessService.DEFAULT_SIP_DIRECTORY;
        }
        
        if(!userhome.endsWith(File.separator)) {
            userhome += File.separator;
        }
        if(!sipSubdir.endsWith(File.separator)) {
        	sipSubdir += File.separator;
        }
        
        return userhome + sipSubdir;
    }


	/**
	 * Checks if a file exists and if it is writable or readable. If not - checks 
     * if the user has a write privileges to the containing directory.
	 * 
	 * If those conditions are met it returns a File in the directory with a
	 * fileName. If not - returns null.
	 * 
	 * @param homedir
	 * @param fileName
	 * @return Returns null if the file does not exist and cannot be created.
	 *         Otherwise - an object to this file
	 * @throws IOException Thrown if the home directory cannot be created
	 */
	private File accessibleFile(String homedir, String fileName) throws IOException {
		File file = null;
		
		try {
			logger.logEntry();
		
			homedir = homedir.trim();
			if(!homedir.endsWith(File.separator)) {
				homedir += File.separator;
			}
			
			file = new File(homedir + fileName);
			if (file.canRead() || file.canWrite()) {
				return file;
			}
	
			File homedirFile = new File(homedir);
			
			if(!homedirFile.exists()) {
				logger.debug("Creating home directory : " + homedirFile.getAbsolutePath());
				if(!homedirFile.mkdirs()) {
					String message = "Could not create the home directory : " 
						+ homedirFile.getAbsolutePath();
					
					logger.debug(message);
					throw new IOException(message);
				}
				logger.debug("Home directory created : " + homedirFile.getAbsolutePath());
			} else if (!homedirFile.canWrite()) {
				file = null;
			}
			
		} finally {
			logger.logExit();
		}
	
		return file;
	}

}
