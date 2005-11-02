/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.java.sip.communicator.util.Logger;

import org.ungoverned.oscar.Noscar;
import org.ungoverned.oscar.cache.DefaultBundleCache;
import org.ungoverned.oscar.util.MutablePropertyResolver;
import org.ungoverned.oscar.util.MutablePropertyResolverImpl;


/**
 * This is the main entry point of the program. It launches the Oscar OSGI
 * Framework.
 * 
 * @author Alexander Pelov
 */
public class Main {
	
	public static final String CLIENT_RUN_PROPERTIES_FILE = "lib/oscar.client.run.properties";

	/**
	 * The logger for this class.
	 */
	private static Logger log = Logger.getLogger(Main.class);
	
	private static Noscar oscar;

	public static void main(String[] args) {
		
		int errorStatus = -1;
		
		try {
			log.logEntry();
			
			String oscarSystemPropertiesFile = System.getProperty("oscar.system.properties");
			if(oscarSystemPropertiesFile == null) {
				oscarSystemPropertiesFile = Main.CLIENT_RUN_PROPERTIES_FILE;
			}

	        Properties props = Main.loadProperties(oscarSystemPropertiesFile);
	        
			// See if the profile name property was specified.
	        String profileName = System.getProperty(DefaultBundleCache.CACHE_PROFILE_PROP);

	        // See if the profile directory property was specified.
	        String profileDirName = System.getProperty(DefaultBundleCache.CACHE_PROFILE_DIR_PROP);

	        // If no profile or profile directory is specified in the
	        // properties, then ask for a profile name.
	        if ((profileName == null) && (profileDirName == null))
	        {
	        	boolean cacheProfileSet = props.containsKey(DefaultBundleCache.CACHE_PROFILE_PROP);
	        	boolean cacheProfileDirSet = props.containsKey(DefaultBundleCache.CACHE_PROFILE_DIR_PROP);
	        	
	        	if(!cacheProfileSet && !cacheProfileDirSet) {
	        		props.put(DefaultBundleCache.CACHE_PROFILE_PROP, ".sip-communicator");
	        	}
	        }

    		MutablePropertyResolver propertyResolver = 
    			new MutablePropertyResolverImpl(props);
    		
	        Main.oscar = new Noscar();
	        Main.oscar.start(propertyResolver, null);
	        
	        errorStatus = 0;
		} catch(Exception e) {
			log.error("Error occured while starting Oscar OSGI", e);
			
			errorStatus = -1;
		} finally {
			log.logExit();
		}
		
		System.exit(errorStatus);
	}

	private static Properties loadProperties(String file) throws IOException {
		int pos = file.indexOf(':');
		
		// Test if the protocol is defined.
		// It is set if ':' exists in the filename and if
		// there is no path separator before it.
		//
		// Several characters are tested for file separator candidates:
		// Window's '\', Unix's '/', and the separator of the current
		// system.. Who knows.. maybe some day someone will use @ for file
		// separator on @ix.. ;)
		if(pos >= 0 &&
			Main.isPosBeforeChar(file, pos, File.separatorChar) &&
			Main.isPosBeforeChar(file, pos, '\\') &&
			Main.isPosBeforeChar(file, pos, '/'))
		{
			file = file.substring(pos+1);
		}
		
		InputStream in = new FileInputStream(file);
        
		Properties props = null;
		
		try {
			props = new Properties();
			props.load(in);
		} finally {
			try {
				in.close();
			} catch(Exception e) {}
		}
		
		return props;
	}

	private static boolean isPosBeforeChar(String text, int pos, char c) {
		int charPos = text.indexOf(c);
		
		return charPos < 0 || pos < charPos;
	}
	
}
