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

package com.vlengine.scene.animation;

import com.vlengine.scene.animation.MD5.MD5BoneAnimation;
import com.vlengine.util.FastList;

/**
 * This class represents an animation action, these actions are created
 * and released as animations are started and finished. The proper way is to
 * request instances, and release instances. The general rule on using actions
 * is that every user of the action should make a copy of the action, and
 * release that copy when it dont need it any more.
 * 
 * @author vear (Arpad Vekas)
 */
public class Action {
    
    /*
    protected static FastList<Action> free = new FastList<Action>();
    
    
    public static Action getNew() {
        if(free.size()>0) {
            int idx = free.size() -1;
            Action a = free.get(idx);
            free.remove(idx);
            return a;
        } else
            return new Action();
    }
    
    public void release() {
        id=-1;
        time=0;
        loop = LoopMode.StopAtEnd;
        exclusiveGroup = 0;
        free.add(this);
    }
     */
    
    // protected constructor
    public Action() {}
    
    // the animation name to run
    public int id = -1;

    // how long to run the animation, if 0, then the animation is run until
    // it finishes
    public float time = 0;
           
    // should this action be resheduled when it finishes
    public enum LoopMode {
        // stop animation at end
        StopAtEnd,
        // show last frame indefinitely
        ClampAtEnd,
        // restart animation after last frame
        RestartAtEnd,
        // continue with next action after this one finished
        ContinueNext,
    }

    public LoopMode loop = LoopMode.StopAtEnd;
    
    // all animation is stopped in the target group
    // value of 0 means no group
    // 1    is move
    public int exclusiveGroup = 0;
    
    // the animation range start
    public int rangeStart = 0;
    
    // the animation range end, if 0, then the animation end is used
    public int rangeEnd = -1;
    
    // which frame to consider as starting frame
    public int startFrame = 0;
    
    // next action to run after this one is finished
    // with ContinueNext
    public Action nextAction = null;
    
    public Action copy(Action toCopy) {
        toCopy.id = id;
        toCopy.loop = loop;
        toCopy.exclusiveGroup = exclusiveGroup;
        toCopy.rangeStart = rangeStart;
        toCopy.rangeEnd = rangeEnd;
        toCopy.startFrame = startFrame;
        toCopy.nextAction = nextAction;
        return toCopy;
    }
}
