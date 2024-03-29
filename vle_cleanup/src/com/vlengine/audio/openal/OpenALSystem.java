/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * * Neither the name of 'VL Engine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vlengine.audio.openal;

import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import com.vlengine.audio.AudioSystem;

/**
 * @see AudioSystem
 * @author Joshua Slack
 * @version $Id: OpenALSystem.java,v 1.9 2007/10/05 22:44:50 nca Exp $
 */
public class OpenALSystem extends AudioSystem {
    private static final Logger logger = Logger.getLogger(OpenALSystem.class.getName());

    private OpenALEar ear;
    private LinkedList<OpenALSource> sourcePool = new LinkedList<OpenALSource>();
    private static int MAX_SOURCES = 64;
    private long lastTime = System.currentTimeMillis();

    public OpenALSystem() {
        ear = new OpenALEar();
        try {
            AL.create();
            AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE);
            setupSourcePool();
        } catch (Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "OpenALSystem()", "Exception",
                    e);
        }
    }

    private void setupSourcePool() {
        IntBuffer alSources = BufferUtils.createIntBuffer(1);
        try {
            for (int x = 0; x < MAX_SOURCES; x++) {
                alSources.clear();
                AL10.alGenSources(alSources);
                OpenALSource source = new OpenALSource(alSources.get(0));
                sourcePool.add(source);
            }
        } catch (OpenALException e) {
            MAX_SOURCES = sourcePool.size();
        }
        logger.info("max source channels: " + MAX_SOURCES);
    }

    @Override
    public OpenALEar getEar() {
        return ear;
    }

    @Override
    public void update() {
        synchronized(this) {
            if (!AL.isCreated()) return;

            long thisTime = System.currentTimeMillis();
            float dt = (thisTime - lastTime) / 1000f; 
            lastTime  = thisTime;
        
            try {
                for (int x = 0; x < MAX_SOURCES; x++) {
                    OpenALSource src = sourcePool.get(x);
                    src.setState(AL10.alGetSourcei(src.getId(), AL10.AL_SOURCE_STATE));
                    if (src.getState() == AL10.AL_PLAYING) src.getTrack().update(dt);
                }
                ear.update(dt);
            } catch (Exception e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "update()", "Exception", e);
            }
            try {
                getMusicQueue().update(dt);
            } catch (Exception e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "update()", "Exception", e);
                try {
                    getMusicQueue().clearTracks();
                } catch (Exception ex) {
                    logger.logp(Level.SEVERE, this.getClass().toString(), "update()", "Exception", ex);
                }
            }
            try {
                getEnvironmentalPool().update(dt);
            } catch (Exception e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "update()", "Exception", e);
                try {
                    getEnvironmentalPool().clearTracks();
                } catch (Exception ex) {
                    logger.logp(Level.SEVERE, this.getClass().toString(), "update()", "Exception", ex);
                }
            }
        }
    }

    public OpenALSource getNextFreeSource() {
        synchronized(this) {
            for (int x = 0; x < MAX_SOURCES; x++) {
                OpenALSource src = sourcePool.get(x);
                if (isAvailableState(src.getState())) {
                    sourcePool.remove(x);
                    sourcePool.add(src);
                    return src;
                }
            }
        }
        return null;
    }
    
    private boolean isAvailableState(int state) {
        if (state != AL10.AL_PLAYING && state != AL10.AL_PAUSED && state != -1)
            return true;
        return false;
    }

    @Override
    public void setMasterGain(float gain) {
        AL10.alListenerf(AL10.AL_GAIN, gain);
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        synchronized(this) {
            if (getMusicQueue() != null) {
                getMusicQueue().clearTracks();
            }
            if (getEnvironmentalPool() != null) {
                getEnvironmentalPool().clearTracks();
            }
            AL.destroy();
        }
        sourcePool.clear();
    }

    @Override
    public void setDopplerFactor(float amount) {
        AL10.alDopplerFactor(amount);

    }

    @Override
    public void setSpeedOfSound(float unitsPerSecond) {
        AL10.alDopplerVelocity(unitsPerSecond);
    }
}