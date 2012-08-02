/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import ch.imvs.sdes4j.srtp.*;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.impl.neomedia.transform.sdes.*;
import org.jitsi.service.neomedia.*;

import org.jivesoftware.smack.packet.*;

/**
 * An implementation of the <tt>CallPeerMediaHandler</tt> abstract class for the
 * common part of Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 */
public abstract class AbstractCallPeerMediaHandlerJabberGTalkImpl
        <T extends AbstractCallPeerJabberGTalkImpl<?,?>>
    extends CallPeerMediaHandler<T>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallPeerMediaHandlerJabberImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractCallPeerMediaHandlerJabberGTalkImpl.class);

    /**
     * Indicates if the <tt>CallPeer</tt> will support </tt>inputevt</tt>
     * extension (i.e. will be able to be remote-controlled).
     */
    private boolean localInputEvtAware = false;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>AbstractCallPeerJabberGTalkImpl</tt> instance that
     * we will be managing media for.
     */
    public AbstractCallPeerMediaHandlerJabberGTalkImpl(T peer)
    {
        super(peer, peer);
    }

    /**
     * Gets the <tt>inputevt</tt> support: true for enable, false for disable.
     *
     * @return The state of inputevt support: true for enable, false for
     * disable.
     */
    public boolean getLocalInputEvtAware()
    {
        return this.localInputEvtAware;
    }

    /**
     * Enable or disable <tt>inputevt</tt> support (remote-control).
     *
     * @param enable new state of inputevt support
     */
    public void setLocalInputEvtAware(boolean enable)
    {
        localInputEvtAware = enable;
    }

    /**
     * Detects and adds ZRTP available encryption method present in the
     * description given in parameter.
     *
     * @param isInitiator True if the local call instance is the initiator of
     * the call. False otherwise.
     * @param description The DESCRIPTION element of the JINGLE element which
     * contains the PAYLOAD-TYPE and (more important here) the ENCRYPTION.
     * @param mediaType The type of media (AUDIO or VIDEO).
     */
    protected void addZRTPAdvertisedEncryptions(
            boolean isInitiator,
            RtpDescriptionPacketExtension description,
            MediaType mediaType)
    {
        // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION element for
        // a given DESCRIPTION.
        EncryptionPacketExtension encryptionPacketExtension
            = description.getFirstChildOfType(
                EncryptionPacketExtension.class);
        if(encryptionPacketExtension != null)
        {
            AccountID accountID
                = getPeer().getProtocolProvider().getAccountID();

            // ZRTP
            ZrtpHashPacketExtension zrtpHashPacketExtension =
                encryptionPacketExtension.getFirstChildOfType(
                    ZrtpHashPacketExtension.class);

            if(zrtpHashPacketExtension != null
                && zrtpHashPacketExtension.getValue() != null
                && accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true)
                && getPeer().getCall().isSipZrtpAttribute())
            {
                addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
            }
        }
    }

    /**
     * Detects and adds SDES available encryption method present in the
     * description given in parameter.
     *
     * @param isInitiator True if the local call instance is the initiator of
     * the call. False otherwise.
     * @param description The DESCRIPTION element of the JINGLE element which
     * contains the PAYLOAD-TYPE and (more important here) the ENCRYPTION.
     * @param mediaType The type of media (AUDIO or VIDEO).
     */
    protected void addSDESAdvertisedEncryptions(
            boolean isInitiator,
            RtpDescriptionPacketExtension description,
            MediaType mediaType)
    {
        // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION element for
        // a given DESCRIPTION.
        EncryptionPacketExtension encryptionPacketExtension
            = description.getFirstChildOfType(
                EncryptionPacketExtension.class);
        if(encryptionPacketExtension != null)
        {
            AccountID accountID
                = getPeer().getProtocolProvider().getAccountID();

            // SDES
            if(accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.SDES_ENABLED,
                        false)
                    && accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true))
            {
                Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                    = getSrtpControls();
                MediaTypeSrtpControl key
                    = new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
                SrtpControl control = srtpControls.get(key);
                if(control == null)
                {
                    control
                        = JabberActivator.getMediaService().createSDesControl();
                    srtpControls.put(key, control);
                }

                SDesControl tmpSDesControl = (SDesControl) control;
                SrtpCryptoAttribute selectedSdes = selectSdesCryptoSuite(
                        isInitiator,
                        tmpSDesControl,
                        encryptionPacketExtension);

                if(selectedSdes != null)
                {
                    //found an SDES answer, remove all other controls
                    Iterator<Map.Entry<MediaTypeSrtpControl, SrtpControl>> iter
                            = srtpControls.entrySet().iterator();

                    while (iter.hasNext())
                    {
                        Map.Entry<MediaTypeSrtpControl, SrtpControl> entry
                            = iter.next();
                        MediaTypeSrtpControl mtsc = entry.getKey();

                        if ((mtsc.mediaType == mediaType)
                             && (mtsc.srtpControlType != SrtpControlType.SDES))
                        {
                            entry.getValue().cleanup();
                            iter.remove();
                        }
                    }

                    addAdvertisedEncryptionMethod(SrtpControlType.SDES);
                }
                else
                {
                    control.cleanup();
                    srtpControls.remove(key);
                }
            }
        }
        // If we were initiating the encryption, and the remote peer does not
        // manage it, then we must remove the unusable SDES srtpControl.
        else if(isInitiator)
        {
            AccountID accountID
                = getPeer().getProtocolProvider().getAccountID();

            // SDES
            if(accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.SDES_ENABLED,
                        false)
                    && accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true))
            {
                Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                    = getSrtpControls();
                MediaTypeSrtpControl key
                    = new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
                SrtpControl control = srtpControls.get(key);
                if(control != null)
                {
                    control.cleanup();
                    srtpControls.remove(key);
                }
            }
        }
    }

    /**
     * Returns the selected SDES crypto suite selected.
     *
     * @param isInitiator True if the local call instance is the initiator of
     * the call. False otherwise.
     * @param sDesControl The SDES based SRTP MediaStream encryption
     * control.
     * @param encryptionPacketExtension The ENCRYPTION element received from the
     * remote peer. This may contain the SDES crypto suites available for the
     * remote peer.
     *
     * @return The selected SDES crypto suite supported by both the local and
     * the remote peer. Or null, if there is no crypto suite supported by both
     * of the peers.
     */
    protected SrtpCryptoAttribute selectSdesCryptoSuite(
            boolean isInitiator,
            SDesControl sDesControl,
            EncryptionPacketExtension encryptionPacketExtension)
    {
        List<CryptoPacketExtension> cryptoPacketExtensions =
            encryptionPacketExtension.getCryptoList();
        Vector<SrtpCryptoAttribute> peerAttributes
            = new Vector<SrtpCryptoAttribute>(cryptoPacketExtensions.size());

        for(int i = 0; i < cryptoPacketExtensions.size(); ++i)
        {
            peerAttributes.add(
                    cryptoPacketExtensions.get(i).toSrtpCryptoAttribute());
        }

        if(peerAttributes == null)
        {
            return null;
        }

        if(isInitiator)
        {
            return sDesControl.initiatorSelectAttribute(peerAttributes);
        }
        else
        {
            return sDesControl.responderSelectAttribute(peerAttributes);
        }
    }

    /**
     * Returns if the remote peer supports ZRTP.
     *
     * @param encryptionPacketExtension The ENCRYPTION element received from
     * the remote peer. This may contain the ZRTP acket element for the remote
     * peer.
     *
     * @return True if the remote peer supports ZRTP. False, otherwise.
     */
    protected boolean isRemoteZrtpCapable(
            EncryptionPacketExtension encryptionPacketExtension)
    {
        List<? extends PacketExtension> packetExtensions =
            encryptionPacketExtension.getChildExtensions();

        for(int i = 0; i < packetExtensions.size(); ++i)
        {
            if(packetExtensions.get(i) instanceof ZrtpHashPacketExtension)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets ZRTP element to the ENCRYPTION element of the DESCRIPTION for a
     * given media.
     *
     * @param mediaType The type of media we are modifying the DESCRIPTION to
     * integrate the ENCRYPTION element.
     * @param description The element containing the media DESCRIPTION and its
     * encryption.
     * @param remoteDescription The element containing the media DESCRIPTION and
     * its encryption for the remote peer. Null, if the local peer is the
     * initiator of the call.
     *
     * @return True if the ZRTP element has been added to encryption. False,
     * otherwise.
     */
    protected boolean setZrtpEncryptionToDescription(
            MediaType mediaType,
            RtpDescriptionPacketExtension description,
            RtpDescriptionPacketExtension remoteDescription)
    {
        boolean isRemoteZrtpCapable = (remoteDescription == null);
        if(remoteDescription != null)
        {
            // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION
            // element for a given DESCRIPTION.
            EncryptionPacketExtension remoteEncryption
                = remoteDescription.getFirstChildOfType(
                        EncryptionPacketExtension.class);
            if(remoteEncryption != null
                    && isRemoteZrtpCapable(remoteEncryption))
            {
                isRemoteZrtpCapable = true;
            }
        }

        if(getPeer().getProtocolProvider().getAccountID()
                .getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && getPeer().getCall().isSipZrtpAttribute()
                && isRemoteZrtpCapable)
        {
            Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                = getSrtpControls();
            MediaTypeSrtpControl key
                = new MediaTypeSrtpControl(mediaType, SrtpControlType.ZRTP);
            SrtpControl control = srtpControls.get(key);

            if(control == null)
            {
                control = JabberActivator.getMediaService().createZrtpControl();
                srtpControls.put(key, control);
            }

            String helloHash[] = ((ZrtpControl)control).getHelloHashSep();

            if(helloHash != null && helloHash[1].length() > 0)
            {
                ZrtpHashPacketExtension hash = new ZrtpHashPacketExtension();
                hash.setVersion(helloHash[0]);
                hash.setValue(helloHash[1]);

                EncryptionPacketExtension encryption
                    = description.getFirstChildOfType(
                            EncryptionPacketExtension.class);
                if(encryption == null)
                {
                    encryption = new EncryptionPacketExtension();
                    description.addChildExtension(encryption);
                }
                encryption.addChildExtension(hash);
                return true;
            }
        }

        return false;
    }

    /**
     * Sets SDES element(s) to the ENCRYPTION element of the DESCRIPTION for a
     * given media.
     *
     * @param mediaType The type of media we are modifying the DESCRIPTION to
     * integrate the ENCRYPTION element.
     * @param description The element containing the media DESCRIPTION and its
     * encryption.
     * @param remoteDescription The element containing the media DESCRIPTION and
     * its encryption for the remote peer. Null, if the local peer is the
     * initiator of the call.
     *
     * @return True if the crypto element has been added to encryption. False,
     * otherwise.
     */
    protected boolean setSDesEncryptionToDescription(
            MediaType mediaType,
            RtpDescriptionPacketExtension localDescription,
            RtpDescriptionPacketExtension remoteDescription)
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();

        // check if SDES and encryption is enabled at all
        if(accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.SDES_ENABLED,
                    false)
                && accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true))
        {
            // get or create the control
            Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                = getSrtpControls();
            MediaTypeSrtpControl key
                = new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
            SrtpControl control = srtpControls.get(key);

            if (control == null)
            {
                control = JabberActivator.getMediaService().createSDesControl();
                srtpControls.put(key, control);
            }

            // set the enabled ciphers suites
            SDesControl sdcontrol = (SDesControl) control;
            String ciphers
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.SDES_CIPHER_SUITES);
            
             if (ciphers == null)
            {
                ciphers =
                    JabberActivator.getResources().getSettingsString(
                        SDesControl.SDES_CIPHER_SUITES);
            }
            sdcontrol.setEnabledCiphers(Arrays.asList(ciphers.split(",")));

            // act as initiator
            if (remoteDescription == null)
            {
                EncryptionPacketExtension localEncryption
                    = localDescription.getFirstChildOfType(
                            EncryptionPacketExtension.class);
                if(localEncryption == null)
                {
                    localEncryption = new EncryptionPacketExtension();
                    localDescription.addChildExtension(localEncryption);
                }

                for(SrtpCryptoAttribute ca:
                        sdcontrol.getInitiatorCryptoAttributes())
                {
                    CryptoPacketExtension crypto
                        = new CryptoPacketExtension(ca);
                    localEncryption.addChildExtension(crypto);
                }

                return true;
            }
            // act as responder
            else
            {
                // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION
                // element for a given DESCRIPTION.
                EncryptionPacketExtension remoteEncryption
                    = remoteDescription.getFirstChildOfType(
                            EncryptionPacketExtension.class);
                if(remoteEncryption != null)
                {
                    SrtpCryptoAttribute selectedSdes = selectSdesCryptoSuite(
                            false,
                            sdcontrol,
                            remoteEncryption);

                    if(selectedSdes != null)
                    {
                        EncryptionPacketExtension localEncryption
                            = localDescription.getFirstChildOfType(
                                    EncryptionPacketExtension.class);
                        if(localEncryption == null)
                        {
                            localEncryption = new EncryptionPacketExtension();
                            localDescription.addChildExtension(localEncryption);
                        }
                        CryptoPacketExtension crypto
                            = new CryptoPacketExtension(selectedSdes);
                        localEncryption.addChildExtension(crypto);

                        return true;
                    }
                    else
                    {
                        // none of the offered suites match, destroy the sdes
                        // control
                        sdcontrol.cleanup();
                        srtpControls.remove(key);
                        logger.warn(
                                "Received unsupported sdes crypto attribute");
                    }
                }
                else
                {
                    // peer doesn't offer any SDES attribute, destroy the sdes
                    // control
                    sdcontrol.cleanup();
                    srtpControls.remove(key);
                }
            }
        }

        return false;
    }
}
