/*
 * Copyright (c) 2008 VL Engine
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

package com.vlengine.scene.control;

import com.vlengine.app.AppContext;
import com.vlengine.audio.AudioSystem;
import com.vlengine.audio.AudioTrack;
import com.vlengine.audio.AudioTrack.TrackType;
import com.vlengine.audio.RangedAudioTracker;
import com.vlengine.math.Vector3f;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.Spatial;
import com.vlengine.util.FastList;
import java.util.Arrays;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ControllerSoundPack {

    public static enum SoundType {
        None(0),
        Walk(1),
        Run(2),
        Land(3);
        
        protected int soundIndex;
        SoundType(int soundIdx) {
            soundIndex = soundIdx;
        }
    }

    
    // the sounds the player can produce
    protected FastList<AudioTrack> tracks = new FastList<AudioTrack>();
    AudioSystem audio;

    protected float playRange = 250;
    protected float playRangeSQ = playRange*playRange;
    //protected float fullVolumeDistance = 200;
    protected float maxVolume = 1f;
    
    // expand this when more sounds
    protected boolean[] shouldPlay = new boolean[4];
    
    // the tracked spatial, the ear position
    protected Spatial earPosition;
    
    public ControllerSoundPack() {
    }
    
    private AudioTrack getSFX(AppContext app, String resource) {
        // Create a non-streaming, looping, positional sound clip.
        AudioTrack sound = app.getResourceFinder().getAudioTrack(
                resource, ParameterMap.DIRECTBUFFER);
        sound.setType(TrackType.POSITIONAL);
        sound.setRelative(false);
        sound.setLooping(true);
        return sound;
    }
    
    public void setupPC1Sounds(AppContext app) {
        
        maxVolume = app.conf.soundSFXVolume;
        // the tracked position is the camera node
        earPosition = app.camn;
        
        audio = app.audio;

        AudioTrack sfx;
        
        // we have a walk sound
        sfx = getSFX(app, "Walking-NormalHardSurface.ogg");
        sfx.setMaxAudibleDistance(playRange);
        sfx.setReferenceDistance(playRange / 10f);
        sfx.setVolume(maxVolume);
        //sfx.setRolloff(.5f);
        tracks.set(SoundType.Walk.soundIndex, sfx);
        
        // we have a run SFX
        sfx = getSFX(app, "Running-NormalHardSurface.ogg");
        sfx.setMaxAudibleDistance(playRange);
        sfx.setReferenceDistance(playRange / 10f);
        sfx.setVolume(maxVolume);
        //sfx.setRolloff(.5f);
        tracks.set(SoundType.Run.soundIndex, sfx);

        // landing on hard surface
        sfx = getSFX(app, "landing-normalhardsurface.ogg");
        sfx.setMaxAudibleDistance(playRange);
        sfx.setReferenceDistance(playRange / 10f);
        sfx.setVolume(maxVolume);
        //sfx.setRolloff(.5f);
        tracks.set(SoundType.Land.soundIndex, sfx);
        
    }
    
    public void update(Spatial toTrack, boolean walk, boolean run, boolean landed) {
        Arrays.fill(shouldPlay, false);
        
        if(landed) {
            shouldPlay[SoundType.Land.soundIndex] = true;
        } else if(walk) {
            shouldPlay[SoundType.Walk.soundIndex] = true;
        } else if(run) {
            shouldPlay[SoundType.Run.soundIndex] = true;
        }
        
        Vector3f position = toTrack.getWorldTranslation();
        Vector3f from = earPosition.getWorldTranslation();
        
        float distSQ = position.distanceSquared(from);
        
        boolean indistance = (distSQ <= playRangeSQ);

        // go over the audio tracks
        // and determine which should be started and which should be stopped
        for(SoundType s: SoundType.values()) {
            AudioTrack a = tracks.get(s.soundIndex);
            if(a==null)
                continue;
            
            if (shouldPlay[s.soundIndex] && indistance) {
                // TODO: beacuse of this, we need to clone audiotracks?
                a.setWorldPosition(position);
                if( !a.isPlaying()) {
                    a.setVolume(maxVolume);
                    a.setTargetVolume(maxVolume);
                    a.play();
                }
                
            } else {
                if(a.isPlaying() ) {
                    a.stop();
                }
            }
        }
    }
}
