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

package com.vlengine.scene.animation.x;

import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix4f;
import com.vlengine.scene.animation.Action;
import com.vlengine.scene.animation.Bone;
import com.vlengine.scene.animation.BoneAnimationController;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.util.FastList;
import java.util.Arrays;


/**
 *
 * @author vear (Arpad Vekas)
 */
public class XBoneAnimationController extends BoneAnimationController {
    
    // the X boneanimation controller works on shader uniform arrays
    // holding transformed interpolated bone matrices
    
    // the currently active animations
    protected FastList<XAction> activeAnim = new FastList<XAction>();

    // the animations we are transitioning into, animations here will replace
    // those in active, that have the same group as these
    protected FastList<XAction> scheduledAnim = new FastList<XAction>();

   
    // the animated items, these hold references to data which
    // we result in animation in the skinning shader
    protected FastList<XAnimatedItem> animated = new FastList<XAnimatedItem>();
    
    // the animation pack we are using to get animations
    protected XBoneAnimationPack animPack;
    // the root bone
    //protected Bone rootBone;

    // the actions we use internaly
    protected FastList<XAction> free = new FastList<XAction>();
    
    // the active bones during animation
    protected boolean[] activeBones;
    // the bone transforms in an animation state
    protected Matrix4f[] boneTransforms;
    protected Matrix4f[] skinTransforms;

    // temp matrix for applying the matrixoffset
    private Matrix4f tempMat = new Matrix4f();
    // tmp action when blending with next animation
    protected XAction nextAction = new XAction();
    
    public XBoneAnimationController(String name) {
        super(name);
    }
    
    public FastList<XAnimatedItem> getAnimatedItems() {
        return animated;
    }
    
    public XBoneAnimationPack getAnimationPack() {
        return animPack;
    }
    
    public void setAniamtionPack(XBoneAnimationPack pack) {
        animPack = pack;
        // find the root none
        /*
        rootBone = null;
        FastList<Bone> bones = animPack.getBones();
        for(int i=0; i<bones.size() && rootBone == null; i++) {
            Bone b = bones.get(i);
            if(b!=null && b.parent==null)
                rootBone = b;
        }
         */
        shouldUpdate = true;
    }

    protected void setAction(XAction xa, Action a) {
        xa.sheduledTime = referenceTime+a.time;
        // find the named animation
        xa.anim = animPack.getAnimation(a.id);
        xa.action = a;
    }

    public void scheduleAction(Action a) {
        // create a new action
        XAction xa = getNewAction();

        setAction(xa, a);

        //xa.loop = a.loop;
        int exclusiveGroup = a.exclusiveGroup;
        

        // remove animations in the same group as the one added
        if(exclusiveGroup != 0) {
            // remove all animations in the group
            if(scheduledAnim.size()>0) {
            // go over and find where to insert the action
                for(int i=0; i<scheduledAnim.size(); i++) {
                    XAction xa2 = scheduledAnim.get(i);
                    if(xa2.action.exclusiveGroup == exclusiveGroup) {
                        XAction xo = scheduledAnim.get(i);
                        releaseAction(xo);
                        scheduledAnim.remove(i);
                        i--;
                    }
                }
            }
            // 
            if(activeAnim.size()>0) {
            // go over and find where to insert the action
                for(int i=0; i<activeAnim.size(); i++) {
                    XAction xa2 = activeAnim.get(i);
                    if(xa2.action.exclusiveGroup == exclusiveGroup) {
                        //XAction xo = activeAnim.get(i);
                        releaseAction(xa2);
                        activeAnim.remove(i);
                        i--;
                    }
                }
            }
        }
        // if we did find an animation
        if(xa.anim !=null) {
            int pos = -1;
            if(scheduledAnim.size()>0) {
                // go over and find where to insert the action
                for(int i=0; i<scheduledAnim.size() && pos == -1; i++) {
                    XAction xa2 = scheduledAnim.get(i);
                    if(xa2.sheduledTime<xa.sheduledTime) {
                        // insert it here
                        pos = i;
                    }
                }
            } else {
                pos = 0;
            }
            // add in to the list
            scheduledAnim.add(pos, xa);
        } else {
            releaseAction(xa);
        }
        
        // force update
        shouldUpdate=true;
    }
    
    protected XAction getNewAction() {
        if(free.size()>0) {
            int idx = free.size() -1;
            XAction a = free.get(idx);
            free.remove(idx);
            return a;
        } else
            return new XAction();
    }
    
    protected void releaseAction(XAction a) {
        a.clear();
        free.add(a);
    }
    
    protected void processCurrentFrame(XAction a) {
        
        int cf = 0;
        boolean restart = true;
        while(restart) {
            restart = false;

            int rangeStart = a.action.rangeStart;
            int rangeEnd = a.action.rangeEnd;

            if(rangeEnd==-1)
                rangeEnd = a.anim.frames.length-1;
            int startFrame = a.action.startFrame;
            if(startFrame==0)
                startFrame = rangeStart;

            float frame = ((float)a.anim.frameRate)*(referenceTime-a.sheduledTime) + startFrame;
            // apply loop mode
            int numFrames = rangeEnd - rangeStart +1;

            cf = ((int) frame);
            //int nf = cf+1;
            //float interpolation = frame - FastMath.floor(frame);

            switch(a.action.loop) {
                case StopAtEnd : {
                        // is it over the max frames in the animation?
                        if(cf>=rangeStart+numFrames-1) {
                            // remove this aniamtion
                            a.remove = true;
                            return;
                            //activeAnim.remove(i);
                            //i--;
                            //continue;
                        }
                    } break;
                case ClampAtEnd : {
                    if(cf>=rangeStart+numFrames-1) {
                        cf = rangeStart+numFrames-1;
                    }
                } break;
                case RestartAtEnd : {
                    cf = (cf-rangeStart)%numFrames + rangeStart;
                } break;
                case ContinueNext : {
                    if(cf>=rangeStart+numFrames-1) {
                        // we finished, set to next animation, if we have one
                        if(a.action.nextAction != null) {
                            float residueTime = (frame - cf)/a.anim.frameRate;
                            a.action.nextAction.copy(a.action);
                            setAction(a, a.action);
                            //a.action.nextAction = null;
                            a.sheduledTime -= residueTime;
                            restart = true;
                            // TODO: we would need to transfer the offset we gained
                            // to the next animation
                        } else {
                            cf = (cf-rangeStart)%numFrames + rangeStart;
                        }
                    }
                } break;
            }
        }
        if( cf < a.anim.frames.length && a.currentFrame != a.anim.frames[cf]) {
            a.currentFrame = a.anim.frames[cf];
            a.needUpdate = true;
        }
    }
    
    protected void processNextFrame(XAction act) {
        
        XAction a = act;
        boolean restart = true;
        int nf = 0;
        
        while(restart) {
            restart = false;

            int rangeStart = a.action.rangeStart;
            int rangeEnd = a.action.rangeEnd;

            if(rangeEnd==-1)
                rangeEnd = a.anim.frames.length-1;
            int startFrame = a.action.startFrame;
            if(startFrame==0)
                startFrame = rangeStart;

            float frame = ((float)a.anim.frameRate)*(referenceTime-a.sheduledTime) + startFrame;
            // apply loop mode
            int numFrames = rangeEnd - rangeStart +1;

            int cf = ((int) frame);
            nf = cf+1;
            float interpolation = frame - FastMath.floor(frame);
            boolean needupdate = false;
            switch(a.action.loop) {
                case StopAtEnd : {
                        // is it over the max frames in the animation?
                        if(cf>=rangeStart+numFrames-1) {
                            // remove this aniamtion
                            a.remove = true;
                            return;
                            //activeAnim.remove(i);
                            //i--;
                            //continue;
                        }
                    } break;
                case ClampAtEnd : {
                    if(nf>rangeStart+numFrames-1) {
                        a.nextFrame = null;
                        return;
                    }
                } break;
                case RestartAtEnd : {
                    nf = (nf-rangeStart)%numFrames + rangeStart;
                } break;
                case ContinueNext : {
                    if(nf>=rangeStart+numFrames-1) {
                        // we finished, set to next animation, if we have one
                        if(a.action.nextAction != null) {
                            float residueTime = (frame - cf)/a.anim.frameRate;
                            setAction(nextAction, a.action.nextAction);
                            nextAction.sheduledTime -= residueTime;
                            a = nextAction;
                            restart = true;
                        } else {
                            nf = (nf-rangeStart)%numFrames + rangeStart;
                        }
                    }
                }
            }
        }
        if(nf < a.anim.frames.length && act.nextFrame != a.anim.frames[nf]) {
            act.nextFrame = a.anim.frames[nf];
            act.needUpdate = true;
        }

    }
   
    protected void updateAnimations(UpdateContext ctx) {
        // add all sheduled animations that should start
        for(int i=0;i<scheduledAnim.size() && scheduledAnim.get(i).sheduledTime<=this.referenceTime; i++) {
            this.activeAnim.add(scheduledAnim.get(i));
            scheduledAnim.remove(i);
            i--;

            // force update
            shouldUpdate=true;
        }
        // reset active animations
        if(activeBones==null) {
            activeBones = new boolean[animPack.bones.size()];
        }
        Arrays.fill(activeBones, false);
        // reset matrices
        if(boneTransforms==null) {
            boneTransforms = new Matrix4f[animPack.bones.size()];
            for(int i=0; i<boneTransforms.length; i++) {
                boneTransforms[i] = new Matrix4f();
            }
            // reset bone 0 to zero matrix
            boneTransforms[0].zero();
        }

        if(skinTransforms==null) {
            skinTransforms = new Matrix4f[animPack.bones.size()];
            for(int i=0; i<skinTransforms.length; i++) {
                skinTransforms[i] = new Matrix4f();
            }
            // reset bone 0 to zero matrix
            skinTransforms[0].zero();
        }
        
       
        FastList<Bone> bones = animPack.bones;
/*         
        // fill the unrelated bones as zero matrix
        for(int i=0; i<boneTransforms.length; i++) {
            Matrix4f boneTrans = bones.get(i).frameMatrix;
            if(boneTrans==null)
                boneTransforms[i].zero();
        }
        
        // go over and multiply bones transform with parents transforms
        // starting with root bone
        calcBoneFrameTransform(rootBone);
 */

        // do we need to update the items
        boolean updateItems = shouldUpdate;
                
        // calculate frames that needs to be interpolated
        for(int i=0; i< activeAnim.size(); i++) {
            XAction a = activeAnim.get(i);
            a.needUpdate = false;
            
            int rangeStart = a.action.rangeStart;
            int rangeEnd = a.action.rangeEnd;

            if(rangeEnd==-1)
                rangeEnd = a.anim.frames.length-1;
            int startFrame = a.action.startFrame;
            if(startFrame==0)
                startFrame = rangeStart;

            float frame = ((float)a.anim.frameRate)*(referenceTime-a.sheduledTime) + startFrame;
            // apply loop mode
            //int numFrames = rangeEnd - rangeStart +1;

            //int cf = ((int) frame);
            //int nf = cf+1;
            float interpolation = frame - FastMath.floor(frame);
                
            processCurrentFrame(a);
            if(a.remove) {
                // remove this aniamtion
                activeAnim.remove(i);
                i--;
                continue;
            }
            processNextFrame(a);

            //boolean restart = true;
            //while(restart) {
            //    restart = false;
                
                
                /*
                boolean needupdate = false;
                switch(a.action.loop) {
                    case StopAtEnd : {
                            // is it over the max frames in the animation?
                            if(cf>=rangeStart+numFrames-1) {
                                // remove this aniamtion
                                activeAnim.remove(i);
                                i--;
                                continue;
                            }
                            if(a.currentFrame != a.anim.frames[cf]) {
                                a.currentFrame = a.anim.frames[cf];
                                needupdate = true;
                            }
                            if(a.nextFrame != a.anim.frames[nf]) {
                                a.nextFrame = a.anim.frames[nf];
                                needupdate = true;
                            }
                        } break;
                    case ClampAtEnd : {
                        if(cf>=rangeStart+numFrames-1) {
                            cf = rangeStart+numFrames-1;
                            if(a.currentFrame != a.anim.frames[cf]) {
                                a.currentFrame = a.anim.frames[cf];
                                needupdate = true;
                            }
                            if(a.nextFrame != null) {
                                a.nextFrame = a.anim.frames[nf];
                                needupdate = true;
                            }
                        } else {
                            if(a.currentFrame != a.anim.frames[cf]) {
                                a.currentFrame = a.anim.frames[cf];
                                needupdate = true;
                            }
                            if(a.nextFrame != a.anim.frames[nf]) {
                                a.nextFrame = a.anim.frames[nf];
                                needupdate = true;
                            }
                        }
                    } break;
                    case RestartAtEnd : {
                        cf = (cf-rangeStart)%numFrames + rangeStart;
                        nf = (nf-rangeStart)%numFrames + rangeStart;
                        if(a.currentFrame != a.anim.frames[cf]) {
                            a.currentFrame = a.anim.frames[cf];
                            needupdate = true;
                        }
                        if(a.nextFrame != a.anim.frames[nf]) {
                            a.nextFrame = a.anim.frames[nf];
                            needupdate = true;
                        }
                    } break;
                    case ContinueNext : {
                        if(cf>=rangeStart+numFrames-1) {
                            // we finished, set to next animation, if we have one
                            if(a.action.nextAction != null) {
                                float residueTime = (frame - cf)/a.anim.frameRate;
                                setAction(a, a.action.nextAction);
                                a.sheduledTime += residueTime;
                                restart = true;
                                // TODO: we would need to transfer the offset we gained
                                // to the next animation
                            }
                        } else {
                            if(a.currentFrame != a.anim.frames[cf]) {
                                a.currentFrame = a.anim.frames[cf];
                                needupdate = true;
                            }
                            if(nf>=rangeStart+numFrames-1) {
                                // we got the last frame from the old, and the first frame from the new animation
                                float residueTime = (frame - nf)/a.anim.frameRate;
                                setAction(nextAction, a.action.nextAction);
                                nextAction.sheduledTime += residueTime;
                            }
                        }
                    }
                    
                }
                */
                if(a.needUpdate) {
                    a.needUpdate = false;
                    if(a.currentFrame == null || a.nextFrame == null) {
                        System.out.append("null anim");
                    } else {
                        for(int j=0; j<bones.size(); j++) {
                            Bone b = bones.get(j);
                            // this bone is already animated
                            if(b==null || activeBones[j])
                                continue;


                            // calculate the bone transform
                            calcBoneTransform(b, a.currentFrame.transform, a.nextFrame!=null?a.nextFrame.transform:null, interpolation);
                        }
                        updateItems = true;
                    }
                }
            //}
        }
        // update is forced
        // update not yet updated matrices with frame transform
        if(shouldUpdate ) {
            for(int j=0; j<bones.size(); j++) {
                Bone b = bones.get(j);
                if(activeBones[j] || b==null)
                    continue;
                calcBoneTransform(b, null, null, 0);
            }
            updateItems = true;
        }
        if(updateItems) {
            // apply matrix offset
            skinTransforms[0].zero();
            // not to matrix 0 (which is zero matrix)
            for(int j=1, mj=bones.size(); j<mj; j++) {
                Bone b = bones.get(j);
                if(b==null || b.matrixOffset==null)
                    continue;
                
                skinTransforms[j].set(b.matrixOffset).multLocal(boneTransforms[j]);//
                
                //tempMat.set(b.frameMatrix).invertLocal().multLocal(boneTransforms[j]);
                //skinTransforms[j].set(b.matrixOffset).multLocal(tempMat);//
            }

            // apply the bones to animated items
            for(int k=0; k<animated.size(); k++) {
                XAnimatedItem item = animated.get(k);
                item.setMatrixValues(skinTransforms);
                /*
                // go over the matrices and set all that are needed in the item
                item.matrixBuffer.position(0);
                for(int l=0, ml=item.boneMapping.size(); l<ml; l++) {
                    // the bone-s mapped id for this animated item
                    int origBone = item.boneMapping.get(l);
                    //if(origBone!=0) {
                        // TODO: just for testing
                        //skinTransforms[origBone].loadIdentity();
                        // if this bone is actualy required in the item
                        item.matrixBuffer.put(skinTransforms[origBone]);
                    //}
                }
                item.matrixBuffer.rewind();
                //item.shaderParams.setNeedsRefresh(true);
                 */
            }
        }
    }
    
    /**
     * Calculates the bones frame transform, and that of its children recursively
     * @param b
     */
    protected void calcBoneTransform(Bone b, Matrix4f[] transforms1, Matrix4f[] transforms2, float interpolation) {
        
        // this bone is already animated
        //if(activeBones[b.id])
        //    return;
        // if have animation for thius bone
        
        //boneTransforms[b.id].set(b.frameMatrix);
        
        if(transforms1!=null && transforms1[b.id]!=null) {
            // multiply matrix with parents matrix
            //boneTransforms[b.id].set(transforms1[b.id]);
            tempMat.set(transforms1[b.id]);
            // do we interpolate?
            if(transforms2!=null && transforms2[b.id]!=null ) {
                // TODO: maybe we need slerp on rotation?
                tempMat.interpolate(transforms2[b.id], interpolation);
            }
            boneTransforms[b.id].set(tempMat);
            //boneTransforms[b.id].set(b.frameMatrix).invertLocal();
            //boneTransforms[b.id].multLocal(tempMat);
        } else {
            // apply frame transform
            boneTransforms[b.id].set(b.frameMatrix);
            //boneTransforms[b.id].loadIdentity();
        }
          
        //boneTransforms[b.id].multLocal(b.frameMatrix);
        
        /*
        // do we interpolate?
        if(transforms2!=null && transforms2[b.id]!=null ) {
            // TODO: maybe we need slerp on rotation?
            boneTransforms[b.id].interpolate(transforms2[b.id], interpolation);
        }
         */
             
        if(b.parent != null) {
            // if parent is not yet animated animate it
            if(!activeBones[b.parent.id])
                calcBoneTransform(b.parent, transforms1, transforms2, interpolation);
            //tempMat.set(boneTransforms[b.id]);
            //boneTransforms[b.id].set(boneTransforms[b.parent.id]).multLocal(tempMat);
            
            boneTransforms[b.id].multLocal(boneTransforms[b.parent.id]);
        }

        activeBones[b.id] = true;
        /*
        // process our children
        for(int i=0, mi=animPack.bones.size(); i<mi; i++) {
            Bone cb = animPack.bones.get(i);
            if(cb!=null && cb.parent == b) {
                // child bone, process it
                calcBoneTransform(cb, transforms1, transforms2, interpolation);
            }
        }
         */
    }

}
