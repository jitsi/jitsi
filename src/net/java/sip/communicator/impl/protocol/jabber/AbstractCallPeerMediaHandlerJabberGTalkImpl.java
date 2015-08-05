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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;

import ch.imvs.sdes4j.srtp.*;

/**
 * An implementation of the <tt>CallPeerMediaHandler</tt> abstract class for the
 * common part of Jabber and Gtalk protocols.
 *
 * @author Vincent Lucas
 * @author Lyubomir Marinov
 */
public abstract class AbstractCallPeerMediaHandlerJabberGTalkImpl
        <T extends AbstractCallPeerJabberGTalkImpl<?,?,?>>
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
    protected void addZrtpAdvertisedEncryptions(
            boolean isInitiator,
            RtpDescriptionPacketExtension description,
            MediaType mediaType)
    {
        CallPeer peer = getPeer();
        Call call = peer.getCall();

        /*
         * ZRTP is not supported in telephony conferences utilizing the
         * server-side technology Jitsi Videobridge yet.
         */
        if (call.getConference().isJitsiVideobridge())
            return;

        // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION element for
        // a given DESCRIPTION.
        EncryptionPacketExtension encryptionPacketExtension
            = description.getFirstChildOfType(
                    EncryptionPacketExtension.class);

        if(encryptionPacketExtension != null)
        {
            AccountID accountID = peer.getProtocolProvider().getAccountID();

            if (accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true)
                    && accountID.isEncryptionProtocolEnabled(
                            SrtpControlType.ZRTP)
                    && call.isSipZrtpAttribute())
            {
                // ZRTP
                ZrtpHashPacketExtension zrtpHashPacketExtension
                    = encryptionPacketExtension.getFirstChildOfType(
                            ZrtpHashPacketExtension.class);

                if ((zrtpHashPacketExtension != null)
                        && (zrtpHashPacketExtension.getValue() != null))
                {
                    addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
                }
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
    protected void addSDesAdvertisedEncryptions(
            boolean isInitiator,
            RtpDescriptionPacketExtension description,
            MediaType mediaType)
    {
        CallPeer peer = getPeer();

        /*
         * SDES is not supported in telephony conferences utilizing the
         * server-side technology Jitsi Videobridge yet.
         */
        if (peer.getCall().getConference().isJitsiVideobridge())
            return;

        // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION element for
        // a given DESCRIPTION.
        EncryptionPacketExtension encryptionPacketExtension
            = description.getFirstChildOfType(
                    EncryptionPacketExtension.class);

        if(encryptionPacketExtension != null)
        {
            AccountID accountID = peer.getProtocolProvider().getAccountID();

            // SDES
            if(accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true)
                    && accountID.isEncryptionProtocolEnabled(
                            SrtpControlType.SDES))
            {
                SrtpControls srtpControls = getSrtpControls();
                SDesControl sdesControl
                    = (SDesControl)
                        srtpControls.getOrCreate(
                                mediaType,
                                SrtpControlType.SDES);
                SrtpCryptoAttribute selectedSdes
                    = selectSdesCryptoSuite(
                            isInitiator,
                            sdesControl,
                            encryptionPacketExtension);

                if(selectedSdes != null)
                {
                    //found an SDES answer, remove all other controls
                    removeAndCleanupOtherSrtpControls(
                            mediaType,
                            SrtpControlType.SDES);
                    addAdvertisedEncryptionMethod(SrtpControlType.SDES);
                }
                else
                {
                    sdesControl.cleanup(null);
                    srtpControls.remove(mediaType, SrtpControlType.SDES);
                }
            }
        }
        // If we were initiating the encryption, and the remote peer does not
        // manage it, then we must remove the unusable SDES srtpControl.
        else if(isInitiator)
        {
            // SDES
            SrtpControl sdesControl
                = getSrtpControls().remove(mediaType, SrtpControlType.SDES);

            if (sdesControl != null)
                sdesControl.cleanup(null);
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
        List<CryptoPacketExtension> cryptoPacketExtensions
            = encryptionPacketExtension.getCryptoList();
        List<SrtpCryptoAttribute> peerAttributes
            = new ArrayList<SrtpCryptoAttribute>(cryptoPacketExtensions.size());

        for (CryptoPacketExtension cpe : cryptoPacketExtensions)
            peerAttributes.add(cpe.toSrtpCryptoAttribute());

        return
            isInitiator
                ? sDesControl.initiatorSelectAttribute(peerAttributes)
                : sDesControl.responderSelectAttribute(peerAttributes);
    }

    /**
     * Returns if the remote peer supports ZRTP.
     *
     * @param encryptionPacketExtension The ENCRYPTION element received from
     * the remote peer. This may contain the ZRTP packet element for the remote
     * peer.
     *
     * @return True if the remote peer supports ZRTP. False, otherwise.
     */
    protected boolean isRemoteZrtpCapable(
            EncryptionPacketExtension encryptionPacketExtension)
    {
        return
            (encryptionPacketExtension.getFirstChildOfType(
                    ZrtpHashPacketExtension.class)
                != null);
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
    protected boolean setZrtpEncryptionOnDescription(
            MediaType mediaType,
            RtpDescriptionPacketExtension description,
            RtpDescriptionPacketExtension remoteDescription)
    {
        CallPeer peer = getPeer();
        Call call = peer.getCall();

        /*
         * ZRTP is not supported in telephony conferences utilizing the
         * server-side technology Jitsi Videobridge yet.
         */
        if (call.getConference().isJitsiVideobridge())
            return false;

        boolean isRemoteZrtpCapable;

        if (remoteDescription == null)
            isRemoteZrtpCapable = true;
        else
        {
            // Conforming to XEP-0167 schema there is 0 or 1 ENCRYPTION element
            // for a given DESCRIPTION.
            EncryptionPacketExtension remoteEncryption
                = remoteDescription.getFirstChildOfType(
                        EncryptionPacketExtension.class);

            isRemoteZrtpCapable
                = (remoteEncryption != null)
                    && isRemoteZrtpCapable(remoteEncryption);
        }

        boolean zrtpHashSet = false; // Will become true if at least one is set.

        if (isRemoteZrtpCapable)
        {
            AccountID accountID = peer.getProtocolProvider().getAccountID();

            if(accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true)
                    && accountID.isEncryptionProtocolEnabled(
                            SrtpControlType.ZRTP)
                    && call.isSipZrtpAttribute())
            {
                ZrtpControl zrtpControl
                    = (ZrtpControl)
                        getSrtpControls().getOrCreate(
                                mediaType,
                                SrtpControlType.ZRTP);
                int numberSupportedVersions
                    = zrtpControl.getNumberSupportedVersions();

                for (int i = 0; i < numberSupportedVersions; i++)
                {
                    String helloHash[] = zrtpControl.getHelloHashSep(i);

                    if ((helloHash != null) && (helloHash[1].length() > 0))
                    {
                        ZrtpHashPacketExtension hash
                            = new ZrtpHashPacketExtension();

                        hash.setVersion(helloHash[0]);
                        hash.setValue(helloHash[1]);

                        EncryptionPacketExtension encryption
                            = description.getFirstChildOfType(
                                    EncryptionPacketExtension.class);

                        if (encryption == null)
                        {
                            encryption = new EncryptionPacketExtension();
                            description.addChildExtension(encryption);
                        }
                        encryption.addChildExtension(hash);
                        zrtpHashSet = true;
                    }
                }
            }
        }

        return zrtpHashSet;
    }

    /**
     * Sets SDES element(s) to the ENCRYPTION element of the DESCRIPTION for a
     * given media.
     *
     * @param mediaType The type of media we are modifying the DESCRIPTION to
     * integrate the ENCRYPTION element.
     * @param localDescription The element containing the media DESCRIPTION and
     * its encryption.
     * @param remoteDescription The element containing the media DESCRIPTION and
     * its encryption for the remote peer. Null, if the local peer is the
     * initiator of the call.
     *
     * @return True if the crypto element has been added to encryption. False,
     * otherwise.
     */
    protected boolean setSDesEncryptionOnDescription(
            MediaType mediaType,
            RtpDescriptionPacketExtension localDescription,
            RtpDescriptionPacketExtension remoteDescription)
    {
        CallPeer peer = getPeer();

        /*
         * SDES is not supported in telephony conferences utilizing the
         * server-side technology Jitsi Videobridge yet.
         */
        if (peer.getCall().getConference().isJitsiVideobridge())
            return false;

        AccountID accountID = peer.getProtocolProvider().getAccountID();

        // check if SDES and encryption is enabled at all
        if (accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true)
                && accountID.isEncryptionProtocolEnabled(
                        SrtpControlType.SDES))
        {
            // get or create the control
            SrtpControls srtpControls = getSrtpControls();
            SDesControl sdesControl
                = (SDesControl)
                    srtpControls.getOrCreate(mediaType, SrtpControlType.SDES);
            // set the enabled ciphers suites
            String ciphers
                = accountID.getAccountPropertyString(
                        ProtocolProviderFactory.SDES_CIPHER_SUITES);

            if (ciphers == null)
            {
                ciphers =
                    JabberActivator.getResources().getSettingsString(
                        SDesControl.SDES_CIPHER_SUITES);
            }
            sdesControl.setEnabledCiphers(Arrays.asList(ciphers.split(",")));

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
                        sdesControl.getInitiatorCryptoAttributes())
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
                            sdesControl,
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
                        sdesControl.cleanup(null);
                        srtpControls.remove(mediaType, SrtpControlType.SDES);
                        logger.warn(
                                "Received unsupported sdes crypto attribute");
                    }
                }
                else
                {
                    // peer doesn't offer any SDES attribute, destroy the sdes
                    // control
                    sdesControl.cleanup(null);
                    srtpControls.remove(mediaType, SrtpControlType.SDES);
                }
            }
        }

        return false;
    }

    /**
     * Selects the preferred encryption protocol (only used by the callee).
     *
     * @param mediaType The type of media (AUDIO or VIDEO).
     * @param localDescription The element containing the media DESCRIPTION and
     * its encryption.
     * @param remoteDescription The element containing the media DESCRIPTION and
     * its encryption for the remote peer; <tt>null</tt> if the local peer is
     * the initiator of the call.
     */
    protected void setAndAddPreferredEncryptionProtocol(
            MediaType mediaType,
            RtpDescriptionPacketExtension localDescription,
            RtpDescriptionPacketExtension remoteDescription)
    {
        // Sets ZRTP or SDES, depending on the preferences for this account.
        List<SrtpControlType> preferredEncryptionProtocols
            = getPeer()
                .getProtocolProvider()
                    .getAccountID()
                        .getSortedEnabledEncryptionProtocolList();

        for(SrtpControlType srtpControlType : preferredEncryptionProtocols)
        {
            if (setAndAddPreferredEncryptionProtocol(
                    srtpControlType,
                    mediaType,
                    localDescription,
                    remoteDescription))
            {
                // Stop once an encryption advertisement has been chosen.
                return;
            }
        }
    }

    /**
     * Selects a specific encryption protocol if it is the preferred (only used
     * by the callee).
     *
     * @param mediaType The type of media (AUDIO or VIDEO).
     * @param localDescription The element containing the media DESCRIPTION and
     * its encryption.
     * @param remoteDescription The element containing the media DESCRIPTION and
     * its encryption for the remote peer; <tt>null</tt> if the local peer is
     * the initiator of the call.
     * @return <tt>true</tt> if the specified encryption protocol has been
     * selected; <tt>false</tt>, otherwise
     */
    protected boolean setAndAddPreferredEncryptionProtocol(
            SrtpControlType srtpControlType,
            MediaType mediaType,
            RtpDescriptionPacketExtension localDescription,
            RtpDescriptionPacketExtension remoteDescription)
    {
        /*
         * Neither SDES nor ZRTP is supported in telephony conferences utilizing
         * the server-side technology Jitsi Videobridge yet.
         */
        if (getPeer().isJitsiVideobridge())
            return false;

        // SDES
        if(srtpControlType == SrtpControlType.SDES)
        {
            addSDesAdvertisedEncryptions(
                    false,
                    remoteDescription,
                    mediaType);
            if(setSDesEncryptionOnDescription(
                    mediaType,
                    localDescription,
                    remoteDescription))
            {
                // Stop once an encryption advertisement has been chosen.
                return true;
            }
        }
        // ZRTP
        else if(srtpControlType == SrtpControlType.ZRTP)
        {
            if(setZrtpEncryptionOnDescription(
                    mediaType,
                    localDescription,
                    remoteDescription))
            {
                addZrtpAdvertisedEncryptions(
                        false,
                        remoteDescription,
                        mediaType);
                // Stop once an encryption advertisement has been chosen.
                return true;
            }
        }
        return false;
    }
}
