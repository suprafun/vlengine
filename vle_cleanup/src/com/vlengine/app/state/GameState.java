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

package com.vlengine.app.state;

import com.vlengine.app.AppContext;
import com.vlengine.app.frame.Frame;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.control.UpdateContext;

/**
 * A GameState is used to encapsulate a certain state of a game, e.g. "ingame" or
 * "main menu".
 * <p>
 * A GameState can be attached to a GameStateNode, forming a tree structure 
 * similar to jME's scenegraph.
 * <p>
 * It contains two important methods: update(float) and render(float),
 * which gets called by the parent GameStateNode, e.g. the GameStateManager.
 * 
 * @see GameStateManager
 * @see GameStateNode
 * @see BasicGameState
 * @see CameraGameState
 * 
 * @author Per Thulin
 */
public abstract class GameState {
	
    /** The name of this GameState. */
    protected String name;

    /** Flags whether or not this GameState should be processed. */
    protected boolean active; 

    /** GameState's parent, or null if it has none (is the root node). */
    protected GameStateNode parent;

    /*
     * Gets called before a frame processing starts.
     * The scene rootNode can be switched at this time,
     * but the frame or renderer are unacessible.
     * Also good place for processing input.
     */
    public abstract void preFrame(AppContext app);

    /**
     * Gets called every frame before update. 
     * 
     * Notes:
     * The scene is safe to change.
     * The renderer is not safe to access.
     * Nothing is yet decided for the current frame.
     * No data should change in GameState which is used in preMaterial and later stages
     * This is because 2 threads can be runing methods of GameState at the same time.
     * preUpdate and preCull runs in update thread
     * preRender and postRender runs in render thread
     * 
     */
    public abstract void preUpdate(UpdateContext uctx);
    
    /**
     * Get called before culling starts. In this step, the GameState
     * can introduce new/delete RenderPass-es the scene to be culled agains
     * 
     * @param f The frame to be added the renderpasses
     */
    public abstract void preCull(Frame f);
    
    /**
     * Gets called before culling the scene, with each of the cullcontext. 
     * 
     * Notes:
     * ctx.frame.getCamera() is updated for this frame, if it need to be changed
     *  and the changed camera is to affect the culling, ctx.frame.getCamera().copy(ctx.getCamera());
     *  should be called.
     * ctx.getPassManager() is accessible for adding/removing renderpasses
     * ctx.getQueueManager() is accessible for adding/removing queues
     * the scene can be changed in this step
     * Renderable objects can already be added to queue-s
     * @param ctx
     */
    public abstract void preCull(CullContext cctx);
    
    /**
     * Called each time after the scene is culled for a camera.
     * Here the GameState can introduce a new pass with a new camera,
     * take care not to 
     * @param ctx
     */ 
    public abstract void postCull(CullContext cctx);
    
        /**
         * Gets called before materials are updated for the current frame.
         * Notes:
         * In this step access to the scene is safe, but changes wont affect the rendering of the current frame.
         * In this step RenderPasses can be removed if needed, and passes which use
         *   already filled RenderQueue-s can be introduced. RenderPasses which need 
         *   culling by their own wont work.
         * In this step the acess to the renderer is also safe.
         * @param ctx
         */
    public abstract void preMaterial(RenderContext rctx);
	/**
	 * Gets called before rendering starts. The framebuffer is ready for drawing.
         * Access to the scene is not safe
         * Access to the renderer is safe
	 * 
	 * @param tpf The elapsed time since last frame.
	 */
    public abstract void preRender(RenderContext rctx);
    
    /**
     * Gets called after rendering of all passes has finished. But before
     * the backbuffer is displayed.
     * Access to the scene is not safe
     * Access to the renderer is safe
     * @param ctx
     */
    public abstract void postRender(RenderContext rctx);

    /**
     * Gets called after the backbuffer is shown on screen, but the renderer
     * is still locked. It a place to put code that for example captures screen
     * content.
     * Access to the scene is not safe
     * Access to the renderer is safe
     * @param ctx
     */
    public abstract void afterRender(RenderContext rctx);

    /**
     * Gets performed when cleanup is called on a parent GameStateNode (e.g.
     * the GameStateManager).
     */
    public abstract void cleanup();
    
    /**
     * Sets whether or not you want this GameState to be updated and rendered.
     * 
     * @param active 
     *        Whether or not you want this GameState to be updated and rendered.
     */
	public void setActive(boolean active) {
		this.active = active;
	}

    /**
     * Returns whether or not this GameState is updated and rendered.
     * 
     * @return Whether or not this GameState is updated and rendered.
     */
	public boolean isActive() {
		return active;
	}

    /**
     * Returns the name of this GameState.
     * 
     * @return The name of this GameState.
     */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of this GameState.
	 * 
	 * @param name The new name of this GameState.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Sets the parent of this node. <b>The user should never touch this method,
	 * instead use the attachChild method of the wanted parent.</b>
	 * 
	 * @param parent The parent of this GameState.
	 */
	public void setParent(GameStateNode parent) {
		this.parent = parent;
	}
	
	/**
	 * Retrieves the parent of this GameState. If the parent is null, this is
	 * the root node.
	 * 
	 * @return The parent of this node.
	 */
	public GameStateNode getParent() {
		return parent;
	}
}


