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

package com.vlengine.renderer;

import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.app.frame.Frame;
import com.vlengine.renderer.pass.PassManager;
import com.vlengine.scene.Node;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.state.RenderState;
import com.vlengine.system.VleException;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gathers data in the cull traversal of the scene,
 * contains the active effects, passes and cameras that will
 * be used to render this part of the scene
 * 
 * Multiple SceneContext objects are used in multithreded
 * traversal of the scene, but only one thread will touch
 * one Spatial. It will cull the Spatial for all the used cameras,
 * and will put the spatial in all the proper queues.
 * 
 * @author vear
 */
public class CullContext implements Runnable {

    // is this SceneContext busy now
    protected volatile boolean busy = false;
    protected volatile boolean started = false;
    
    protected Thread thread;
    
    // the frame we are working with
    private Frame frame;
    
    private int frameId;
    
    // the parent context, null if this is the root
    protected CullContext parent = null;
       
    // the root element which needs to be culled
    protected Node rootNode;
       
    // the Renderable gathering output queues
    // after finishing a branch, will be added to parents
    // queues
    protected RenderQueue que;
    
    // the passes active now
    protected PassManager pass = new PassManager();
        
    // the filter of active passes during culling
    public long passfilter = Long.MAX_VALUE;

    // the current renderstates
    protected FastList<RenderState[]> states = new FastList<RenderState[]>(3);
    
    protected FastList<Renderable> rlist = new FastList<Renderable>();
    
    // the view camera used for reference
    protected ViewCamera vcam;
    // the camera used for culling 
    protected Camera cam = new Camera();
    
    // clear all the data from this context, and allow reuse
    public void clear() {
        frame = null;
        frameId = -1;
        parent = null;
        if(que!=null)
            que.clear();
        else
            que=new RenderQueue();
        pass.clear();
        for(int i=0; i<states.size(); i++ ) {
            RenderState[] st = states.get(i);
            Arrays.fill(st, null);
        }
        rlist.clear();
        vcam = null;
    }
    
    // returns the unique id of the frame this context is working on
    public int getFrameId() {
        return frameId;
    }
    
    public Frame getFrame() {
        return frame;
    }
    
    public ViewCamera getViewCamera() {
        return vcam;
    }
    
    public Camera getCullCamera() {
        return cam;
    }
    
    public RenderState[] getRenderStateList(int index ) {
        if( index < 0 || index >= states.size() )
            return null;
        return states.get(index);
    }
    
    public void setRenderStateList(int material, RenderState[] rsl) {
        if( material == -1 || rsl == null )
            return;
        states.ensureCapacity(material + 1);
        states.set(material, rsl);
    }
    
    public void setRenderState(int material, int rsNum, RenderState s) {
        if(material == -1 )
            return;
        states.ensureCapacity(material + 1);
        RenderState[] st = states.get(material);
        if( st == null ) {
            // create state array for material
            st = new RenderState[RenderState.RS_MAX_STATE];
            states.set(material, st);
        }
        st[ rsNum ] = s;
    }
    
    public RenderState getRenderState( int material, int rstype ) {
        if( material == -1 || states.size() >= material )
            return null;
        RenderState[] st = states.get(material);
        if(st == null)
            return null;
        return st[rstype];
    }
    
    public void setState( Frame f , Node e, ViewCamera vcam ) {
        clear();
        parent = null;
        this.frame = f;
        this.frameId = frame.getFrameId();
        this.vcam = vcam;
        vcam.copy(cam);
        
        // get all passes that have the given camera
        FastList<RenderPass> pss = f.getPasses().getPasses();
        for(int i=0, mx=pss.size(); i<mx; i++) {
            RenderPass p = pss.get(i);
            ViewCamera pc=p.getCamera();
            if(pc==null)
                pc=frame.getCamera();
            if(pc==vcam)
                pass.addPass(p);
        }
        //pass.merge(f.getPasses());
        this.rootNode = e;
        que.createQueue( frame.getQueueManager() );
        passfilter = Long.MAX_VALUE;
        
        /*
        // copy default states from parent to material 0
        RenderState[] stp = 
        for(int i=0; i<parent.states.size(); i++ ) {
            RenderState[] stp = parent.states.get(i);
            if( stp != null ) {
                RenderState[] st = states.get(i);
                if(st == null) {
                    st = new RenderState[RenderState.RS_MAX_STATE];
                    states.set(i, st);
                }
                System.arraycopy(stp, 0, st, 0, RenderState.RS_MAX_STATE);
            }
        }
         */
    }
    
    public void setState( CullContext parent, Node e ) {
        clear();
        this.parent = parent;
        this.frame = parent.frame;
        this.frameId = frame.getFrameId();
        vcam = parent.vcam;
        vcam.copy(cam);
        pass.merge(parent.pass);
        this.rootNode = e;       
        que.createQueue( parent.getQueueManager() );
        passfilter = parent.passfilter;
        
        // copy states from parent
        states.ensureCapacity(parent.states.size());
        for(int i=0; i<parent.states.size(); i++ ) {
            RenderState[] stp = parent.states.get(i);
            if( stp != null ) {
                RenderState[] st = states.get(i);
                if(st == null) {
                    st = new RenderState[RenderState.RS_MAX_STATE];
                    states.set(i, st);
                }
                System.arraycopy(stp, 0, st, 0, RenderState.RS_MAX_STATE);
            }
        }
    }

    public void setThread(Thread t) {
        thread = t;
    }
    
    public Thread getThread() {
        return thread;
    }
    
    public PassManager getPassManager() {
        return pass;
    }
    
    public int getPassQuantity() {
        return pass.getQuantity();
    }
    
    public RenderPass getPass(int index) {
        return pass.getPassByIndex(index);
    }
    
    public void removePass( RenderPass p ) {
        pass.remove(p);
    }
    
    public void addPass( RenderPass p ) {
        pass.addPass(p);
    }
    
    public void addPass( FastList<RenderPass> p ) {
        pass.addAll(p);
    }
    
    public void addToQueue(int qn, Renderable r) {
        que.add(qn, r);
    }
    
    public RenderQueue getQueueManager() {
        return que;
    }
    
    public void addToPrepareList( Renderable r ) {
        rlist.add( r );
    }

    public FastList<Renderable> getPrepareList() {
        return rlist;
    }
    
    public void start() {
        if( !started ) {
            started = true;
            thread.start();
        } else {
            this.notify();
        }
    }
    
    
    public void setThreadContext() {
        CullContext scx = LocalContext.getContext().scene;
        if( scx == null ) {
            LocalContext.getContext().scene = this;
        } else if ( scx != this ) {
            throw new VleException("SceneContext does not match thread");
        }
    }
    
    public void run() {
        busy = true;
        setThreadContext();
        while(true) {
            try {
                busy = true;
                if( rootNode != null) {
                    rootNode.docull(this);
                    // pass our data to the frame
                    frame.merge(this);
                }
                
                rootNode = null;
                // finished processing, ready for the next job
                busy = false;
                // report to frame that we are finished working
                frame.workFinished(this);
                
                // pause until another thread wakes us up
                this.wait();
            } catch (InterruptedException ex) {
                if( busy ) {
                    Logger.getLogger(CullContext.class.getName()).log(Level.SEVERE, "Thread interrupted, while it was busy", ex);
                    throw new VleException("Thread interrupted, while it was busy");
                }
            }
        }
    }
}
