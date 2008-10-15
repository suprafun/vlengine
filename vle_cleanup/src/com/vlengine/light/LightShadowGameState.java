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

package com.vlengine.light;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.app.state.GameState;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.Camera;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.pass.DepthTexturePass;
import com.vlengine.renderer.pass.LightPass;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.util.FastList;
import java.util.HashMap;

/**
 * This gamestate is supposed to managed lights as LightSorterGameState
 * but also to managed perspective split shadow maps of those lisghts too.
 * TODO: this is incomplete code
 * 
 * @author vear (Arpad Vekas)
 */
public class LightShadowGameState extends GameState {

    // the sorter used to sort lights
    protected LightSorter ls = new LightSorter();
    
    // the frustum splits for perspective shadow mapping
    int frustumSplits = 1;
    float[] frustumSplitDistances;
    ViewCamera[] frustumSplitCamera;
    
    @Override
    public void preFrame(AppContext ctx) {
        // store the current config parameters
        frustumSplits = ctx.conf.shadowmap_splits;
    }

    @Override
    public void preUpdate(UpdateContext ctx) {
        
    }

    // object pooling for arrays
    FastList<FastList<Renderable>[]> queuPool = new FastList<FastList<Renderable>[]>();
    FastList<FastList<Renderable>[]> usedPool = new FastList<FastList<Renderable>[]>();
    
    //HashMap<ShadowPart, FastList<Renderable>[]> currentShadows = new HashMap<ShadowPart, FastList<Renderable>[]>();
    // for each non-shadowing light, arrays of renderables for opaque and transparent queues
    HashMap<LightBatch, FastList<Renderable>[]> currentLights = new HashMap<LightBatch, FastList<Renderable>[]>();
    // for each shadowing-light
    HashMap<LightBatch, FastList<Renderable>[]> currentShadowingLights = new HashMap<LightBatch, FastList<Renderable>[]>();
    
    // the list of usable renderpasses for shadow creation
    FastList<DepthTexturePass> freeDepthPasses = new FastList<DepthTexturePass>();
    FastList<DepthTexturePass> usedDepthPasses = new FastList<DepthTexturePass>();

    // the list of usable renderpasses for rendering lights with shadows
    FastList<LightPass> freeLightPasses = new FastList<LightPass>();
    FastList<LightPass> usedLightPasses = new FastList<LightPass>();

    Vector3f[] frustumPoints = new Vector3f[8];
    Vector3f frustumCenter = new Vector3f();

    // have we processed lights for the current frame
    boolean lightsprocessed = false;
    // are all the nesessary passes been processed?
    
    public void preCull(Frame f) {
        lightsprocessed = false;
    };

    public void postCull(CullContext ctx) {

        // if we processed lights for this frame already, do nothing
        if(lightsprocessed)
            return;
        
        Frame f = ctx.getFrame();

        // if the scene is yet to be culled against some cameras
        // return
        if(f.hasCameraToCull())
            return;

        // mark that we not process lights again for this frame
        lightsprocessed = true;

        // shadowmaps which need to be updated are processed here,
        // and for each a DepthTexturePass is put into the context
        FastList<Renderable> lights = f.getQueueManager().getQueue(RenderQueue.StandardQueue.Ligh.queuId);
        
        // the opaque geometry
        FastList<Renderable> opaque = f.getQueueManager().getQueue(RenderQueue.StandardQueue.Opaque.queuId);
        
        // the transparent (we dont process this?)
        FastList<Renderable> transparent = f.getQueueManager().getQueue(RenderQueue.StandardQueue.AlphaTested.queuId);
        
        // the two-sided transparent (this is the "glass" geometry), and should have
        // shine from light
        FastList<Renderable> twosided = f.getQueueManager().getQueue(RenderQueue.StandardQueue.AlphaBlended.queuId);
        
        // we will gather shadowparts
        //currentShadows.clear();
        // we will gather lights without shadows here
        currentLights.clear();

        // TODO: this is not multithread-friendly
        // put all used pooled objects back to unused
        queuPool.addAll(usedPool);
        usedPool.clear();
        
        // sort out the opaque queue
        sortQueueForLights(f.getFrameId(), 0, opaque, lights);
        
        // sort out the tranparent queue, those will be lit like opaque
        sortQueueForLights(f.getFrameId(), 0, transparent, lights);
        
        // sort out the twosided transparent, thise will be lit like tranparent
        sortQueueForLights(f.getFrameId(), 1, twosided, lights);

        // get the main view camera
        ViewCamera vc = f.getCamera();
        // get the number of frustum splits
        
        // calculate the frustum splits for perspective shadowmap
        if(frustumSplitDistances==null || frustumSplitDistances.length != frustumSplits) {
            frustumSplitDistances = ShadowUtils.calculateSplitDistances(frustumSplits, vc.getFrustumNear(), vc.getFrustumFar(), frustumSplitDistances);
            // construct a camera for each of the splits
            frustumSplitCamera = new ViewCamera[frustumSplits];
            for(int i=0; i<frustumSplits; i++) {
                frustumSplitCamera[i] = new ViewCamera();
            }
        }
        
        // copy over data from main camera to spits cameras
        for(int i=0; i<frustumSplits; i++) {
            // copy over the data from main camera
            vc.copy(frustumSplitCamera[i]);
            // but set the near, and far as proper for the split
            frustumSplitCamera[i].setFrustumNear(frustumSplitDistances[i]);
            frustumSplitCamera[i].setFrustumFar(frustumSplitDistances[i]+1);
            // update the split camera
            frustumSplitCamera[i].update();
            
        }
        
        // handling of perspective shadowing
        // go over each split, 
        for(int i=0;i<frustumSplits;i++) {
            ViewCamera sc = frustumSplitCamera[i];
            
            // calculate the frustum points and center
            sc.getFrustumCorners(frustumPoints, frustumCenter);
            
            // get the frustum points
            
            // then select all opaque and transparent geometry that falls in that split
            // and is affected by the light

            // collect all slices of shadowing visible lights
            for(LightBatch lb:currentShadowingLights.keySet()) {
                // select the lights that have effect in that split
                // if the light has bounds, but it does not intersect the frustum split
                BoundingVolume lbound = lb.getWorldBound(f.getFrameId());
                if(lbound != null) {
                    // check aginst the bound
                    if(sc.contains(lbound)==Camera.OUTSIDE_FRUSTUM)
                        continue;  // the light does not affect current split
                }
                
                // check if there are any objects that are affected by the light
                FastList<Renderable>[] objects = currentShadowingLights.get(lb);
                if( objects == null || ( ( objects[0] == null || objects[0].size() == 0 )
                                        && ( objects[1] == null || objects[1].size() == 0 )))
                    continue; // nothing to light or shadow

                // this light casts shadows in the current split
                Shadow s = lb.getShadow();
                // ensure that the shadow can hold the split
                s.setPerspectiveSplits(i);
                // get the split
                ShadowPart sp = s.getSplits().get(i);
                // set dimension based on config setting
                sp.setDimension(f.getConfig().shadowmap_dimension>>i);

                // select objects that are in camera split
                FastList<Renderable>[] allshadowed = prepareShadowedObjectsInSplit(lb, sc, sp, f.getFrameId());
                if(allshadowed == null)
                    continue; // nothing to light in this split
                
                // prepare perspective camera
                prepareShadowPartCamera(lb, sp, f.getFrameId());

                // prepare depth pass
                prepareShadowDepthPass(sp, f);

                // prepare shadowed lighting pass for geometry
                // in the split
                prepareLightShadowPass(sc, allshadowed, lb, sp, f);
                    
                    
            }
        }
        
        // TODO: handling of non-shadow caster lights
        for(LightBatch lb:currentLights.keySet()) {
            // check if there are any objects that are affected by the light
            FastList<Renderable>[] objects = currentLights.get(lb);
            if( objects == null || ( ( objects[0] == null || objects[0].size() == 0 )
                                    && ( objects[1] == null || objects[1].size() == 0 )))
                continue; // nothing to light
            // prepare all the batches for the light
            // main camera
            // objects lit
            // the light
            // no shadow
            // the frame
            prepareLightShadowPass(vc, objects, lb, null, f);
        }

        
        
        // with light indexed rendering, sort lights into no-overlapping arrays
        
        // add the lights that cast shadows
        
        
        // extract the light batches from the lights queue
    }
    
    protected FastList<Renderable>[] prepareShadowedObjectsInSplit(LightBatch lb, ViewCamera sc, ShadowPart sp, int frameId) {
        FastList<Renderable>[] allshadowed = null;
                
        FastList<Renderable>[] allobjects = currentShadowingLights.get(lb);
        
        for(int i=0; i<2; i++) {
            FastList<Renderable> objects = allobjects[i];
            if(objects==null || objects.size() == 0)
                continue;
            // go over all batches, and see if they are in the camera split frustum
            for(int j=0; j<objects.size(); j++) {
                Renderable r = objects.get(j);
                if(sc.contains(r.getWorldBound(frameId)) != Camera.OUTSIDE_FRUSTUM) {
                    if(allshadowed==null)
                        allshadowed = this.allocQueue();
                    // put it into the shadowparts list
                    allshadowed[i].add(r); 
                }
            }
        }
        // if we collected anything, put the queues into the map
        return allshadowed;
    }

    /**
     * Prepare a pass to light or shadow a section of the view frustum using a shadowmap
     * made in a DepthTesturePass pass
     * 
    */
    protected void prepareLightShadowPass(ViewCamera sc, FastList<Renderable>[] objects, LightBatch lb, ShadowPart sp, Frame f) {
        // allocate a new lightpass
        int flp = this.freeLightPasses.size() - 1;
        LightPass lp = null;
        if(flp>0) {
            lp = freeLightPasses.get(flp);
            freeLightPasses.remove(flp);
            
        } else {
            // allocate new
            lp = new LightPass("lightpass");
            // allocate an id for it
            lp.setId(f.getApp().genPassId());
            // allocate queue id for it
            lp.setQueueNo(f.getApp().genQueId());
            // do not gather anything, we will fill it
            lp.setQueueFilter(0);
            // TODO: fix this when materials are differentiated
            lp.setUsedMaterialFlags(0);
            lp.setEnabled(true);
        }
        // add the pass to the used ones
        usedLightPasses.add(lp);
        // set parameters into the pass
        
//lp.setLight(lb.getLight());
//lp.setShadow(sp);
        
        // the frustum split camera to use
        lp.setCamera(sc);
        // use the default renderer
        //lp.setRenderer(f.getApp().display.getRenderer());
        // declare queue for the pass
        f.getQueueManager().createQueue(lp.getQueueNo(), null);
        // fill the queue with selected objects
        // both opaque and transparent? (maybe tranparent will need another pass?
        f.getQueueManager().addAll(lp.getQueueNo(), objects[0]);
        f.getQueueManager().addAll(lp.getQueueNo(), objects[1]);
        // add the pass to the passmanager
        f.getPasses().addPass(lp);
        // depends on transparent, but before ortho
        // if we separate opaque and transparent lighting, then for opaque,
        // opaque should be here
/*
RenderPass transp = f.getPasses().getPass("alpha");
RenderPass ortho = f.getPasses().getPass("ortho");
f.getPasses().addDependency(lp, transp);
f.getPasses().addDependency(ortho, lp);
 */
        
    }

    /**
     * Prepare a pass to refresh a shadowmap
     * @param sp
     * @param f
     */
    protected void prepareShadowDepthPass(ShadowPart sp, Frame f) {
        // add a shadowmap texture creation pass to passes
        DepthTexturePass dp=null;

        // try get it from the list of free ones
        if(freeDepthPasses.size()>0) {
            int idx = freeDepthPasses.size()-1;
            while(dp==null && idx>=0) {
                dp = freeDepthPasses.get(idx);
                if(dp.getDimension() == sp.getDimension()) {
                    // the pass is of right dimension
                    // remove from free
                    freeDepthPasses.remove(idx);
                } else
                    dp =null;
            }
        }
        if(dp==null) {
            // create new
            dp = new DepthTexturePass();
            // create id
            dp.setId(f.getApp().genPassId());
            // create queue id
            dp.setQueueNo(f.getApp().genQueId());
            // collect opaque
            dp.setQueueFilter(RenderQueue.QueueFilter.Opaque.value);
            // set dimension
            dp.setDimension(sp.getDimension());

        }
        // add it to used
        usedDepthPasses.add(dp);
        // set the shadow part into the pass
        dp.setShadowPart(sp);
        // add this pass: depends on lightextract
        // but before opaque
/*
f.getPasses().addPass(dp);
f.getPasses().addDependency(dp, f.getPasses().getPass("lightextract"));
f.getPasses().addDependency(f.getPasses().getPass("opaque"), dp);
 */
        // need a queue for this pass
        f.getQueueManager().createQueue(dp.getQueueNo(), null);
        // notify Frame that we updated the list of cameras
        f.addCameraToCull(sp.getCamera());
    }

    protected void prepareShadowPartCamera(LightBatch lb, ShadowPart sp, int frameid) {
        // construct a shadowmap camera for the light in that split
        ViewCamera shcam = sp.getCamera();
        if(shcam == null) {
            shcam = new ViewCamera();
            sp.setCamera(shcam);
        }

        // set the light view matrix
        Transform lightTrans = lb.getWorldTransForm(frameid);
        // for directional light set position relative to the center of the split
        // so that it looks at the center of it
        if(lb.getLight().getType()==Light.LT_DIRECTIONAL) {
            shcam.getLocation().set(frustumCenter).addLocal(lightTrans.getTranslation());
            // look at the center of the split
            shcam.lookAt(frustumCenter, Vector3f.UNIT_Y);
        } else {
            // TODO: handle other light types
        }
        // set frustrum
        shcam.setFrustumPerspective(45.0f, 1.0f, 1.8f, 50.0f);
        // zoom-in the frustum on the main camera perspective split
        shcam.setZoom(frustumPoints);
        shcam.update();
    }

    protected FastList<Renderable>[] allocQueue() {
        // fetch an object from the top
        FastList<Renderable>[] qu;
        int qps = queuPool.size();
        if( qps > 0) {
            qu = queuPool.get(qps-1);
            queuPool.remove(qps-1);
            qu[0].clear();
            qu[1].clear();
        } else {
            // reate a new one
            qu = new FastList[2];
            qu[0] = new FastList<Renderable>();
            qu[1] = new FastList<Renderable>();
        }
        // add it to the used list
        usedPool.add(qu);
        return qu;
    }
    
    protected void sortQueueForLights(int frameid, int listno, FastList<Renderable> queue, FastList<Renderable> lights) {
        // clear out the lights for opaque (we will fill them later in)
        for(int j=0, mj=queue.size(); j<mj; j++) {
            Renderable or=queue.get(j);
            or.lights[frameid][0] = null;
            or.lightpriority[frameid][0] = 0;
            or.lights[frameid][1] = null;
            or.lightpriority[frameid][1] = 0;
        }
        //process all the lights
        for(int i=0, mi=lights.size(); i<mi; i++) {
            // ignore anything other than lightbatch in light queue
            if(lights.get(i) instanceof LightBatch) {
                LightBatch lb = (LightBatch) lights.get(i);
                // skip disabled lights
                if(!lb.getLight().isEnabled())
                    continue;
                
                // check this light against all the opaque, calculate a coeficient
                // based on distance
                for(int j=0, mj=queue.size(); j<mj; j++) {
                    Renderable or=queue.get(j);
                    float value = ls.getValueFor(lb, or);
                    // if the batch isnt lit by this light, skip
                    if(value==0)
                        continue;
                    // store the light, and value into the batch
                    if( value > or.lightpriority[frameid][0]) {
                        // shift light in 0 to light 1
                        or.lights[frameid][1] = or.lights[frameid][0];
                        or.lightpriority[frameid][1] = or.lightpriority[frameid][0];
                        // store as light 0
                        or.lights[frameid][0] = lb;
                        or.lightpriority[frameid][0] = value;
                    } else if(value > or.lightpriority[frameid][1]) {
                        // store as light 1
                        or.lights[frameid][1] = lb;
                        or.lightpriority[frameid][1]  = value;
                    } else
                        continue;  // we already have two bigger priority lights
                }
            }
        }        

        // collect all lights that finished as the 2 most influential lights for
        // a batch
        for(int j=0, mj=queue.size(); j<mj; j++) {
            Renderable or=queue.get(j);
            for(int k=0; k<2; k++) {
                LightBatch lb = or.lights[frameid][k];
                if(lb==null)
                    continue;
                // we go a lightbatch, that is used by a renderable
                FastList<Renderable>[] alllightbatches;
                // decide, if this lights is to be processed as shadowing,
                // or non-shadowing light
                if(lb.getLight().isShadowCaster() && lb.getShadow()!=null)
                    alllightbatches = currentShadowingLights.get(lb);
                else
                    alllightbatches = currentLights.get(lb);
                if(alllightbatches==null) {
                    alllightbatches = allocQueue();
                    if(lb.getLight().isShadowCaster() && lb.getShadow()!=null)
                        currentShadowingLights.put(lb, alllightbatches);
                    else
                        currentLights.put(lb, alllightbatches);
                }
                alllightbatches[listno].add(or);
            }
        }
    }

    @Override
    public void preCull(CullContext ctx) {
    }

    @Override
    public void preMaterial(RenderContext ctx) {
        
    }

    @Override
    public void preRender(RenderContext ctx) {
        
    }

    @Override
    public void postRender(RenderContext ctx) {
        // TODO: clean up not used shadows
        
        // TODO: clean up not used splits from used shadows
        
        // free up all shadow creation passes
        freeDepthPasses.addAll(usedDepthPasses);
        usedDepthPasses.clear();
        // free up all the sorting queues
        queuPool.addAll(this.usedPool);
        usedPool.clear();
        
        currentLights.clear();
        currentShadowingLights.clear();
        
        // free up all lighting passes
        freeLightPasses.addAll(usedLightPasses);
        usedLightPasses.clear();
    }

    @Override
    public void cleanup() {
        // clean up all depth creation passes
        freeDepthPasses.addAll(usedDepthPasses);
        usedDepthPasses.clear();
        for(int i=0, mx=freeDepthPasses.size(); i<mx; i++) {
            freeDepthPasses.get(i).cleanup();
        }
        freeDepthPasses.clear();
    }

    @Override
    public void afterRender(RenderContext ctx) {
        
    }

}
