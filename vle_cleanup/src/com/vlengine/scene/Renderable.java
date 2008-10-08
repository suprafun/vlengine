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

package com.vlengine.scene;

import com.vlengine.app.frame.Frame;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.light.Light;
import com.vlengine.math.Transform;
import com.vlengine.renderer.Camera;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.state.RenderState;
import com.vlengine.system.VleException;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.BitSet;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import java.util.Arrays;

/**
 * Interface for object which can be rendered, only renderable objects
 * can be placed into renderqueues.
 * 
 * @author vear (Arpad Vekas)
 */
public abstract class Renderable extends SceneElement {
    
    // members in this class are duplicated so that
    // while one set of data is updated, the other set can be rendered
    
    // world bounds for every frame
    protected BoundingVolume[] worldBound;
    
    // the reference to parents transform for its children
    protected Transform[] worldTransform;
    
    // material information for this renderable
    protected Material material;
   
    protected boolean needupdate[];
    
    // the set of materials which need to be updated
    //protected final BitSet mlist = new BitSet();
    protected boolean updateMaterial = false;
    
    // the id-s for sorting the queues
    protected final IntList idlist = new IntList( 3 );
    
    // how many lights we handle in the low profile
    public static final int LOWPROFILE_LIGHTS = 3;
    // we hold lights for two frames, two lights, and two values
    public final LightBatch[][] lights = new LightBatch[Frame.MAX_FRAMES][LOWPROFILE_LIGHTS];
    public final float[][] lightpriority = new float[Frame.MAX_FRAMES][LOWPROFILE_LIGHTS];
    
    public Renderable() {
        needupdate=new boolean[Frame.MAX_FRAMES];
        Arrays.fill(needupdate, true);
        // default is opaque for renderables
        renderQueueMode = RenderQueue.QueueFilter.Opaque.value;
    }
    
    
    public void setNeedUpdate(boolean upd) {
        Arrays.fill(needupdate, upd);
    }
    
    public void setNeedUpdate( int frameId, boolean upd ) {
        needupdate[frameId] = upd;
    }
    
    public boolean isNeedUpdate( int frameId ) {
        return needupdate[frameId];
    }
    
    @Override
    public BoundingVolume getWorldBound( ) {
        return getWorldBound(getFrameId());
    }
    
    public void updateWorldBound() {
        updateWorldBound(getFrameId());
    }

    public abstract void updateWorldBound(int frameid);

    public BoundingVolume getWorldBound( int frameId ) {
        return worldBound[ frameId ];
    }
    
    public void setWorldTransform( Transform[] t ) {
        this.worldTransform = t;
    }
    
    public Transform getWorldTransForm() {
        return worldTransform[getFrameId()];
    }
    
    public Transform getWorldTransForm( int frameId ) {
        return worldTransform[frameId];
    }
    
    public void setMaterial( Material mat ) {
        material = mat;
    }
    
    public Material getMaterial( ) {
        return material;
    }
   
    // this method decides if this batch should be rendered
    // if the batch does not maintain its own bound, its best to
    // set it to: return true
    @Override
    public boolean docull( CullContext ctx ) {
        
        CullMode cm = getCullMode();
        if (cm == Spatial.CullMode.ALWAYS) {
            setLastFrustumIntersection(Camera.OUTSIDE_FRUSTUM);
            return false;
        } else if (cm == Spatial.CullMode.NEVER) {
            setLastFrustumIntersection(Camera.INSIDE_FRUSTUM);
            return true;
        }
        
        // check to see if we can cull this node
        frustrumIntersects = (parent != null ? parent.frustrumIntersects
                : Camera.INTERSECTS_FRUSTUM);


        if (cm == Spatial.CullMode.DYNAMIC && frustrumIntersects == Camera.INTERSECTS_FRUSTUM) {
            frustrumIntersects = ctx.getCullCamera().contains(worldBound[ctx.getFrameId()]);
        }

        if (frustrumIntersects != Camera.OUTSIDE_FRUSTUM) {
            return true;
        }
        
        return false;
    }
    
    
    @Override
    public boolean queue( CullContext ctx ) {
        boolean found = false;
        // check the queue mode
        //long cm = getRenderQueueMode();
        if ( renderQueueMode != RenderQueue.QueueFilter.None.value ) {
            // get variable for holding data on queues
            Context tmp = LocalContext.getContext();
            BitSet qlist=tmp.qlist;
            qlist.clear();
            
            // check for every our pass
            for( int i = 0; i < ctx.getPassQuantity(); i++ ) {
                RenderPass p = ctx.getPass( i );
                if(isUsePass(ctx, p)) {
//                long pcm = p.getQueueFilter();
//                if( ( cm & pcm ) != 0 ) {
                    // the element passed for at least one
                    found = true;
                    
                    // update the batch (copy data from the parent)
                    // this is here, so the batch is updated before materials
                    update( ctx );
                        
                    if( needupdate[ ctx.getFrameId() ] ) {
                        // no need to update this
                        // the update method can reset this, so it will be called again
                        needupdate[ ctx.getFrameId() ] = false;

                        // add this batch to prepare list
                        ctx.addToPrepareList( this );

                    }
                    // mark the queue for the pass
                    int qn=p.getQueueNo();
                    if( !qlist.contains( qn ) ) {
                        // the object has not yet been placed in this queue
                        qlist.add( qn );
                        // compute the id for the queue
                        recalculateId( ctx, qn );
                        // place it into the queue
                        ctx.addToQueue( qn, this );
                    }
                        
                    // mark the materials which will be used in the scene
                    int mn = p.getUsedMaterialFlags();
                    // if the pass uses a material, and we have not yet updated it
                    if( mn != -1 ) {
                        // if this is the first use of the material
                        if(!updateMaterial) {
                            // update material with data from scene
                            Material mat = getMaterial( );
                            if( mat !=null && mat.isNeedUpdate() ) {
                                // update material
                                mat.update( ctx );
                            }
                        }
                        // mark the material for prepare
                        updateMaterial = true;
                    }
                }
            }
        }
        return found;
    }
    
    // this method should gather all data into this renderable
    // befor the rendering, this method is only called once per frame
    // but only if the batch is selected for rendering
    // if continous updating is needed, then this method should
    // set needUpdate[] to true
    public abstract void update( CullContext ctx );
    
    // recalculate the id of this batch used for sorting in the
    // given queue. this is called when the batch is selected
    // for rendering in a given queue
    protected void recalculateId(CullContext ctx, int idn) {
        // reset
        idlist.set(idn, 0);

        // the fixed queues:
        // 0 opaque         - sort by material (shader, texture)
        //                    then front to back
        // 1 transparent    - sort back to front
        // 2 ortho          - no sort (maybe Z order)?
        RenderQueue.SortType st = ctx.getQueueManager().getSortId(idn);
        if(st!=null) {
            ViewCamera vc = ctx.getViewCamera();
            switch(st) {
                // front to back
                case DistanceSquaredFromCamera : {
                    float dist;
                    BoundingVolume bv = this.getWorldBound(ctx.getFrameId());
                    if(bv!=null) {
                        // if we have a bound, calculate it to the bound center
                        dist = bv.distanceSquaredTo(vc.getLocation());
                    } else {
                        // else, calculate from objects position
                        dist = this.getWorldTransForm(ctx.getFrameId()).getTranslation().distanceSquared(vc.getLocation());
                    }
                    
                    float frustSq = vc.getFrustumFar()*vc.getFrustumFar();
                    int sid;
                    if(dist > frustSq) {
                        sid = Integer.MAX_VALUE;
                    } else {
                        sid = (int)(dist*((float)Integer.MAX_VALUE)/frustSq);
                    }
                    idlist.set(idn, sid);
                } break;
                // back to front
                case InverseDistanceSquaredFromCamera : {
                    BoundingVolume bv = this.getWorldBound(ctx.getFrameId());
                    float dist;
                    if(bv!=null) {
                        // if we have a bound, calculate it to the bound center
                        dist = bv.distanceSquaredTo(vc.getLocation());
                    } else {
                        // else, calculate from objects position
                        dist = this.getWorldTransForm(ctx.getFrameId()).getTranslation().distanceSquared(vc.getLocation());
                    }
                    float frustSq = vc.getFrustumFar()*vc.getFrustumFar();
                    int sid;
                    if(dist < 0.001) {
                        sid = Integer.MAX_VALUE;
                    } else {
                        sid = (int)(((float)Integer.MAX_VALUE)/dist);
                    }
                    idlist.set(idn, sid);
                } break;
                // back to front
                case AbsoluteDepth : {
                    float dpth = this.getWorldTransForm(ctx.getFrameId()).getTranslation().z;
                    int sid;
                    if(dpth < 0.001) {
                        sid = Integer.MAX_VALUE;
                    } else {
                        sid = (int)(((float)Integer.MAX_VALUE)/dpth);
                    }
                    idlist.set(idn, sid);
                } break;
            }
        }
    }
    
    // this method is used by the renderqueue to order the batch
    public int getSortId( int queueNo ) {
        return idlist.get( queueNo );
    }
    
    
    /**
     * this method is called in MATERIAL phase
     * it can access to both the scene and the rendercontext
     * it is only called if previously update() was called
     * the batch can ensure that its called, by setting needUpdate[] to true
     */
    public boolean prepare( RenderContext ctx ) {
        //int frameId = ctx.getFrameId();
        // update the materials
        if( updateMaterial ) {
            Material mat = getMaterial();
            if( mat !=null && mat.isNeedRefresh() ) {
                mat.prepare( ctx );
            }
        }
        // clear the material list for, it will be filled next frame
        updateMaterial=false;
        return true;
    }
    
    // this method is called to render the object
    // it should be: ctx.getRenderer().draw(ctx, this);
    // anything else put into this method should not
    // access the scene
    public abstract void draw(RenderContext ctx);


}
