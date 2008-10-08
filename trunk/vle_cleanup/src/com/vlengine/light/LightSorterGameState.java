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
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.util.FastList;
import com.vlengine.util.IntMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This gamestate is 
 * @author vear (Arpad Vekas)
 */
public class LightSorterGameState extends GameState {

    // the sorter used to sort lights
    protected LightSorter ls = new LightSorter();
        
    @Override
    public void preFrame(AppContext ctx) {
        
    }

    @Override
    public void preUpdate(UpdateContext ctx) {
        
    }

    // object pooling for arrays
    //FastList<FastList<Renderable>[]> queuPool = new FastList<FastList<Renderable>[]>();
    //FastList<FastList<Renderable>[]> usedPool = new FastList<FastList<Renderable>[]>();
    
    // the map of all lightstates that a light is in
    FastList<HashMap<Light,FastList<LightState>>> lightOccurences = new FastList<HashMap<Light,FastList<LightState>>>();

    // the mapping of lights to unique ids
    int lightIdCounter = 0;
    HashMap<LightBatch,Integer> lightId = new HashMap<LightBatch,Integer>();
    HashMap<Integer,LightBatch> lightById = new HashMap<Integer,LightBatch>();
    
    // the array used for sorting lights by id
    Integer[] sortedBatchLightIds = new Integer[Renderable.LOWPROFILE_LIGHTS];
    Light[] sortedBatchLights = new Light[Renderable.LOWPROFILE_LIGHTS];
    
    // the list of renderables affected by a light
    //HashMap<LightState, FastList<Renderable>[]> affectedBatches = new HashMap<LightState, FastList<Renderable>[]>();

    // the list of usable renderpasses for rendering lights with shadows
    //HashSet<LightState> usedLightPasses = new HashSet<LightState>();
    FastList<LightState> allLightPasses = new FastList<LightState>();

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

        // we will gather passes here
        // add the pass to the used ones
        //usedLightPasses.clear();

        // put all used pooled objects back to unused
        //queuPool.addAll(usedPool);
        //usedPool.clear();
        
        // sort out the opaque queue
        sortQueueForLights(f, 0, opaque, lights);
        
        // sort out the tranparent queue, those will be lit like opaque
        if(transparent.size()>0) {
            sortQueueForLights(f, 1, transparent, lights);
        }
        
        // sort out the twosided transparent, thise will be lit like tranparent
        sortQueueForLights(f, 2, twosided, lights);
        
        // clear out the main queues
        
    }

    /*
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
            qu = new FastList[3];
            qu[0] = new FastList<Renderable>();
            qu[1] = new FastList<Renderable>();
            qu[2] = new FastList<Renderable>();
        }
        // add it to the used list
        usedPool.add(qu);
        return qu;
    }
     */
    
    protected void sortQueueForLights(Frame f, int listno, FastList<Renderable> queue, FastList<Renderable> lights) {
        int frameid = f.getFrameId();
        // clear out the lights for opaque (we will fill them later in)
        for(int j=0, mj=queue.size(); j<mj; j++) {
            Renderable or=queue.get(j);
            if(or.getMaterial()!=null) {
                for(int i=0; i<Renderable.LOWPROFILE_LIGHTS; i++) {
                    or.lights[frameid][i] = null;
                    or.lightpriority[frameid][i] = 0;
                }
                // clear out the lightstate too
                if(or.getMaterial().states!=null)
                    or.getMaterial().states[RenderState.RS_LIGHT] = null;
            }
        }
        //process all the lights
        for(int i=0, mi=lights.size(); i<mi; i++) {
            // ignore anything other than lightbatch in light queue
            if(lights.get(i) instanceof LightBatch) {
                LightBatch lb = (LightBatch) lights.get(i);
                // skip disabled lights
                if(!lb.getLight().isEnabled())
                    continue;
                
                // ensure the light has id
                Integer lightid = lightId.get(lb);
                if(lightid==null) {
                    lightid = new Integer(++lightIdCounter);
                    lightId.put(lb, lightid);
                    lightById.put(lightid, lb);
                }
                
                // check this light against all the opaque, calculate a coeficient
                // based on distance
                for(int j=0, mj=queue.size(); j<mj; j++) {
                    Renderable or=queue.get(j);
                    if(or.getMaterial() == null || or.getMaterial().getLightCombineMode() == LightState.OFF )
                        continue;
                    float value = ls.getValueFor(lb, or);//or.getWorldBound(frameid));
                    // if the batch isnt lit by this light, skip
                    if(value==0)
                        continue;
                    LightBatch[] batchLights = or.lights[frameid];
                    float[] batchLightPriority = or.lightpriority[frameid];
                    boolean found = false;
                    
                    for(int k=0; k<Renderable.LOWPROFILE_LIGHTS && !found; k++) {
                        if(value>batchLightPriority[k]) {
                            // we have a light with bigger priority
                            // shift the lower priority lights
                            if(k<Renderable.LOWPROFILE_LIGHTS-1 && batchLightPriority[k]!=0) {
                                System.arraycopy(batchLights, k, batchLights, k+1, Renderable.LOWPROFILE_LIGHTS-k-1);
                                System.arraycopy(batchLightPriority, k, batchLightPriority, k+1, Renderable.LOWPROFILE_LIGHTS-k-1);
                            }
                            batchLights[k] = lb;
                            batchLightPriority[k] = value;
                            found = true;
                        }
                    }
                }
            }
        }        

        // collect all lights that finished as the 2 most influential lights for
        // a batch
        for(int j=0, mj=queue.size(); j<mj; j++) {
            Renderable or=queue.get(j);
            if(or.getMaterial() == null )
                continue;
            //LightBatch lb1 = or.lights[frameid][0];
            //LightBatch lb2 = or.lights[frameid][1];
            if(or.lights[frameid][0]==null)
                continue;
            
            // sort the lights by id
            Arrays.fill(sortedBatchLightIds, null);
            Arrays.fill(sortedBatchLights, null);
            
            int k;
            
            for(k=0; k<Renderable.LOWPROFILE_LIGHTS && or.lights[frameid][k]!=null; k++) {
                LightBatch lb = or.lights[frameid][k];
                // get id for it
                sortedBatchLightIds[k] = lightId.get(lb);
            }
            
            Arrays.sort(sortedBatchLightIds,0,k-1);
            
            for(int l=0; l<k; l++) {
                sortedBatchLights[l] = lightById.get(sortedBatchLightIds[l]).getLight();
            }
            // go over and find the lightstate that has these lights
            // get the list by the least important light
            
            Light l1 = sortedBatchLights[k-1];
            
            FastList<LightState> passlist = null;
            LightState pass = null;
            
            // find the proper list set
            HashMap<Light,FastList<LightState>> lOccur = lightOccurences.get(listno);
            if(lOccur!=null) {
                // find the proper lightstate
                // try to find entry with just the one light
                passlist = lOccur.get(l1);
                if(passlist!=null) {
                    for(int i=0; i<passlist.size()&&pass==null; i++) {
                        LightState lsl = passlist.get(i);
                        if(lsl.getQuantity()==k) {
                            boolean fail = false;
                            for(int l=0; l<k && !fail; l++) {
                                if(lsl.get(l) != sortedBatchLights[l])
                                    fail = true;
                            }
                            // the list of lights should match that in the sorted list
                            if(!fail) {
                               // this is the pass
                                pass = passlist.get(i);
                            }
                        }
                    }
                }
            }
            // if no pass is found, create it
            if(pass==null) {
                // allocate new
                pass = (LightState) f.getApp().display.getRenderer().createState(RenderState.RS_LIGHT);
                pass.setEnabled(true);

                // put into proper map
                if(lOccur==null) {
                    lOccur = new HashMap<Light,FastList<LightState>>();
                    lightOccurences.set(listno, lOccur);
                }

                // atttach all the lights in the order
                for(int l=0; l<k; l++) {
                    pass.attach(sortedBatchLights[l]);
                    passlist = lOccur.get(sortedBatchLights[l]);
                    if(passlist==null) {
                        passlist = new FastList<LightState>();
                        lOccur.put(sortedBatchLights[l], passlist);
                    }
                    passlist.add(pass);
                }

                pass.setEnabled(true);
                // TODO: get global ambient from area map
                pass.setGlobalAmbient(ColorRGBA.darkGray);
                pass.setTwoSidedLighting(false);
                // register it
                allLightPasses.add(pass);
            }
            /*
            if(!usedLightPasses.contains(pass)) {
                usedLightPasses.add(pass);
            }
             */
            
            if(or.getMaterial() != null 
                    //&& or.getMaterial().getRenderState(RenderState.RS_LIGHT) == null
                    )
                or.getMaterial().setRenderState(pass);
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
        // free up all the sorting queues
        /*
        queuPool.addAll(this.usedPool);
        usedPool.clear();
         */
        //usedLightPasses.clear();
    }

    @Override
    public void cleanup() {
        // TODO: throw out data from list allLightPasses

    }

    @Override
    public void afterRender(RenderContext ctx) {
        
    }

}
