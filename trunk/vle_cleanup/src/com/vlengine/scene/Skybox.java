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

package com.vlengine.scene;

import com.vlengine.bounding.BoundingBox;
import com.vlengine.image.Texture;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.TextureState;
import com.vlengine.system.VleException;

/**
 * A Box made of textured quads that simulate having a sky, horizon and so forth
 * around your scene. Either attach to a camera node or update on each frame to
 * set this skybox at the camera's position.
 * 
 * @author David Bitkowski
 * @author Jack Lindamood (javadoc only)
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Skybox extends SetNode {
    

    /** The +Z side of the skybox. */
    public final static int NORTH = 0;

    /** The -Z side of the skybox. */
    public final static int SOUTH = 1;

    /** The -X side of the skybox. */
    public final static int EAST = 2;

    /** The +X side of the skybox. */
    public final static int WEST = 3;

    /** The +Y side of the skybox. */
    public final static int UP = 4;

    /** The -Y side of the skybox. */
    public final static int DOWN = 5;

    private float xExtent;

    private float yExtent;

    private float zExtent;

    //private TriBatch[] skyboxQuads;

    
    public Skybox() {
        super("SkyBox");
    }
    
    /**
     * Creates a new skybox. The size of the skybox and name is specified here.
     * By default, no textures are set.
     * 
     * @param name
     *            The name of the skybox.
     * @param xExtent
     *            The x size of the skybox in both directions from the center.
     * @param yExtent
     *            The y size of the skybox in both directions from the center.
     * @param zExtent
     *            The z size of the skybox in both directions from the center.
     */
    public Skybox(String name, float xExtent, float yExtent, float zExtent) {
        super(name);

        this.xExtent = xExtent;
        this.yExtent = yExtent;
        this.zExtent = zExtent;

        initialize();
    }
    
    /**
     * Set the texture to be displayed on the given side of the skybox. Replaces
     * any existing texture on that side.
     * 
     * @param direction
     *            One of Skybox.NORTH, Skybox.SOUTH, and so on...
     * @param texture
     *            The texture for that side to assume.
     */
    public void setTexture(int direction, Texture texture) {
        if (direction < 0 || direction > 5) {
            throw new VleException("Direction " + direction
                    + " is not a valid side for the skybox");
        }

        setTexture(direction, texture, 0);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Only
     * replaces the texture at the index specified by textureUnit.
     * 
     * @param direction
     *            One of Skybox.NORTH, Skybox.SOUTH, and so on...
     * @param texture
     *            The texture for that side to assume.
     * @param textureUnit
     *            The texture unite of the given side's TextureState the texture
     *            will assume.
     */
    public void setTexture(int direction, Texture texture, int textureUnit) {
        // Validate
        if (direction < 0 || direction > 5) {
            throw new VleException("Direction " + direction
                    + " is not a valid side for the skybox");
        }

        Material mat = ((Mesh)children.get(direction)).getBatch().getMaterial();
        if(mat==null) {
            mat = new Material();
            ((Mesh)children.get(direction)).getBatch().setMaterial(mat);
        }
        TextureState ts = (TextureState)mat.getRenderState(RenderState.RS_TEXTURE);
        if (ts == null) {
            ts = new TextureState();
        }

        // Initialize the texture state
        ts.setTexture(texture, textureUnit);
        ts.setEnabled(true);

        // Set the texture to the quad
        mat.setRenderState(ts);

        return;
    }
    
    public Texture getTexture(int direction) {
        if (direction < 0 || direction > 5  ) {
            return null;
        }
        return ((TextureState)((Mesh)children.get(direction)).getBatch().getMaterial().getRenderState(RenderState.RS_TEXTURE)).getTexture();
    }

    private void initialize() {

        // Create each of the quads
        Mesh m;
        m=createSide(xExtent * 2, yExtent * 2);
        
        m.getLocalRotation().fromAngles(new float[] { 0,
                (float) Math.toRadians(180), 0 });
        m.getLocalTranslation().set(0, 0, zExtent);
        this.attachChild(m);
        
        m=createSide(xExtent * 2, yExtent * 2);
        m.getLocalTranslation().set(0, 0, -zExtent);
        this.attachChild(m);
        
        m=createSide(zExtent * 2, yExtent * 2);
        m.getLocalRotation().fromAngles(new float[] { 0,(float) Math.toRadians(90), 0 });
        m.getLocalTranslation().set(-xExtent, 0, 0);
        this.attachChild(m);

        m=createSide(zExtent * 2, yExtent * 2);
        m.getLocalRotation().fromAngles(new float[] { 0,
                (float) Math.toRadians(270), 0 });
        m.getLocalTranslation().set(xExtent, 0, 0);
        this.attachChild(m);
        
        m=createSide(xExtent * 2, zExtent * 2);
        m.getLocalRotation().fromAngles(new float[] {
                (float) Math.toRadians(90), (float) Math.toRadians(270), 0 });
        m.getLocalTranslation().set(0, yExtent, 0);
        this.attachChild(m);

        m=createSide(xExtent * 2, zExtent * 2);
        m.getLocalRotation().fromAngles(new float[] {
                (float) Math.toRadians(270), (float) Math.toRadians(270), 0 });
        m.getLocalTranslation().set(0, -yExtent, 0);
        this.attachChild(m);
       
        // We don't want it making our skybox disapear, so force view
        setCullMode(SceneElement.CullMode.NEVER);

        TriBatch t;
        Material mat;
        for (int i = 0; i < 6; i++) {
            m = (Mesh) this.getChild(i);
            t = m.getBatch();
            mat = new Material();
            
            // Make sure no lighting on the skybox
            mat.setLightCombineMode(LightState.OFF);

            // Make sure the quad is viewable
            t.setCullMode(SceneElement.CullMode.NEVER);

            t.setRenderQueueMode(RenderQueue.QueueFilter.BackGround.value);
            t.getModel().setDisplayListMode(BaseGeometry.LIST_YES);

        }
    }

    protected Mesh createSide(float width, float height) {
        Quad q = new Quad( width, height);
        // Set a bounding volume
        q.setModelBound(new BoundingBox());
        q.updateModelBound();
        TriBatch t=new TriBatch();
        t.setModel(q);
        Mesh m=new Mesh("north");
        m.setBatch(t);
        return m;
    }

    // cheat, dont update rotation from parent
    @Override
    protected void updateWorldRotation() {
        worldRotation.set(localRotation);
    }
}