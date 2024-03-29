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

import java.nio.FloatBuffer;

import org.lwjgl.openal.AL10;

import com.vlengine.math.Vector3f;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.audio.Ear;

/**
 * @see Ear
 * @author Joshua Slack
 * 
 */
public class OpenALEar extends Ear {

    private FloatBuffer orientBuf = BufferUtils.createFloatBuffer(6);

    @Override
    public void update(float dt) {
        super.update(dt);

        Vector3f pos = getPosition();
        Vector3f vel = getCurrVelocity();
        Vector3f up = getUpVector();
        Vector3f dir = getFacingVector();

        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);

        orientBuf.rewind();
        orientBuf.put(dir.x);
        orientBuf.put(dir.y);
        orientBuf.put(dir.z);
        orientBuf.put(up.x);
        orientBuf.put(up.y);
        orientBuf.put(up.z);
        orientBuf.rewind();

        AL10.alListener(AL10.AL_ORIENTATION, orientBuf);
    }

}
