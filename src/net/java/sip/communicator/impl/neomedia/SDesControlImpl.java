package net.java.sip.communicator.impl.neomedia;

import ch.imvs.sdes4j.srtp.*;

import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.impl.neomedia.transform.sdes.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;

public class SDesControlImpl
    implements SDesControl
{
    private String[] supportedCryptoSuites = new String[]
        {
            SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80,
            //SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32,
            //SrtpCryptoSuite.F8_128_HMAC_SHA1_80
        };
    private SrtpSDesFactory sdesFactory = new SrtpSDesFactory();
    private SrtpCryptoAttribute[] attributes;
    private SDesTransformEngine engine;
    private SrtpCryptoAttribute selectedInAttribute;
    private SrtpCryptoAttribute selectedOutAttribute;

    public SDesControlImpl()
    {
    }

    public void cleanup()
    {
    }

    public void setZrtpListener(SrtpListener zrtpListener)
    {
    }

    public SrtpListener getSrtpListener()
    {
        return null;
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
        attributes = new SrtpCryptoAttribute[supportedCryptoSuites.length];
        for (int i = 0; i < attributes.length; i++)
        {
            attributes[i] =
                sdesFactory.createCryptoAttribute(i + 1,
                    supportedCryptoSuites[i]);
        }
        String[] result = new String[attributes.length];
        for(int i = 0; i < attributes.length; i++)
            result[i] = attributes[i].encode();
        return result;
    }

    public String responderSelectAttribute(String[] peerAttributes)
    {
        for (String suite : supportedCryptoSuites)
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

    public void initiatorSelectAttribute(String[] peerAttributes)
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
                    return;
                }
            }
        }
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
