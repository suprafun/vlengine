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

package com.vlengine.test;

import com.vlengine.app.AppContext;
import com.vlengine.app.MainGame;
import com.vlengine.app.frame.Frame;
import java.util.ArrayList;

import com.vlengine.math.Vector3f;
import com.vlengine.audio.AudioSystem;
import com.vlengine.audio.AudioTrack;
import com.vlengine.audio.RangedAudioTracker;
import com.vlengine.audio.AudioTrack.TrackType;
import com.vlengine.audio.MusicTrackQueue.RepeatType;
import com.vlengine.model.PrimitiveFactory;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.control.SpatialTransformer;

/**
 * Demonstrates the use and some of the functionality of the com.jmex.audio
 * package.
 * 
 * @author lex (Aleksey Nikiforov) reworked for vlengine
 * @author Joshua Slack
 * @version $Id: TestJmexAudio.java,v 1.2 2007/03/12 03:02:09 renanse Exp $
 */
public class Test048Audio extends MainGame {

    private AudioSystem audio;
    private ArrayList<RangedAudioTracker> trackers = new ArrayList<RangedAudioTracker>();

    /**
     * Entry point for the test,
     * 
     * @param args
     */
    public static void main(String[] args) {
        Test048Audio app = new Test048Audio();
        app.start();
    }

    private AudioTrack getMusic(String resource) {
        // Create a non-streaming, non-looping, relative sound clip.
        AudioTrack sound = app.getResourceFinder().getAudioTrack(
                resource, ParameterMap.NODIRECTBUFFER);
        sound.setType(TrackType.MUSIC);
        sound.setRelative(true);
        sound.setTargetVolume(0.4f);
        sound.setLooping(false);
        return sound;
    }

    private AudioTrack getSFX(String resource) {
        // Create a non-streaming, looping, positional sound clip.
        AudioTrack sound = app.getResourceFinder().getAudioTrack(
                resource, ParameterMap.DIRECTBUFFER);
        sound.setType(TrackType.POSITIONAL);
        sound.setRelative(false);
        sound.setLooping(true);
        return sound;
    }

    @Override
    protected void simpleInitGame(AppContext app) {
        // setup a very simple scene
        LodMesh emit1 = PrimitiveFactory.createBox("b1", new Vector3f(), 2, 2, 2);
        app.rootNode.attachChild(emit1);
        
        LodMesh emit2 = PrimitiveFactory.createBox("b2", new Vector3f(), 1, 1, 1);
        emit2.getLocalTranslation().set(10, 0, 0);
        app.rootNode.attachChild(emit2);
        
        SpatialTransformer st = new SpatialTransformer("Move", 1);
        st.setObject(emit2, 0, -1);
        st.setPosition(0, 0.0f, new Vector3f(8,-8,0));
        st.setPosition(0, 0.5f, new Vector3f(6,-4,-6));
        st.setPosition(0, 1.0f, new Vector3f(0,0,-8));
        st.setPosition(0, 1.5f, new Vector3f(-6,4,-6));
        st.setPosition(0, 2.0f, new Vector3f(-8,8,0));
        st.setPosition(0, 2.5f, new Vector3f(-6,4,6));
        st.setPosition(0, 3.0f, new Vector3f(0,0,8));
        st.setPosition(0, 3.5f, new Vector3f(6,-4,6));
        st.setPosition(0, 4.0f, new Vector3f(8,-8,0));
        st.interpolateMissing();
        st.setRepeatType(SpatialTransformer.RT_WRAP);
        emit2.addController(st);
        
        // SOUND STUFF BELOW
        
        // grab a handle to our audio system.
        audio = AudioSystem.getSystem();
        
        // setup our ear tracker to track the camera's position and orientation.
        audio.getEar().trackOrientation(app.cam);
        audio.getEar().trackPosition(app.cam);
        
        app.getResourceFinder().addClassPathFolder("/testData",
                "com/vlengine/test/data", "");
        
        // setup a music score for our demo
        /*
        AudioTrack music1 = getMusic("ocean.ogg");
        audio.getMusicQueue().setRepeatType(RepeatType.ALL);
        audio.getMusicQueue().setCrossfadeinTime(2.5f);
        audio.getMusicQueue().setCrossfadeoutTime(2.5f);
        audio.getMusicQueue().addTrack(music1);
        audio.getMusicQueue().play();
         */

        // setup positional sounds in our scene
        AudioTrack sfx1 = getSFX("crowd.ogg");
        RangedAudioTracker track1 = new RangedAudioTracker(sfx1, 25, 30);
        track1.setToTrack(emit1);
        track1.setTrackIn3D(true);
        track1.setMaxVolume(0.7f);  // set volume on the tracker as it will control fade in, etc.
        trackers.add(track1);
        
        AudioTrack sfx2 = getSFX("steps.ogg");
        RangedAudioTracker track2 = new RangedAudioTracker(sfx2, 25, 30);
        track2.setToTrack(emit2);
        track2.setTrackIn3D(true);
        track2.setMaxVolume(1.0f);
        trackers.add(track2);
    }

    @Override
    protected void simpleUpdate(Frame f) {
        // update our audio system here:
        audio.update();
        
        for (int x = trackers.size(); --x >= 0; ) {
            RangedAudioTracker t = trackers.get(x);
            t.checkTrackAudible(app.cam.getLocation());
        }
    }
}
