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
package net.java.sip.communicator.impl.configuration;

import java.beans.*;
import java.io.*;
import java.sql.*;
import java.sql.Statement;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.*;

import com.google.common.collect.*;

/**
 * Implementation of the {@link ConfigurationService} based on JDBC.
 * 
 * @author Ingo Bauersachs
 */
public final class JdbcConfigService
    implements ConfigurationService
{
    /**
     * The <tt>Logger</tt> used by this class.
     */
    private final Logger logger
        = Logger.getLogger(JdbcConfigService.class);

    /**
     * Name of the file containing default properties.
     */
    private static final String DEFAULT_PROPS_FILE_NAME
        = "jitsi-defaults.properties";

    /**
     * Name of the file containing overrides (possibly set by the distributor)
     * for any of the default properties.
     */
    private static final String DEFAULT_OVERRIDES_PROPS_FILE_NAME
        = "jitsi-default-overrides.properties";

    /**
     * A set of immutable properties deployed with the application during
     * install time. The properties in this file will be impossible to override
     * and attempts to do so will simply be ignored.
     * @see #defaultProperties
     */
    private Map<String, String> immutableDefaultProperties
        = new HashMap<String, String>();

    /**
     * A set of properties deployed with the application during install time.
     * Contrary to the properties in {@link #immutableDefaultProperties} the
     * ones in this map can be overridden with call to the
     * <tt>setProperty()</tt> methods. Still, re-setting one of these properties
     * to <tt>null</tt> would cause for its initial value to be restored.
     */
    private Map<String, String> defaultProperties
        = new HashMap<String, String>();

    /**
     * Registered property change listeners that may veto a change.
     */
    private SetMultimap<String, ConfigVetoableChangeListener> vetoListeners
        = HashMultimap.create();

    /**
     * Registered property change listeners.
     */
    private SetMultimap<String, PropertyChangeListener> listeners
        = HashMultimap.create();

    /**
     * Connection to the JDBC database.
     */
    private Connection connection;

    // SQL statements for queries against the database
    private PreparedStatement selectExact;
    private PreparedStatement selectLike;
    private PreparedStatement selectAll;
    private PreparedStatement insertOrUpdate;
    private PreparedStatement delete;

    /**
     * Reference to the {@link FileAccessService}.
     */
    private FileAccessService fas; 

    /**
     * Creates a new instance of this class.
     * @param fas Reference to the {@link FileAccessService}.
     * @throws Exception
     */
    public JdbcConfigService(FileAccessService fas) throws Exception
    {
        this.fas = fas;
        File dataFile = fas.getPrivatePersistentFile(
            "props.hsql.script",
            FileCategory.PROFILE);
        File oldProps = fas.getPrivatePersistentFile(
            "sip-communicator.properties",
            FileCategory.PROFILE);

        // if the file for the current database does not exist yet but
        // the previous properties-based file is there, migrate it
        boolean migrate = false;
        if (!dataFile.exists() && oldProps.exists())
        {
            migrate = true;
        }

        // open the connection
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        checkConnection();

        // then do the actual migration
        if (migrate)
        {
            Properties p = new Properties();
            p.load(new FileInputStream(oldProps));

            this.connection.setAutoCommit(false);
            for (Map.Entry<Object, Object> e : p.entrySet())
            {
                this.setProperty(e.getKey().toString(), e.getValue(), false);
            }

            this.connection.commit();
            this.connection.setAutoCommit(true);
        }

        // and finally load the (mandatory) system properties
        loadDefaultProperties(DEFAULT_PROPS_FILE_NAME);
        loadDefaultProperties(DEFAULT_OVERRIDES_PROPS_FILE_NAME);
    }

    /**
     * Verifies that the connection to the database and all prepared statement
     * are valid.
     * 
     * @throws SQLException
     */
    private void checkConnection() throws SQLException
    {
        if (this.connection != null && this.connection.isValid(1))
        {
            try
            {
                PreparedStatement st = this.connection.prepareStatement(
                    "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
                if (st.execute())
                {
                    return;
                }
            }
            catch(Exception e)
            {
                this.connection = null;
                logger.error("Database connection is invalid, recreating", e);
            }
        }

        String filename;
        try
        {
            File f = fas.getPrivatePersistentFile(
                "props.hsql",
                FileCategory.PROFILE);
            filename = f.getAbsolutePath();
        }
        catch (Exception e)
        {
            throw new SQLException(e);
        }

        this.connection = DriverManager.getConnection(
            "jdbc:hsqldb:file:"
            + filename
            + ";shutdown=true;hsqldb.write_delay=false;"
            + "hsqldb.write_delay_millis=0");
        Statement st = this.connection.createStatement();
        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS Props ("
            + "k LONGVARCHAR UNIQUE, v LONGVARCHAR"
            + ")");

        this.selectExact = this.connection.prepareStatement(
            "SELECT v FROM Props WHERE k=?");
        this.selectLike = this.connection.prepareStatement(
            "SELECT k, v FROM Props WHERE k LIKE ?");
        this.selectAll = this.connection.prepareStatement(
            "SELECT k, v FROM Props");
        this.insertOrUpdate = this.connection.prepareStatement(
            "MERGE INTO Props"
                + " USING (VALUES(?,?)) AS i(k,v) ON Props.k = i.k"
                + " WHEN MATCHED THEN UPDATE SET Props.v = i.v"
                + " WHEN NOT MATCHED THEN INSERT (k, v) VALUES (i.k, i.v)");
        this.delete = this.connection.prepareStatement(
            "DELETE FROM Props WHERE k=?");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#setProperty(java
     * .lang.String, java.lang.Object)
     */
    @Override
    public synchronized void setProperty(String propertyName, Object property)
    {
        this.setProperty(propertyName, property, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#setProperty(java
     * .lang.String, java.lang.Object, boolean)
     */
    @Override
    public synchronized void setProperty(String propertyName, Object property,
        boolean isSystem)
    {
        // a property with the same name as an existing system property cannot
        // be set, so mark it as a system property
        if (!isSystem && System.getProperty(propertyName) != null)
        {
            isSystem = true;
        }

        if (isSystem)
        {
            if (property == null)
            {
                System.clearProperty(propertyName);
                return;
            }

            System.setProperty(propertyName, property.toString());
        }
        else
        {
            if (immutableDefaultProperties.containsKey(propertyName))
            {
                return;
            }

            try
            {
                this.checkConnection();
                Object oldValue = this.getProperty(propertyName);
                this.fireVetoableChange(propertyName, oldValue, property);
                if (property == null)
                {
                    this.delete.setString(1, propertyName);
                    this.delete.execute();
                }
                else
                {
                    this.insertOrUpdate.setString(1, propertyName);
                    this.insertOrUpdate.setString(2, property.toString());
                    this.insertOrUpdate.execute();
                }

                this.fireChange(propertyName, oldValue, property);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#setProperties(java
     * .util.Map)
     */
    @Override
    public synchronized void setProperties(Map<String, Object> properties)
    {
        try
        {
            checkConnection();
            this.connection.setAutoCommit(false);
            for (Map.Entry<String, Object> e : properties.entrySet())
            {
                this.setProperty(e.getKey(), e.getValue(), false);
            }

            this.connection.commit();
            this.connection.setAutoCommit(true);
        }
        catch (SQLException e1)
        {
            throw new RuntimeException(e1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getProperty(java
     * .lang.String)
     */
    @Override
    public synchronized Object getProperty(String propertyName)
    {
        Object value = immutableDefaultProperties.get(propertyName);
        if (value != null)
        {
            return value;
        }

        try
        {
            this.checkConnection();
            this.selectExact.setString(1, propertyName);
            ResultSet q = this.selectExact.executeQuery();
            if (q.next())
            {
                value = q.getString(1);
            }
        }
        catch (SQLException e)
        {
            logger.error(e);
            throw new RuntimeException(e);
        }

        if (value != null)
        {
            return value;
        }

        value = defaultProperties.get(propertyName);
        if (value != null)
        {
            return value;
        }

        return System.getProperty(propertyName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#removeProperty(java
     * .lang.String)
     */
    @Override
    public synchronized void removeProperty(String propertyName)
    {
        //remove all properties
        for (String child : this.getPropertyNamesByPrefix(propertyName, false))
        {
            this.setProperty(child, null, false);
        }

        this.setProperty(propertyName, null, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getAllPropertyNames
     * ()
     */
    @Override
    public List<String> getAllPropertyNames()
    {
        List<String> data = new ArrayList<String>(
            immutableDefaultProperties.keySet());
        data.addAll(defaultProperties.keySet());
        try
        {
            this.checkConnection();
            ResultSet q = this.selectAll.executeQuery();
            while (q.next())
            {
                data.add(q.getString(1));
            }
        }
        catch (SQLException e)
        {
            logger.error(e);
            throw new RuntimeException(e);
        }

        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService
     * #getPropertyNamesByPrefix(java.lang.String, boolean)
     */
    @Override
    public List<String> getPropertyNamesByPrefix(String prefix,
        boolean exactPrefixMatch)
    {
        try
        {
            List<String> resultSet = new ArrayList<String>(50);
            this.checkConnection();
            this.selectLike.setString(1, prefix + "%");
            ResultSet q = this.selectLike.executeQuery();
            while (q.next())
            {
                String key = q.getString(1);

                if(exactPrefixMatch)
                {
                    int ix = key.lastIndexOf('.');
                    if(ix == -1)
                    {
                        continue;
                    }

                    String keyPrefix = key.substring(0, ix);

                    if(prefix.equals(keyPrefix))
                    {
                        resultSet.add(key);
                    }
                }
                else
                {
                    if(key.startsWith(prefix))
                    {
                        resultSet.add(key);
                    }
                }
            }

            return resultSet;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService
     * #getPropertyNamesBySuffix(java.lang.String)
     */
    @Override
    public List<String> getPropertyNamesBySuffix(String suffix)
    {
        try
        {
            List<String> resultKeySet = new ArrayList<String>(20);
            this.checkConnection();
            this.selectLike.setString(1, "%" + suffix);
            ResultSet q = this.selectLike.executeQuery();
            while (q.next())
            {
                String key = q.getString(1);
                int ix = key.lastIndexOf('.');
                if (ix != -1 && suffix.equals(key.substring(ix + 1)))
                    resultKeySet.add(key);
            }

            return resultKeySet;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getString(java.lang
     * .String)
     */
    @Override
    public String getString(String propertyName)
    {
        String value = (String)this.getProperty(propertyName);
        if (value != null)
        {
            value = value.trim();
            if (value.length() == 0)
            {
                return null;
            }
        }

        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getString(java.lang
     * .String, java.lang.String)
     */
    @Override
    public String getString(String propertyName, String defaultValue)
    {
        String value = this.getString(propertyName);
        if (value == null)
        {
            return defaultValue;
        }

        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getBoolean(java.
     * lang.String, boolean)
     */
    @Override
    public boolean getBoolean(String propertyName, boolean defaultValue)
    {
        Object value = this.getProperty(propertyName);
        if (value == null)
        {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getInt(java.lang
     * .String, int)
     */
    @Override
    public int getInt(String propertyName, int defaultValue)
    {
        Object value = this.getProperty(propertyName);
        if (value == null || "".equals(value.toString()))
        {
            return defaultValue;
        }

        try
        {
            return Integer.parseInt(value.toString());
        }
        catch (NumberFormatException ex)
        {
            logger.error(String.format(
                "'%s' for property %s not an integer, returning default (%s)",
                value, propertyName, defaultValue), ex);
            return defaultValue;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getLong(java.lang
     * .String, long)
     */
    @Override
    public long getLong(String propertyName, long defaultValue)
    {
        Object value = this.getProperty(propertyName);
        if (value == null || "".equals(value.toString()))
        {
            return defaultValue;
        }

        try
        {
            return Long.parseLong(value.toString());
        }
        catch (NumberFormatException ex)
        {
            logger.error(String.format(
                "'%s' for property %s not a long, returning default (%s)",
                value, propertyName, defaultValue), ex);
            return defaultValue;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.listeners.put(null, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.listeners.remove(null, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * addPropertyChangeListener(java.lang.String,
     * java.beans.PropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(String propertyName,
        PropertyChangeListener listener)
    {
        this.listeners.put(propertyName, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * removePropertyChangeListener(java.lang.String,
     * java.beans.PropertyChangeListener)
     */
    @Override
    public void removePropertyChangeListener(String propertyName,
        PropertyChangeListener listener)
    {
        this.listeners.remove(propertyName, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * addVetoableChangeListener
     * (org.jitsi.service.configuration.ConfigVetoableChangeListener)
     */
    @Override
    public void addVetoableChangeListener(ConfigVetoableChangeListener listener)
    {
        this.vetoListeners.put(null, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * removeVetoableChangeListener
     * (org.jitsi.service.configuration.ConfigVetoableChangeListener)
     */
    @Override
    public void removeVetoableChangeListener(
        ConfigVetoableChangeListener listener)
    {
        this.vetoListeners.remove(null, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * addVetoableChangeListener(java.lang.String,
     * org.jitsi.service.configuration.ConfigVetoableChangeListener)
     */
    @Override
    public void addVetoableChangeListener(String propertyName,
        ConfigVetoableChangeListener listener)
    {
        this.vetoListeners.put(propertyName, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jitsi.service.configuration.ConfigurationService#
     * removeVetoableChangeListener(java.lang.String,
     * org.jitsi.service.configuration.ConfigVetoableChangeListener)
     */
    @Override
    public void removeVetoableChangeListener(String propertyName,
        ConfigVetoableChangeListener listener)
    {
        this.vetoListeners.remove(propertyName, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#storeConfiguration()
     */
    @Override
    public void storeConfiguration() throws IOException
    {
        try
        {
            this.connection.close();
        }
        catch (SQLException e)
        {
            logger.error(e);
        }
        finally
        {
            this.connection = null;
        }
    }

    /**
     * Does nothing. The database cannot be edited from the outside.
     */
    @Override
    public void reloadConfiguration() throws IOException
    {
        // nothing to do, the file cannot be edited outside
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#purgeStoredConfiguration
     * ()
     */
    @Override
    public void purgeStoredConfiguration()
    {
        try
        {
            this.checkConnection();
            Statement st = this.connection.createStatement();
            st.executeUpdate("TRUNCATE TABLE Props");
        }
        catch (SQLException e)
        {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getScHomeDirName()
     */
    @Override
    public String getScHomeDirName()
    {
        return System.getProperty(PNAME_SC_HOME_DIR_NAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getScHomeDirLocation
     * ()
     */
    @Override
    public String getScHomeDirLocation()
    {
        return System.getProperty(PNAME_SC_HOME_DIR_LOCATION);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jitsi.service.configuration.ConfigurationService#getConfigurationFilename
     * ()
     */
    @Override
    public String getConfigurationFilename()
    {
        return "props.hsql.script";
    }

    /**
     * Loads the specified default properties maps from the Jitsi installation
     * directory. Typically this file is to be called for the default properties
     * and the admin overrides.
     * 
     * @param fileName the name of the file we need to load.
     */
    private void loadDefaultProperties(String fileName)
    {
        try
        {
            Properties fileProps = new Properties();

            InputStream fileStream;
            if(OSUtils.IS_ANDROID)
            {
                fileStream
                        = getClass().getClassLoader()
                                .getResourceAsStream(fileName);
            }
            else
            {
                fileStream = ClassLoader.getSystemResourceAsStream(fileName);
            }

            fileProps.load(fileStream);
            fileStream.close();

            // now get those properties and place them into the mutable and
            // immutable properties maps.
            for (Map.Entry<Object, Object> entry : fileProps.entrySet())
            {
                String name  = (String) entry.getKey();
                String value = (String) entry.getValue();

                if (   name == null
                    || value == null
                    || name.trim().length() == 0)
                {
                    continue;
                }

                if (name.startsWith("*"))
                {
                    name = name.substring(1);

                    if(name.trim().length() == 0)
                    {
                        continue;
                    }

                    //it seems that we have a valid default immutable property
                    immutableDefaultProperties.put(name, value);

                    //in case this is an override, make sure we remove previous
                    //definitions of this property
                    defaultProperties.remove(name);
                }
                else
                {
                    //this property is a regular, mutable default property.
                    defaultProperties.put(name, value);

                    //in case this is an override, make sure we remove previous
                    //definitions of this property
                    immutableDefaultProperties.remove(name);
                }
            }
        }
        catch (Exception ex)
        {
            //we can function without defaults so we are just logging those.
            logger.info("No defaults property file loaded: " + fileName
                + ". Not a problem.");

            if(logger.isDebugEnabled())
                logger.debug("load exception", ex);
        }
    }

    /**
     * Notify all listening objects about a prospective change.
     * 
     * @param propertyName The property that is going to change.
     * @param oldValue The previous value of the property (can be <tt>null</tt>)
     * @param newValue The new value of the property (can be <tt>null</tt>)
     */
    private void fireVetoableChange(String propertyName,
        Object oldValue, Object newValue)
    {
        PropertyChangeEvent evt = new PropertyChangeEvent(
            this,
            propertyName,
            oldValue,
            newValue);

        for (ConfigVetoableChangeListener l : vetoListeners.get(propertyName))
        {
            l.vetoableChange(evt);
        }

        for (ConfigVetoableChangeListener l : vetoListeners.get(null))
        {
            l.vetoableChange(evt);
        }
    }

    /**
     * Notify all listeners that a property has changed.
     * 
     * @param propertyName The property that has just changed.
     * @param oldValue The previous value of the property (can be <tt>null</tt>)
     * @param newValue The new value of the property (can be <tt>null</tt>)
     */
    private void fireChange(String propertyName,
        Object oldValue, Object newValue)
    {
        PropertyChangeEvent evt = new PropertyChangeEvent(
            this,
            propertyName,
            oldValue,
            newValue);

        for (PropertyChangeListener l : listeners.get(propertyName))
        {
            l.propertyChange(evt);
        }

        for (PropertyChangeListener l : listeners.get(null))
        {
            l.propertyChange(evt);
        }
    }
}
