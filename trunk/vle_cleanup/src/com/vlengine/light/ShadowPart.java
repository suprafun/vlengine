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

import com.vlengine.image.Texture;
import com.vlengine.math.Matrix4f;
import com.vlengine.renderer.ViewCamera;

/**
 * This class represents a part of the shadow map in the
 * divided frustum. This class holds the refernece to a
 * shadow map texture.
 * 
 * @author vear (Arpad Vekas)
 */
public class ShadowPart {
    // the camera used to create the shadow map texture
    // this is also used to check if the shadow is affecting the
    // scene, and to check if the light shadows a rendered batch
    protected ViewCamera camera;
    // the perspective matrix used by the camera
    protected Matrix4f projMatrix = new Matrix4f();
    // the view matrix used by the camera
    protected Matrix4f viewMatrix = new Matrix4f();
    // the final texture to light matrix
    protected Matrix4f lightMatrix = new Matrix4f();
    // the camera used in perspective mappi
    // the texture
    protected Texture shadowMap;
    // the dimension for the texture
    protected int dimension;
    // the parent shadow object
    protected Shadow parent;
    
    

    public ShadowPart(){
        
    }
    
    /**
     * Set the parent shadow object
     */
    public void setParent(Shadow parent) {
        this.parent = parent;
    }

    public void setCamera(ViewCamera vcam) {
        this.camera = vcam;
    }
    
    public ViewCamera getCamera() {
        return camera;
    }

    public void setTexture(Texture tex) {
        this.shadowMap = tex;
    }
    
    public Texture getTexture() {
        return shadowMap;
    }
    
    public Matrix4f getProjection() {
        return projMatrix;
    }
    
    public Matrix4f getView() {
        return viewMatrix;
    }
    
    public Matrix4f getLightMatrix() {
        return lightMatrix;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
