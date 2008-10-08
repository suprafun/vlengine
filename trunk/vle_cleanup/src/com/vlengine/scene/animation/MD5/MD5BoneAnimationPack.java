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

package com.vlengine.scene.animation.MD5;

import com.vlengine.scene.animation.*;
import com.vlengine.resource.model.*;
import com.vlengine.util.FastList;
import java.util.HashMap;

/**
 * This class represents all animations of a model (character, gameobject)
 * 
 * @author vear (Arpad Vekas)
 */
public class MD5BoneAnimationPack {
    
    // the bones of animation pack
    protected FastList<Bone> bones = new FastList<Bone>();

    // the map of animations by their name
    protected HashMap<String,MD5BoneAnimation> animations = new HashMap<String,MD5BoneAnimation>();
    // the exclusive groups of animations, enabling an animation from a group
    // disables other animations from that group
    protected FastList<FastList<MD5BoneAnimation>> groups = new FastList<FastList<MD5BoneAnimation>>();
    
    public void addAnimation(String name, MD5BoneAnimation anim) {
        animations.put(name, anim);
        // put it into proper group
        groups.ensureCapacity(anim.group+1);
        FastList<MD5BoneAnimation> group = groups.get(anim.group);
        if(group==null) {
            // create group
            group = new FastList<MD5BoneAnimation>();
            groups.set(anim.group, group);
        }
        // add the animation to the group
        group.add(anim);
    }
    
    public MD5BoneAnimation getAnimation(String name) {
        return animations.get(name);
    }
    
    public FastList<Bone> getBones() {
        return bones;
    }
    
}
