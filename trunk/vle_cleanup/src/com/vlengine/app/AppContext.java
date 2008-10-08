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

package com.vlengine.app;

import com.vlengine.app.frame.Frame;
import com.vlengine.app.state.GameStateNode;
import com.vlengine.app.state.RenderPath;
import com.vlengine.app.state.ThreadTaskManager;
import com.vlengine.audio.AudioSystem;
import com.vlengine.input.InputHandler;
import com.vlengine.input.InputSystem;
import com.vlengine.input.KeyInput;
import com.vlengine.input.MouseInput;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ResourceFinder;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.Node;
import com.vlengine.scene.Text;
import com.vlengine.scene.control.UpdateContext;
import com.vlengine.system.DisplaySystem;
import com.vlengine.system.PropertiesIO;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.Timer;
import java.util.HashMap;


/**
 * This class serves as the main collector of 
 * different parts of the engine. AppContext is accessible
 * to every thread trough its local context.
 * 
 * @author vear (Arpad Vekas)
 */
public class AppContext {

    // the configuration parameters of this application
    public Config conf;
    
    public PropertiesIO properties;

    public DisplaySystem display;
    
    public InputHandler input;
    
    public InputSystem inputSystem;
    
    public Timer timer;
    
    public ViewCamera cam;
    public CameraNode camn;
    
    public Node rootNode;
    
    public Node fpsNode;
    
    public Text fps;
    public Text stats;

    //public LightState lightState;
    
    //public RenderStateEffect lightEffect;
    
    public boolean pause;
   
    public Frame[] frame;
    
    protected GameStateNode gameState;
    
    // the renderpath controlling renderpass setup
    public RenderPath renderPath;
    
    protected boolean paused = false;
    
    protected boolean stepupdate = false;
    
    public boolean showDepth = false;

    public boolean showBounds = false;

    /**
     * True if the rnederer should display normals.
     */
    public boolean showNormals = false;

    protected boolean showFps = true;
    
    protected boolean showWire = false;
    
    /** Flag for running the system. */
    public boolean finished;

    protected ResourceFinder rf;

    // the first renderpass id usable by the user
    protected int nextUserPassId = RenderPass.StandardPass.User.passId;
    // the first queue id usable by the user
    protected int nextUserQueueId = RenderQueue.StandardQueue.User.queuId;

    // null contexts usable during preload startup
    public UpdateContext nullUpdate = null;
    public RenderContext nullRender = null;
    
    // should the main camera be parallel projection?
    public boolean cameraParallel = false;
    
    // should the input be updated each frame?
    public boolean updateInput = true;
    // the input system handlers
    public KeyInput keyInput;
    public MouseInput mouseInput;
    
    public boolean enableLight = true;
    
    
    // managing shader uniforms and attributes
    
    // the map of uniform and attribute indices
    // these are filled at first use of the attribute or uniform
    // the ShaderObjectState uses these indices to store the shader in's in its map
    protected int lastVariableIndex = 0;
    protected HashMap<String,Integer> variableIndexMap = new HashMap<String,Integer>();
    
    
    public MainGame mainGame;

    public boolean doTakeScreenShot = false;
    
    public AudioSystem audio;
    
    public ThreadTaskManager glQueue;
    
    public AppContext() {}
    
    public void setRootGameState(GameStateNode state) {
        gameState = state;
    }
    
    public GameStateNode getGameStates() {
        return gameState;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setStepUpdate(boolean stepupdate) {
        this.stepupdate = stepupdate;
    }
    
    public boolean isStepUpdate() {
        return stepupdate;
    }
    
    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }
    
    public Node getRootNode() {
        return rootNode;
    }
    
    public void setDebugNode(Node debugNode) {
        this.fpsNode = debugNode;
    }
    
    public Node getDebugNode() {
        return fpsNode;
    }
    
    public ViewCamera getCamera() {
        return cam;
    }
    
    public void setCamera(ViewCamera cam) {
        this.cam = cam;
    }
    
    public void setTimer(Timer t) {
        this.timer = t;
    }
    
    public Timer getTimer() {
        return timer;
    }
    
    public boolean isShowFps() {
        return showFps;
    }
    
    public void setShowFps(boolean show) {
        this.showFps = show;
    }
        
    public void setShowWireframe(boolean wire ) {
        this.showWire = wire;
    }
    
    public boolean getShowWireframe() {
        return this.showWire;
    }
    
    public Frame[] getFrameArray() {
        return frame;
    }
    
    public void setResourceFinder(ResourceFinder rf) {
        this.rf = rf;
    }
    
    public ResourceFinder getResourceFinder() {
        return rf;
    }
    
    public int genPassId() {
        return nextUserPassId++;
    }

    public int genQueId() {
        return nextUserQueueId++;
    }

    public int getVariableIndex(String uniformName) {
        return this.variableIndexMap.get(uniformName);
    }
    
    public int ensureVariableIndex(String variableName) {
        Integer idx = variableIndexMap.get(variableName);
        if(idx==null) {
            idx = new Integer(lastVariableIndex);
            lastVariableIndex++;
            variableIndexMap.put(variableName, idx);
        }
        return idx.intValue();
    }
    
    public boolean isMultithreaded() {
        return LocalContext.isUseMultithreading() && !conf.nomultithread;
    }
}
