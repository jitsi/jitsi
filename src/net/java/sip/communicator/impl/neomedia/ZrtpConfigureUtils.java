/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.configuration.*;
import gnu.java.zrtp.ZrtpConfigure;
import gnu.java.zrtp.ZrtpConstants;

public class ZrtpConfigureUtils
{
    public static <T extends Enum<T>>String getPropertyID(T algo)
    {
        Class<T> clazz = algo.getDeclaringClass();
        return "net.java.sip.communicator." + clazz.getName().replace('$', '_');
    }

    public static ZrtpConfigure getZrtpConfiguration()
    {
        ZrtpConfigure active = new ZrtpConfigure();
        setupConfigure(ZrtpConstants.SupportedPubKeys.DH2K, active);
        setupConfigure(ZrtpConstants.SupportedHashes.S256, active);
        setupConfigure(ZrtpConstants.SupportedSymCiphers.AES1, active);
        setupConfigure(ZrtpConstants.SupportedSASTypes.B32, active);
        setupConfigure(ZrtpConstants.SupportedAuthLengths.HS32, active);

        return active;
    }

    private static <T extends Enum<T>> void
        setupConfigure(T algo, ZrtpConfigure active)
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        String savedConf = null;

        if (cfg != null)
        {
            String id = ZrtpConfigureUtils.getPropertyID(algo);

            savedConf = cfg.getString(id);
        }
        if (savedConf == null)
            savedConf = "";

        Class <T> clazz = algo.getDeclaringClass();
        String savedAlgos[] = savedConf.split(";");

        // Configure saved algorithms as active
        for (String str : savedAlgos)
        {
            try
            {
                T algoEnum = Enum.valueOf(clazz, str);

                if (algoEnum != null)
                    active.addAlgo(algoEnum);
            }
            catch (IllegalArgumentException iae)
            {
                // Ignore it and continue the loop.
            }
        }
    }
}
