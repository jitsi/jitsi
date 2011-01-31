/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.packetlogging;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.packetlogging.*;

/**
 * Extends PacketLoggingConfiguration by storing and loading values from
 * configuration service.
 *
 * @author Damian Minkov
 */
public class PacketLoggingConfigurationImpl
    extends PacketLoggingConfiguration
{
    /**
     * Creates new PacketLoggingConfiguration and load values from
     * configuration service and if missing uses already defined
     * default values.
     */
    PacketLoggingConfigurationImpl()
    {
        // load values from config service
        ConfigurationService configService =
                PacketLoggingActivator.getConfigurationService();

        super.setGlobalLoggingEnabled(
            configService.getBoolean(
                PACKET_LOGGING_ENABLED_PROPERTY_NAME,
                isGlobalLoggingEnabled()));
        super.setSipLoggingEnabled(
            configService.getBoolean(
                PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME,
                isSipLoggingEnabled()));
        super.setJabberLoggingEnabled(
            configService.getBoolean(
                PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME,
                isJabberLoggingEnabled()));
        super.setRTPLoggingEnabled(
            configService.getBoolean(
                PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME,
                isRTPLoggingEnabled()));
        super.setIce4JLoggingEnabled(
            configService.getBoolean(
                PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME,
                isIce4JLoggingEnabled()));
        super.setLimit(
            configService.getLong(
                PACKET_LOGGING_FILE_SIZE_PROPERTY_NAME,
                getLimit()));
        super.setLogfileCount(
            configService.getInt(
                PACKET_LOGGING_FILE_COUNT_PROPERTY_NAME,
                getLogfileCount()));
    }

    /**
     * Change whether packet logging is enabled and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setGlobalLoggingEnabled(boolean enabled)
    {
        super.setGlobalLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().setProperty(
            PACKET_LOGGING_ENABLED_PROPERTY_NAME, enabled);
    }

    /**
     * Change whether packet logging for sip protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setSipLoggingEnabled(boolean enabled)
    {
        super.setSipLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().setProperty(
            PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Change whether packet logging for jabber protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setJabberLoggingEnabled(boolean enabled)
    {
        super.setJabberLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().setProperty(
            PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Change whether packet logging for RTP is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setRTPLoggingEnabled(boolean enabled)
    {
        super.setRTPLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().setProperty(
            PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Change whether packet logging for Ice4J is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setIce4JLoggingEnabled(boolean enabled)
    {
        super.setIce4JLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().setProperty(
            PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Changes the file size limit.
     * @param limit the new limit size.
     */
    public void setLimit(long limit)
    {
        super.setLimit(limit);

        PacketLoggingActivator.getConfigurationService().setProperty(
                PACKET_LOGGING_FILE_SIZE_PROPERTY_NAME,
                limit);
    }

    /**
     * Changes file count.
     * @param logfileCount the new file count.
     */
    public void setLogfileCount(int logfileCount)
    {
        super.setLogfileCount(logfileCount);

        PacketLoggingActivator.getConfigurationService().setProperty(
                PACKET_LOGGING_FILE_COUNT_PROPERTY_NAME,
                logfileCount);
    }
}
