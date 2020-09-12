/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 *
 */
package net.judah.jack;


import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Getter;
import lombok.extern.log4j.Log4j;

/**
 * Implementation of an Audio Client using Jack (via JNAJack)
 *
 * @author Neil C Smith
 * @author Jeff Masty */
@Log4j
public abstract class AudioClient extends BasicClient {

	/** pass-through client */
	public static class Dummy extends AudioClient { 
		public Dummy(ClientConfig config) throws JackException {
			super(config); start(); }
		@Override protected void makeConnections() throws JackException { }
    }
	
    @Getter protected ClientConfig config;

    /** input audio ports */
    @Getter protected JackPort[] inputPorts;
    /** output audio ports */
    @Getter protected JackPort[] outputPorts;
    
	protected List<FloatBuffer> inputs;
    protected List<FloatBuffer> outputs;

	/** roll your own config, ports and name */
	protected AudioClient(String name) throws JackException { super(name); }

    public AudioClient(ClientConfig config) throws JackException {
    	super(config.getName());
    	this.config = config;
    	inputPorts = new JackPort[config.getAudioInputNames().length];
    	outputPorts = new JackPort[config.getAudioOutputNames().length];
    }

	@Override
	protected void initialize() throws JackException {
        if (config == null) return; // roll your own ports

        int count = config.getAudioInputNames().length;
		inputs = Arrays.asList(new FloatBuffer[config.getAudioInputNames().length]);
		
        for (int i = 0; i < count; i++) {
        	log.debug("registering port " + config.getAudioInputNames()[i]);  
        	inputPorts[i] = jackclient.registerPort(config.getAudioInputNames()[i], JackPortType.AUDIO, JackPortFlags.JackPortIsInput);
        }

        count = config.getAudioOutputNames().length;
	    outputs = Arrays.asList(new FloatBuffer[config.getAudioOutputNames().length]);
        for (int i = 0; i < count; i++) {
        	outputPorts[i] = jackclient.registerPort(config.getAudioOutputNames()[i], JackPortType.AUDIO, JackPortFlags.JackPortIsOutput);
        }
	}

    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
	@Override
	public boolean process(JackClient client, int nframes) {

        if (state.get() != Status.ACTIVE) {
            return false;
        }
        for (int i = 0; i < inputPorts.length; i++) {
            inputs.set(i, inputPorts[i].getFloatBuffer());
        }
        for (int i = 0; i < outputPorts.length; i++) {
            outputs.set(i, outputPorts[i].getFloatBuffer());
        }
        AudioTools.processEcho(inputs, outputs);
        return true;
	}

}
