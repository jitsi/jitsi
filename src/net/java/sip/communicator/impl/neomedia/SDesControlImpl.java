/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.sdes.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.event.CallPeerSecurityStatusEvent;

/**
 * Default implementation of {@link SDesControl}.
 * 
 * @author Ingo Bauersachs
 */
public class SDesControlImpl
    implements SDesControl
{
    private List<String> enabledCryptoSuites = new ArrayList<String>(3)
        {{
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32);
            add(SrtpCryptoSuite.F8_128_HMAC_SHA1_80);
        }};

    private final List<String> supportedCryptoSuites = new ArrayList<String>(3)
        {{
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80);
            add(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32);
            add(SrtpCryptoSuite.F8_128_HMAC_SHA1_80);
        }};

    private SrtpSDesFactory sdesFactory = new SrtpSDesFactory();
    private SrtpCryptoAttribute[] attributes;
    private SDesTransformEngine engine;
    private SrtpCryptoAttribute selectedInAttribute;
    private SrtpCryptoAttribute selectedOutAttribute;
    private SrtpListener srtpListener;

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

    public void setSASVerification(boolean verified)
    {
    }

    public void start(boolean masterSession)
    {
        srtpListener.securityTurnedOn(
            masterSession ? 
                CallPeerSecurityStatusEvent.AUDIO_SESSION :
                CallPeerSecurityStatusEvent.VIDEO_SESSION,
            selectedInAttribute.getCryptoSuite().encode(), null, true, null);
    }

    public void setMultistream(byte[] multiStreamData)
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

        if(engine != null)
            engine.reset(this);

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
                    if(engine != null)
                        engine.reset(this);
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
                    if(engine != null)
                        engine.reset(this);
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
}
