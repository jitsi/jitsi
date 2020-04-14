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
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.net.*;

import com.ircclouds.irc.api.*;
import com.ircclouds.irc.api.ctcp.*;
import com.ircclouds.irc.api.domain.*;
import com.ircclouds.irc.api.filters.*;
import com.ircclouds.irc.api.listeners.*;
import com.ircclouds.irc.api.state.*;

/**
 * Synchronization wrapper for IRCApi.
 *
 * All calls are synchronized. In case of multiple operations one can manually
 * block-synchronize on the instance.
 *
 * @author Danny van Heumen
 */
public class SynchronizedIRCApi
    implements IRCApi
{
    private final IRCApi irc;

    /**
     * Constructor for synchronization wrapper.
     *
     * @param irc IRCApi instance
     */
    public SynchronizedIRCApi(final IRCApi irc)
    {
        if (irc == null)
        {
            throw new IllegalArgumentException("irc instance cannot be null");
        }
        this.irc = irc;
    }

    @Override
    public synchronized void connect(final IServerParameters aServerParameters,
        final Callback<IIRCState> aCallback)
    {
        this.irc.connect(aServerParameters, aCallback);
    }

    @Override
    public synchronized void connect(final IServerParameters aServerParameters,
        final Callback<IIRCState> aCallback,
        final CapabilityNegotiator negotiator)
    {
        this.irc.connect(aServerParameters, aCallback, negotiator);
    }

    @Override
    public synchronized void disconnect()
    {
        this.irc.disconnect();
    }

    @Override
    public synchronized void disconnect(final String aQuitMessage)
    {
        this.irc.disconnect(aQuitMessage);
    }

    @Override
    public synchronized void joinChannel(final String aChannelName)
    {
        this.irc.joinChannel(aChannelName);
    }

    @Override
    public synchronized void joinChannel(final String aChannelName,
        final Callback<IRCChannel> aCallback)
    {
        this.irc.joinChannel(aChannelName, aCallback);
    }

    @Override
    public synchronized void joinChannel(final String aChannelName,
        final String aKey)
    {
        this.irc.joinChannel(aChannelName, aKey);
    }

    @Override
    public synchronized void joinChannel(final String aChannelName,
        final String aKey, final Callback<IRCChannel> aCallback)
    {
        this.irc.joinChannel(aChannelName, aKey, aCallback);
    }

    @Override
    public synchronized void leaveChannel(final String aChannelName)
    {
        this.irc.leaveChannel(aChannelName);
    }

    @Override
    public synchronized void leaveChannel(final String aChannelName,
        final Callback<String> aCallback)
    {
        this.irc.leaveChannel(aChannelName, aCallback);
    }

    @Override
    public synchronized void leaveChannel(final String aChannelName,
        final String aPartMessage)
    {
        this.irc.leaveChannel(aChannelName, aPartMessage);
    }

    @Override
    public synchronized void leaveChannel(final String aChannelName,
        final String aPartMessage, final Callback<String> aCallback)
    {
        this.irc.leaveChannel(aChannelName, aPartMessage, aCallback);
    }

    @Override
    public synchronized void changeNick(final String aNewNick)
    {
        this.irc.changeNick(aNewNick);
    }

    @Override
    public synchronized void changeNick(final String aNewNick,
        final Callback<String> aCallback)
    {
        this.irc.changeNick(aNewNick, aCallback);
    }

    @Override
    public synchronized void message(final String aTarget,
        final String aMessage)
    {
        this.irc.message(aTarget, aMessage);
    }

    @Override
    public synchronized void message(final String aTarget,
        final String aMessage, final Callback<String> aCallback)
    {
        this.irc.message(aTarget, aMessage, aCallback);
    }

    @Override
    public synchronized void act(final String aTarget, final String aMessage)
    {
        this.irc.act(aTarget, aMessage);
    }

    @Override
    public synchronized void act(final String aTarget, final String aMessage,
        final Callback<String> aCallback)
    {
        this.irc.act(aTarget, aMessage, aCallback);
    }

    @Override
    public synchronized void notice(final String aTarget, final String aMessage)
    {
        this.irc.notice(aTarget, aMessage);
    }

    @Override
    public synchronized void notice(final String aTarget,
        final String aMessage, final Callback<String> aCallback)
    {
        this.irc.notice(aTarget, aMessage, aCallback);
    }

    @Override
    public synchronized void kick(final String aChannel, final String aNick)
    {
        this.irc.kick(aChannel, aNick);
    }

    @Override
    public synchronized void kick(final String aChannel, final String aNick,
        final String aKickMessage)
    {
        this.irc.kick(aChannel, aNick, aKickMessage);
    }

    @Override
    public synchronized void kick(final String aChannel, final String aNick,
        final Callback<String> aCallback)
    {
        this.irc.kick(aChannel, aNick, aCallback);
    }

    @Override
    public synchronized void kick(final String aChannel, final String aNick,
        final String aKickMessage, final Callback<String> aCallback)
    {
        this.irc.kick(aChannel, aNick, aKickMessage, aCallback);
    }

    @Override
    public synchronized void changeTopic(final String aChannel,
        final String aTopic)
    {
        this.irc.changeTopic(aChannel, aTopic);
    }

    @Override
    public synchronized void changeMode(final String aModeString)
    {
        this.irc.changeMode(aModeString);
    }

    @Override
    public synchronized void rawMessage(final String aMessage)
    {
        this.irc.rawMessage(aMessage);
    }

    @Override
    public synchronized void dccSend(final String aNick, final File aFile,
        final DCCSendCallback aCallback)
    {
        this.irc.dccSend(aNick, aFile, aCallback);
    }

    @Override
    public synchronized void dccSend(final String aNick, final File aFile,
        final Integer aTimeout, final DCCSendCallback aCallback)
    {
        this.irc.dccSend(aNick, aFile, aTimeout, aCallback);
    }

    @Override
    public synchronized void dccSend(final String aNick,
        final Integer aListeningPort, final File aFile,
        final DCCSendCallback aCallback)
    {
        this.irc.dccSend(aNick, aListeningPort, aFile, aCallback);
    }

    @Override
    public synchronized void dccSend(final String aNick, final File aFile,
        final Integer aListeningPort, final Integer aTimeout,
        final DCCSendCallback aCallback)
    {
        this.irc.dccSend(aNick, aFile, aListeningPort, aTimeout, aCallback);
    }

    @Override
    public synchronized void dccAccept(final String aNick, final File aFile,
        final Integer aPort, final Integer aResumePosition,
        final DCCSendCallback aCallback)
    {
        this.irc.dccAccept(aNick, aFile, aPort, aResumePosition, aCallback);
    }

    @Override
    public synchronized void dccAccept(final String aNick, final File aFile,
        final Integer aPort, final Integer aResumePosition,
        final Integer aTimeout, final DCCSendCallback aCallback)
    {
        this.irc.dccAccept(aNick, aFile, aPort, aResumePosition, aTimeout,
            aCallback);
    }

    @Override
    public synchronized void dccReceive(final File aFile, final Integer aSize,
        final SocketAddress aAddress, final DCCReceiveCallback aCallback)
    {
        this.irc.dccReceive(aFile, aSize, aAddress, aCallback);
    }

    @Override
    public synchronized void dccReceive(final File aFile, final Integer aSize,
        final SocketAddress aAddress, final DCCReceiveCallback aCallback,
        final Proxy aProxy)
    {
        this.irc.dccReceive(aFile, aSize, aAddress, aCallback, aProxy);
    }

    @Override
    public synchronized void dccResume(final File aFile,
        final Integer aResumePosition, final Integer aSize,
        final SocketAddress aAddress, final DCCReceiveCallback aCallback)
    {
        this.irc.dccResume(aFile, aResumePosition, aSize, aAddress, aCallback);
    }

    @Override
    public synchronized void dccResume(final File aFile,
        final Integer aResumePosition, final Integer aSize,
        final SocketAddress aAddress, final DCCReceiveCallback aCallback,
        final Proxy aProxy)
    {
        this.irc.dccResume(aFile, aResumePosition, aSize, aAddress, aCallback,
            aProxy);
    }

    @Override
    public synchronized DCCManager getDCCManager()
    {
        return this.irc.getDCCManager();
    }

    @Override
    public synchronized void addListener(final IMessageListener aListener)
    {
        this.irc.addListener(aListener);
    }

    @Override
    public synchronized void deleteListener(final IMessageListener aListener)
    {
        this.irc.deleteListener(aListener);
    }

    @Override
    public synchronized void setMessageFilter(final IMessageFilter aFilter)
    {
        this.irc.setMessageFilter(aFilter);
    }
}
