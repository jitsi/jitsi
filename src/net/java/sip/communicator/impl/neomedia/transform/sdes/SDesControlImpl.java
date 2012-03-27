/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.sdes;

import gnu.java.zrtp.utils.ZrtpFortuna;

import java.util.*;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.zrtp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Default implementation of {@link SDesControl} that supports the crypto suites
 * of the original RFC4568 and the KDR parameter, but nothing else.
 *
 * @author Ingo Bauersachs
 */
public class SDesControlImpl
    implements SDesControl
{
    /**
     * List of enabled crypto suites.
     */
    private List<String> enabledCryptoSuites = new ArrayList<String>(3)
    {
        private static final long serialVersionUID = 0L;

        {
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32);
            add(SrtpCryptoSuite.F8_128_HMAC_SHA1_80);
        }
    };


    /**
     * List of supported crypto suites.
     */
    private final List<String> supportedCryptoSuites = new ArrayList<String>(3)
     {
        private static final long serialVersionUID = 0L;

        {
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32);
            add(SrtpCryptoSuite.F8_128_HMAC_SHA1_80);
        }
     };

    private SrtpSDesFactory sdesFactory;
    private SrtpCryptoAttribute[] attributes;
    private SDesTransformEngine engine;
    private SrtpCryptoAttribute selectedInAttribute;
    private SrtpCryptoAttribute selectedOutAttribute;
    private SrtpListener srtpListener;

    /**
     * SDESControl
     */
    public SDesControlImpl()
    {
        sdesFactory = new SrtpSDesFactory();
        Random r = new Random()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void nextBytes(byte[] bytes)
            {
                ZrtpFortuna.getInstance().getFortuna().nextBytes(bytes);
            }
        };
        sdesFactory.setRandomGenerator(r);
    }

    public void setEnabledCiphers(Iterable<String> ciphers)
    {
        enabledCryptoSuites.clear();
        for(String c : ciphers)
            enabledCryptoSuites.add(c);
    }

    public Iterable<String> getSupportedCryptoSuites()
    {
        return Collections.unmodifiableList(supportedCryptoSuites);
    }

    public void cleanup()
    {
        if (engine != null) 
        {
            engine.close();
            engine = null;
        }
    }

    public void setSrtpListener(SrtpListener srtpListener)
    {
        this.srtpListener = srtpListener;
    }

    public SrtpListener getSrtpListener()
    {
        return srtpListener;
    }

    public boolean getSecureCommunicationStatus()
    {
        return engine != null;
    }

    /**
     * Not used.
     * @param masterSession not used.
     */
    public void setMasterSession(boolean masterSession)
    {}

    public void start(MediaType type)
    {
        srtpListener.securityTurnedOn(
            type.equals(MediaType.AUDIO) ?
                SecurityEventManager.AUDIO_SESSION
                : SecurityEventManager.VIDEO_SESSION,
            selectedInAttribute.getCryptoSuite().encode(), this);
    }

    public void setMultistream(SrtpControl master)
    {
    }

    public TransformEngine getTransformEngine()
    {
        if(engine == null)
            engine = new SDesTransformEngine(this);
        return engine;
    }

    public String[] getInitiatorCryptoAttributes()
    {
        if(attributes == null)
        {
            attributes = new SrtpCryptoAttribute[enabledCryptoSuites.size()];
            for (int i = 0; i < attributes.length; i++)
            {
                attributes[i] =
                    sdesFactory.createCryptoAttribute(i + 1,
                        enabledCryptoSuites.get(i));
            }
        }
        String[] result = new String[attributes.length];
        for(int i = 0; i < attributes.length; i++)
            result[i] = attributes[i].encode();
        return result;
    }

    public String responderSelectAttribute(Iterable<String> peerAttributes)
    {
        for (String suite : enabledCryptoSuites)
        {
            for (String ea : peerAttributes)
            {
                SrtpCryptoAttribute peerCA = SrtpCryptoAttribute.create(ea);
                if (suite.equals(peerCA.getCryptoSuite().encode()))
                {
                    selectedInAttribute = peerCA;
                    selectedOutAttribute =
                        sdesFactory.createCryptoAttribute(1, suite);
                    return selectedOutAttribute.encode();
                }
            }
        }
        return null;
    }

    public boolean initiatorSelectAttribute(Iterable<String> peerAttributes)
    {
        for (SrtpCryptoAttribute localCA : attributes)
        {
            for (String ea : peerAttributes)
            {
                SrtpCryptoAttribute peerCA = SrtpCryptoAttribute.create(ea);
                if (localCA.getCryptoSuite().equals(peerCA.getCryptoSuite()))
                {
                    selectedInAttribute = peerCA;
                    selectedOutAttribute = localCA;
                    return true;
                }
            }
        }
        return false;
    }

    public SrtpCryptoAttribute getInAttribute()
    {
        return selectedInAttribute;
    }

    public SrtpCryptoAttribute getOutAttribute()
    {
        return selectedOutAttribute;
    }

    public void setConnector(AbstractRTPConnector newValue)
    {
    }

    /**
     * Returns true, SDES always requires the secure transport of its keys.
     *
     * @return true
     */
    public boolean requiresSecureSignalingTransport()
    {
        return true;
    }
}
