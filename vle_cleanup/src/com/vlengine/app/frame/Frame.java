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

package com.vlengine.app.frame;

import com.vlengine.app.AppContext;
import com.vlengine.app.Config;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.pass.PassManager;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.FrameBuffer;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.scene.Node;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.WireframeState;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.VleException;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for constructing and rendering a single frame
 * after it finishes, it can be reused by calling the clear() method.
 * 
 * @author vear (Arpad Vekas)
 */
public class Frame implements Runnable {
    
    // maximum number of frames processed in parallel
    public static final int MAX_FRAMES = 2;
    
    // frame cannot be processed, it is not yet ready
    public static final int FRAME_INCOMPLETE = 0;
    
    public static final int FRAME_STARTING = 1;
    
    // scene update is in process (including physics)
    // can only be called if the other thread is gone past FRAME_MATERIAL
    public static final int FRAME_UPDATE = 2;
    
    // the culling is in process
    public static final int FRAME_CULL = 3;
    
    // update has finished, and is ready to be drawn
    public static final int FRAME_READY = 4;
    
    // the queues, materials, renderstates are being updated and prepared for rendering
    // the render method can be called, but only if the
    // previous frame has finished rendering
    public static final int FRAME_MATERIAL = 5;
    
    // rendering is in process
    public static final int FRAME_RENDERING = 6;
    
    // frame is finished, it is ready to be cleared
    // and start another frame
    // other frame can be rendered
    public static final int FRAME_FINISHED = 7;
    
    // this frame has exited, and cannot be used again
    // this happened due to exception, or general program exit
    public static final int FRAME_ENDED = 8;
    
    // not changing data
    
    // the id of this frame generator, used when different copies of data
    // are required
    private int frameId = 0;
    
    // the synchronizing object, needs to
    // set the same object on each frame used
    //private FrameCounter synch;
       
    // the top Application context
    private AppContext app;
    
    // the root node to render
    protected Node rootNode;
    
    // the node used for debug info
    protected Node debugNode;
    
    // the counter of processed frame
    //private long frameCount = 0;
    // the time since last frame update
    private float time;
    
    // the state of this frame,
    private int state = Frame.FRAME_INCOMPLETE;
    
    // the renderable queues
    private RenderQueue queues;
    
    // the pass manager
    private PassManager passManager = new PassManager();

    // the main camera
    private ViewCamera camera;
    
    protected Timer timer;
    
    // wireframe state for enforcing wireframe rendering
    private WireframeState wireState;  
    
    // the list of usable culler threads
    protected FastList<Thread> freethread = new FastList<Thread>();
    protected FastList<CullContext> freecontext = new FastList<CullContext>();
    
    // the list of used culler threads
    protected FastList<CullContext> usedcontext = new FastList<CullContext>();
    
    // the list of different cameras in the passes yet to culled agains
    protected FastList<ViewCamera> cameras = new FastList<ViewCamera>();
    // the cameras the scene has already been culled agains
    protected FastList<ViewCamera> processedCameras = new FastList<ViewCamera>();
    
    // scene context for updating in this thread
    protected UpdateContext uctx = new UpdateContext();
    
    //scene context for culling in this thread
    protected CullContext sctx = new CullContext();
    
    // rendercontext for rendering
    protected RenderContext rctx = new RenderContext();
    
    // the list of batches which need to be prepared
    protected FastList<Renderable> preparable = new FastList<Renderable>();

    
    protected StringBuffer tempBuffer = new StringBuffer();
    
    // has the OGL context set for this thread?
    protected boolean contextSet = false;
    
    protected FastList<FrameBuffer> drityFrames = new FastList<FrameBuffer>();

    public Frame(int id, AppContext appCtx) {
        frameId = id;
        //this.synch = synch;
        this.app = appCtx;
        queues = new RenderQueue(frameId);
        if( !app.isMultithreaded()) {
            app.conf.maxCullThreads = 1;
        }
    }
    
    public int getFrameId() {
        return frameId;
    }
        
    public ViewCamera getCamera() {
        return camera;
    }
    
    /**
     * Returns the main application configuration.
     * @return  the class of the Config type associated with the application
     */
    public Config getConfig() {
        return app.conf;
    }

    public AppContext getApp() {
        return app;
    }
    
    /**
     * This is used to display print text.
     */
    protected StringBuffer updateBuffer = new StringBuffer( 30 );

    /**
     * Calls the renderpath, to create the renderpasses which will be used
     * to render the current frame.
     */
    protected void createDefaultPasses() {
        // create passes
        if(app.renderPath!=null)
            app.renderPath.createDefaultPasses(this);
        
    }
    
    public PassManager getPasses() {
        return passManager;
    }
    
    public RenderQueue getQueueManager() {
        return queues;
    }
    
    public UpdateContext getUpdateContext() {
        return uctx;
    }
    
    public RenderContext getRenderContext() {
        return rctx;
    }
    
    public Timer getTimer() {
        return timer;
    }
    
    public CullContext cullNode( CullContext parent, Node node, ViewCamera vcam ) {
        CullContext child = null;
        // pre-check if there is a free thread
        // so if the pre-check fails, we dont synchronize
        if(!freecontext.isEmpty() ||
              usedcontext.size() < app.conf.maxCullThreads -1 ) {
            synchronized( freecontext ) {
                if( parent != null && ! freethread.isEmpty() ) {
                    int idx = freecontext.size() - 1;
                    child = freecontext.get(idx);
                    freecontext.remove(idx);
                    child.setState(parent, node);
                    usedcontext.add(child);
                } else if(usedcontext.size() < app.conf.maxCullThreads ) {
                    // we can create a new thread
                    child = new CullContext();
                    if( parent == null )
                        child.setState(this, node, vcam);
                    else
                        child.setState(parent, node);
                    usedcontext.add(child);
                    if( app.conf.maxCullThreads > 1 ) {
                        Thread t = new Thread(child);
                        child.setThread(t);
                    }
                }
            }
        }
        return child;
    }
    
    public void merge( CullContext ctx ) {
        // used by threads to pass prepared data
        synchronized( passManager ) {
            // extract data from the context
            // merge passes
            passManager.merge(ctx.getPassManager());
            // merge queues
            RenderQueue q=ctx.getQueueManager();
            synchronized( q ) {
                queues.merge( q );
                q.clear();
            }
            // merge the list of batches needing preparing
            preparable.addAll(ctx.getPrepareList());
        }
    }
    
    // a context reports its finished through this method
    public void workFinished( CullContext ctx ) {
        // used by threads to signal that they are finished
        synchronized( freecontext ) {
            int ci = usedcontext.indexOf(ctx);
            if( ci == -1) {
                throw new VleException("thread reporting workFinished not found in threads list");
            } else {
                usedcontext.remove(ci);
                freecontext.add(ctx);
            }
        }
    }
    
    // clears all the previous data for the frame and starts a new frame
    public void startFrame() {

        synchronized (app) {
            // get the new frame number
            //frameCount = synch.createFrameCount();
            // get all the data from synchronizer
            rootNode = app.getRootNode();
            debugNode=app.getDebugNode();
            camera=app.getCamera();
            timer=app.getTimer();
        }
        // clear all the culling contexts
        freecontext.addAll(usedcontext);
        usedcontext.clear();
        // clear all the passes
        passManager.clear();
        // create the default passes
        createDefaultPasses();
        
        // clear the list of cameras used during culling
        cameras.clear();
        processedCameras.clear();
        
        rctx.clear();
        rctx.frameId = this.frameId;
        
        //rctx.setRenderer(renderer);
        //rctx.setDefaultStates();
        
        sctx.setThreadContext();
        sctx.setState(this, null, this.camera);

        preparable.clear();
    }
    
    // update scene state, should be enterely runable in 
    // a separate thread(s)
    public void update() {
        timer.update();
        
        uctx.frame = this;
        time = timer.getTimePerFrame();
        uctx.time = time;
        
        if( app.isPaused() && !app.isStepUpdate() )
            return;
        app.setStepUpdate(false);
        
        // call preUpdate gamestates
        app.getGameStates().preUpdate(uctx);
        
        /** Update controllers/render states/transforms/bounds for rootNode. */
        rootNode.updateGeometricState(uctx, true);
    }
    
    // culls the screen with cameras from passes
    public void cullscene() {
        // clear the list of cameras
        cameras.clear();
        processedCameras.clear();

        // call the GameStates
        app.getGameStates().preCull(this);
        // get all the different cameras in the passes
        FastList<RenderPass> passes = this.passManager.getPasses();
        for(int i=0, mx=passes.size(); i<mx; i++) {
            RenderPass p = passes.get(i);
            ViewCamera c = p.getCamera();
            if(c==null)
                c=this.camera;
            if(!cameras.contains(c)) {
                cameras.add(c);
            }
        }

        if( app.conf.maxCullThreads < 2 ) {
            // go over every camera and cull separately
            // note: we need to check against cameras.size() because
            // new passes might have been introduced
            while( !cameras.isEmpty() ) {
                // remove camera from to be processed and add it to
                // the already processed list
                int i = cameras.size()-1;
                ViewCamera vc = cameras.get(i);
                processedCameras.add(vc);
                cameras.remove(i);
                
                sctx.setState(this, rootNode, vc);
                // call preCull states
                app.getGameStates().preCull(sctx);

                // single threaded route
                if(rootNode.docull(sctx)) 
                        rootNode.queue(sctx);
                merge(sctx);
                
                // after merge completes, call postcull
                app.getGameStates().postCull(sctx);
            }
        } else {
            // TODO: multithreaded route
            
        }
    }
    
    /**
     * If culling of the scene is already going on, and we want to introduce
     * new passese and new cameras against which to cull the scene, the new camera
     * should be added with this method. This method has no effect outside
     * the culling phase. Before culling passes can be added, 
     * the Frame will take care to cull the scene against each camera. If the Frame
     * already knows about this camera, it will ignore this request (it will not cull
     * two times against the same camera).
     * @param vcam
     */
    public void addCameraToCull(ViewCamera vcam) {
        if(vcam==null) return;
        if(!cameras.contains(vcam)
         && !processedCameras.contains(vcam)) {
            cameras.add(vcam);
        }
    }
    
    /**
     * This method can be called during culling, to check
     * if there are still cameras to cull the scene against.
     * This is useful, if we want to do something in a gamestate
     * preCull or postCull events when processing the last camera (eg, 
     * add some new passes)
     * @return
     */
    public boolean hasCameraToCull() {
        return !cameras.isEmpty();
    }
    
    protected void activateFb(FrameBuffer fb) {
        if(rctx.fb!=null) {
            // deactivate old
            rctx.fb.deactivate(rctx);
            rctx.fb = null;
        }
        boolean clear = false;
        if(!drityFrames.contains(fb)) {
            drityFrames.add(fb);
            clear = true;
        }
        // activate the new
        fb.activate(rctx, clear);
        rctx.fb = fb;
    }
    
    // render method
    public boolean render() {
        

            rctx.clear();
            rctx.frame = this;
            rctx.app = this.app;
            rctx.frameId = this.frameId;
            rctx.time = time;
            rctx.setRenderer(app.display.getRenderer());

            //rctx.setRenderer(renderer);
            // acuire the main frambuffer from the renderpath
            FrameBuffer mainFb = app.renderPath.getRootFrameBuffer();
            //rctx.fb = mainFb;
            
            rctx.setDefaultStates();
            rctx.setRenderQueue(queues);

            if (!rctx.getRenderer().lockRenderer(rctx)) {
                return false;
            }

            state = FRAME_MATERIAL;
            // after entering this stage, no change is allowed to the scene

            mainFb.activate(rctx);
            rctx.fb = mainFb;
            drityFrames.clear();
            drityFrames.add(mainFb);

            try {

                /** Reset display's tracking information for number of triangles/vertexes */
                rctx.getRenderer().clearStatistics();

                // call preMaterial states
                app.getGameStates().preMaterial(rctx);
                
                // update materials and renderstates
                for (int i = 0, mx = preparable.size(); i < mx; i++) {
                    Renderable r = preparable.get(i);
                    // mark that it needs update in the next frame also
                    r.setNeedUpdate(true);

                    r.prepare(rctx);
                }

                // pass uses no material, apply the default one
                state = FRAME_RENDERING;

                // call preRender states
                app.getGameStates().preRender(rctx);
                
                /** Clears the previously rendered information. */
                rctx.getRenderer().clearBuffers();

                // is rendering overriden to wireframe?
                if (app.getShowWireframe()) {
                    // enforce wireframe rendering
                    if (wireState == null) {
                        wireState = (WireframeState) rctx.getRenderer().createState(RenderState.RS_WIREFRAME);
                    }
                    wireState.setEnabled(true);
                    rctx.enforcedStateList[RenderState.RS_WIREFRAME] = wireState;
                }

                // get the sorted passes
                FastList<RenderPass> pss = passManager.getSortedPasses();
                boolean switchfb = true;
                ViewCamera currCamera = null;

                for (int i = 0, pc = pss.size(); i < pc; i++) {
                    RenderPass p = pss.get(i);
                    if(!p.isEnabled())
                        continue;
                    FrameBuffer fb = p.getTarget();
                    if(fb == null)
                        fb = mainFb;
                    if(fb!=rctx.fb) {
                        // if we switch render target
                        switchfb = true;
                        activateFb(fb);
                    } else {
                        switchfb = false;
                    }

                    currCamera = p.getCamera();
                    if(currCamera==null)
                        currCamera = this.camera;
                    // set the camera from the pass
                    if (currCamera != rctx.currentCamera 
                            || switchfb) {
                        rctx.getRenderer().setCamera(currCamera);
                        rctx.currentCamera = currCamera;
                    }

                    // pass uses no material, apply the default one
                    if (p.getUsedMaterialFlags() == -1) {
                        rctx.defaultMaterial.apply(rctx);
                    }
                    p.renderPass(rctx);
                }

                // switch to main renderer and camera
                if(mainFb!=rctx.fb) {
                    activateFb(mainFb);
                    switchfb = true;
                } else {
                    switchfb = false;
                }

                // set the camera from the pass
                if (camera != rctx.currentCamera 
                        || switchfb) {
                    rctx.getRenderer().setCamera(camera);
                    rctx.currentCamera = currCamera;
                }

                // go back to the main framebuffer
                //app.renderPath.aquireRootFrameBuffer(rctx);
                
                // clear all the enforced states
                for(int i=0;i<RenderState.RS_MAX_STATE; i++)
                    rctx.enforcedStateList[i] = null;
                
                // render debug node
                if (app.isShowFps()) {

                    
                    // update FPS counter
                    updateBuffer.setLength( 0 );
                    updateBuffer.append( "FPS: " );
                    int fr = (int) app.timer.getFrameRate();
                    if(fr<1000)
                        updateBuffer.append( "0" );
                    if(fr<100)
                        updateBuffer.append( "0" );
                    if(fr<10)
                        updateBuffer.append( "0" );
                    updateBuffer.append( fr );
                    app.fps.print( updateBuffer );
                    float fpx = DisplaySystem.getDisplaySystem().getWidth();
                    float fpy = DisplaySystem.getDisplaySystem().getHeight();
                    app.fps.getLocalTranslation().set(0, fpy-20,0);
                    app.fps.getWorldTranslation().set(0, fpy-20,0);
                    app.fps.getLocalScale().set(0.8f, 0.8f, 0.8f);
                    app.fps.getWorldScale().set(0.8f, 0.8f, 0.8f);
                    app.fps.updateGeometricState(getUpdateContext(), true);


                    updateBuffer.setLength( 0 );
                    updateBuffer.append( app.display.getRenderer().getStatistics( tempBuffer ) );
                    /** Send the fps to our fps bar at the bottom. */
                    app.stats.print( updateBuffer );
                
                    // render debug node with main camera
                    sctx.setState(this, debugNode, this.camera);
                    rctx.getRenderer().drawDirect(debugNode);
                }

                // run postRender states
                app.getGameStates().postRender(rctx);
                
                // swap buffers
                rctx.getRenderer().displayBackBuffer();
                
                // if needed, take screenshot
                if(app.doTakeScreenShot) {
                    rctx.getRenderer().takeScreenShot( "GameScreenShot" );
                    app.doTakeScreenShot = false;
                }

                // unlock the renderer
                mainFb.deactivate(rctx);

                // mark the frames dirty
                for(int i=0; i<drityFrames.size(); i++) {
                    drityFrames.get(i).markDirty();
                }
                drityFrames.clear();

                rctx.getRenderer().unlockRenderer();

            } catch (Exception ex) {
                Logger.getLogger(Frame.class.getName()).log(Level.SEVERE, "Render exception", ex);
                state = FRAME_ENDED;
            }
            return true;
    }
    
    public void endFrame() {
        LocalContext.getContext().scene = null;
        this.state = FRAME_FINISHED;
    }
    
    public int getState() {
        return state;
    }
    
    public void setState(int state) {
        this.state = state;
        if(state == FRAME_STARTING) {
            synchronized(this) {
                this.notifyAll();
            }
        }
    }

    protected void waitRestart() {
        while(state != FRAME_STARTING && !app.finished && !app.display.isClosing()) {
            synchronized(this) {
                try {
                    this.wait(10);
                } catch (InterruptedException ex) {
                    
                }
            }
        }
    }

    // process this frame, this is the main method
    public void run() {
        
        if(!contextSet) {
            if(app.isMultithreaded()) {
                // set the opengl context to this thread
                try {
                    
                    //Display.makeCurrent();
                    //GLContext.useContext(appCtx.display.getOGLContext());
                } catch(Exception e) {

                }
            }
            contextSet = true;
        }
        
        while (!app.finished && !app.display.isClosing() ) {
            
            // wait until we are in incomplete state again
            waitRestart();
            
            if(app.finished || app.display.isClosing())
                return;
            
            // be sure that we are the frame that is in order to render
            startFrame();

            //state = Frame.FRAME_STARTING;
            // handle input events prior to updating the scene
            // - some applications may want to put this into update of
            // the game state
            if(app.updateInput)
                app.inputSystem.update();

            // call pre-frame event in gamestates
            app.getGameStates().preFrame(app);

            // update game state, do not use interpolation parameter
            // TODO: convert to multiple frames
            update();


            app.mainGame.simpleUpdateFromFrame(this);

            // TODO: convert to multiple frames
            cullscene();

            // sort the collected queues
            this.queues.sortAll();
            
            //if(app.frame[0]==this) {
                boolean rendered = false;
                while(!rendered) {
                    rendered=render();
                    if(!rendered) {
                        Thread.yield();
                    }
                }
            //}
            
            if(state==FRAME_ENDED) {
                // error, exit
                return;
            }

            endFrame();
            
            if(!app.isMultithreaded()) {
                return;
            }
        }
        
        state = FRAME_ENDED;
    }

}
