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

package com.vlengine.test;

import com.vlengine.app.AppContext;
import com.vlengine.app.MainGame;
import com.vlengine.app.frame.Frame;
import com.vlengine.image.Texture;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.OpaquePass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test006ShaderTexture extends MainGame {

    @Override
    protected void simpleInitGame(AppContext app) {
        
        // create quad geometry
        Quad q = new Quad(100,100);
        q.setVBOMode(BaseGeometry.VBO_LONGLIVED);
        q.createVBOInfos();
        
        // create VBO only once, we will not change the quad geometry
        
        
        // create shader
        ShaderObjectsState shader = new ShaderObjectsState();
        shader.load(
                    // vertex shader
                    "varying vec2 Texcoord;\n" +
                    "void main( void ) {\n" +
                    "Texcoord    = gl_MultiTexCoord0.xy;\n" +
                    "gl_Position = ftransform();\n" +
                    "}\n"
                    , 
                    // fragment shader
                "uniform sampler2D baseMap;\n" +
                "varying vec2 Texcoord;\n" +
                "void main( void ) {\n" +
                "gl_FragColor = texture2D( baseMap, Texcoord );\n" +
                "}\n"
                );
        shader.setEnabled(true);
        
        // load texture
        /*
        Texture tex = TextureManager.loadTexture(Test005Texture.class.getClassLoader()
                            .getResource("com/vlengine/data/tangents.jpg"), Texture.MM_LINEAR,
                            Texture.FM_LINEAR, 0.0f, true);
         */
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);
        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create parameters state
        ShaderParameters params = new ShaderParameters();
        // create shader texture
        ShaderTexture shtex = new ShaderTexture("baseMap", tex);
        // add it to parameters
        params.setTexture(shtex);
        params.setEnabled(true);
        
        // crate material
        Material mat = new Material();
        mat.setRenderState(shader);
        mat.setRenderState(params);
        
        // create the renderable batch
        TriBatch t = new TriBatch();
        t.setModel(q);
        t.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
        t.setCullMode(Spatial.CullMode.NEVER);
        
        // set material to batch as opaque material
        t.setMaterial(mat);
        
        // creeate mesh
        LodMesh m = new LodMesh("Quad");
        // add batch as default lod
        m.addBatch(0, t);
        m.getLocalTranslation().set(0,0,-100);
        app.rootNode.attachChild(m);
        
    }

    public static void main(String[] args) {
        Test006ShaderTexture test = new Test006ShaderTexture();
        test.start();
    }

    @Override
    protected void simpleUpdate(Frame f) {
        f.getPasses().getPassById(RenderPass.StandardPass.Depth.passId).setEnabled(false);
        ((OpaquePass)f.getPasses().getPassById(RenderPass.StandardPass.Opaque.passId)).setZbufferWrite(true);
    }
}
