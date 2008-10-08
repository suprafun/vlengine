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
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.renderer.pass.DepthPass;
import com.vlengine.renderer.pass.PassManager;
import com.vlengine.renderer.pass.SSAOPass;
import com.vlengine.system.VleException;

/**
 * Broken
 * @author vear (Arpad Vekas)
 */
public class Test052SSAOView extends MainGame {

    Node lc;
    Node ln;
    LightNode lin;
    Vector3f lookcenter = new Vector3f();
    float rottime = 0;
    Vector3f boxLocation = new Vector3f(0,400,0);
    
    Texture ssaoTexture;

    @Override
    protected void simpleInitGame(AppContext app) {
        
        // create the texture that will hold the ssao
        ssaoTexture  = new Texture();

        Box b = new Box(new Vector3f(0,0,0), 50,50,50);
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch t = new TriBatch();
        t.setModel(b);
        // this is casting shadow
        long shadowCaster = RenderQueue.FILTER_OPAQUE;
        t.setRenderQueueMode(shadowCaster);
        t.setCullMode(Spatial.CULL_NEVER);
        
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txs = new TextureState();
        txs.setTexture(tex);
        txs.setEnabled(true);
        
        // crate material
        Material mat = new Material();
        mat.setRenderState(txs);
        
        // set material to batch as opaque material
        t.setMaterial(mat);
        
        Mesh m = new Mesh("Box");
        m.setBatch(t);
        m.getLocalTranslation().set(boxLocation);
        m.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(m);
        
        // create the ground quad
        Quad q = new Quad(5000,5000);
        TriBatch tq = new TriBatch();
        tq.setModel(q);
        tq.setRenderQueueMode(shadowCaster);
        tq.setCullMode(Spatial.CULL_NEVER);
        
        Texture texq = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( texq == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txsq = new TextureState();
        txsq.setTexture(tex);
        txsq.setEnabled(true);
        
        // crate material
        Material matq = new Material();
        matq.setRenderState(txsq);
        
        // set material to batch as opaque material
        tq.setMaterial(matq);
        
        Mesh mq = new Mesh("quad");
        mq.setBatch(tq);
        mq.getLocalTranslation().set(0,0,0);
        Quaternion qr = new Quaternion();
        qr.fromAngles(-FastMath.HALF_PI, 0, 0);
        mq.getLocalRotation().set(qr);
        mq.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(mq);


        Box bl = new Box(new Vector3f(0,0,0), 5,5,5);
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch tl = new TriBatch();
        tl.setModel(bl);
        // this object does not cast shadow
        tl.setRenderQueueMode(shadowCaster);
        tl.setCullMode(Spatial.CULL_NEVER);
        Mesh ml = new Mesh("lightpos");
        ml.setBatch(tl);
        ml.updateWorldVectors(app.nullUpdate);
        
        ln = new SetNode("lightnode");
        ln.attachChild(ml);
        
        
        ln.attachChild(lin);

        ln.getLocalTranslation().set(0f, 0f, 0f);
        lc = new SetNode("lightcenter");
        lc.getLocalRotation().fromAngleAxis(0, Vector3f.UNIT_Y);
        lc.getLocalTranslation().set(0,400f,0);

        lookcenter.set(0,100f,200f);
        //lc.getLocalRotation().mult(lookcenter, ln.getLocalTranslation());
        ln.updateWorldVectors(app.nullUpdate);
        lc.attachChild(ln);
        lc.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(lc);
        
        //app.lightEffect.setEnabled(true);
        
        app.camn.getLocalTranslation().set(0,600,600);
        //app.camn.getLocalRotation().lookAt(app.camn.getLocalTranslation().negate().normalizeLocal(), Vector3f.UNIT_Y);
        

        
        // setup an ortho batch to hold the depth texture
        Quad dq = new Quad(256,256);
        dq.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch dview = new TriBatch();
        dview.setModel(dq);
        dview.setCullMode(SceneElement.CULL_NEVER);
        Material dqm=new Material();
        
        /*
        TextureState ts = new TextureState();
        ts.setTexture(shadowTex, 0);
        ts.setEnabled(true);
        dqm.setRenderState(ts);
         */
        
        ShaderObjectsState shader = new ShaderObjectsState();
        shader.load("varying vec2 vTexCoord;" +
                "void main(void) {" +
                "vTexCoord = gl_MultiTexCoord0.xy;" +
                "gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" +
                "}" +
                ""
                , "uniform sampler2D depthTexture;" +
                "varying vec2 vTexCoord;" +
                "void main(void) {" +
                "vec4 depth = texture2D(depthTexture, vTexCoord);" +
                "float len=pow( depth.r, 100.0 );" +
                //"depth = vec4( pow( depth.r, 100.0 ), pow( depth.g, 100.0 ), pow( depth.b, 100.0 ), pow( depth.a, 100.0 ) );" +
                "depth = vec4(len,len,len,1.0);" +
                //"depth = vec4( vTexCoord.x, vTexCoord.y, 0, 1);" +
                "gl_FragColor = depth;" +
                "}" +
                "");
        shader.setEnabled(true);
        ShaderParameters sp = new ShaderParameters();
        sp.setEnabled(true);
        /*
        ShaderVariableInt texunit = new ShaderVariableInt("depthTexture");
        texunit.set(0);
        sp.setUniform(texunit);
         */
        
        
        /*
        ShaderTexture st = new ShaderTexture();
        st.set("depthTexture", depthTexture);
        sp.setTexture(st);
         */
        //dqm.setRenderState(shader);
        //dqm.setRenderState(sp);
        
        // show ssao
        TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(ssaoTexture);
        dqm.setRenderState(ts);
         
        dqm.setLightCombineMode(LightState.OFF);
        
        dview.setMaterial(dqm);
        dview.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        Mesh dvm = new Mesh("depthview");
        dvm.setCullMode(SceneElement.CULL_NEVER);
        dvm.setBatch(dview);
        dvm.getLocalTranslation().set(128,148,0);
        dvm.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        dvm.setRenderPassMode(RenderPass.PassFilter.Ortho.value);
        app.rootNode.attachChild(dvm);
        
        
        CameraNode vcamn = (CameraNode) app.rootNode.getChild("Camera Node");
        //app.rootNode.detachChild(vcamn);
        //app.cam.lookDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        //vcamn.setCamera(app.cam);
        //vcamn.getLocalRotation().lookAt(boxLocation, Vector3f.UNIT_Y);
        //app.cam = lightCamera;
        //ln.attachChild(vcamn);
        //app.frame[0].startFrame();
        
        
    }

    float[] angles = new float[3];

    SSAOPass ssaoPass;
    
    @Override
    protected void simpleUpdate(Frame f) {
        rottime += f.getUpdateContext().time;
        if(rottime>FastMath.PI*2)
            rottime -= FastMath.PI*2;
        
        lc.getLocalRotation().fromAngleAxis(rottime, Vector3f.UNIT_Y);
        lc.getLocalRotation().mult(lookcenter, ln.getLocalTranslation());
        ln.getLocalRotation().lookAt(ln.getLocalTranslation().subtract(boxLocation), Vector3f.UNIT_Y);
        lc.updateGeometricState(f.getUpdateContext(), true);
        
        // get depth pass
        DepthPass dp = (DepthPass) f.getPasses().getPassById(RenderPass.StandardPass.Depth.passId);

        // is SSAO set up?
        if(ssaoPass == null) {
            // create and set up the ssao pass
            ssaoPass = new SSAOPass();
            // generate an id for it
            ssaoPass.setId(app.genPassId());
            // does not use a queue, its a post-processing effect
            ssaoPass.setQueueNo(-1);
            // do not collect to any queue
            ssaoPass.setQueueFilter(0);
            // no material selection
            ssaoPass.setUsedMaterialFlags(-1);
            // set the ssao texture
            ssaoPass.setSSAOTexture(ssaoTexture);
            // disable main screen processing
            ssaoPass.setProcessFrame(true);
            // use main backbuffer
            ssaoPass.setUseCurrentScene(true);
            // use main renderer
            //ssaoPass.setRenderer(app.display.getRenderer());
            // enable the pass
            ssaoPass.setEnabled(true);
            ssaoPass.setNrBlurPasses(2);
            ssaoPass.setBlurSize(0.02f);
            ssaoPass.setBlurIntensityMultiplier(0.5f);
            
        }
        
        // add the pass to the passes
        
        PassManager p = f.getPasses();
        p.addPass(ssaoPass);
        p.setPassOrder(RenderPass.StandardPass.AlphaBlended.passId, ssaoPass.getId(), RenderPass.StandardPass.Ortho.passId);
        
    }

    public static void main(String[] args) {
        Test052SSAOView test = new Test052SSAOView();
        test.start();
    }
}
