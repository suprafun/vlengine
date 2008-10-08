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
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.DepthTexturePass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.CameraNode;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.light.Shadow;
import com.vlengine.light.ShadowPart;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.system.VleException;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test034DepthView extends MainGame {

    Node lc;
    Node ln;
    LightNode lin;
    Vector3f lookcenter = new Vector3f();
    float rottime = 0;
    Vector3f boxLocation = new Vector3f(0,400,0);
    ViewCamera depthCamera = new ViewCamera();

    @Override
    protected void simpleInitGame(AppContext app) {
        
        // set up stuff for shadowmap
        setupShadowMap();

        // enable light management
        app.conf.lightmode = 1;
        
        Box b = new Box(new Vector3f(0,0,0), 50,50,50);
        //q.setDisplayListMode(BaseGeometry.LIST_NO);
        TriBatch t = new TriBatch();
        t.setModel(b);
        // this is casting shadow
        long shadowCaster = RenderQueue.FILTER_OPAQUE;
        t.setRenderQueueMode(shadowCaster);
        t.setCullMode(Spatial.CullMode.NEVER);
        
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
        tq.setCullMode(Spatial.CullMode.NEVER);
        
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
        tl.setCullMode(Spatial.CullMode.NEVER);
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
        dview.setCullMode(SceneElement.CullMode.NEVER);
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
        
        ShaderTexture st = new ShaderTexture();
        st.set("depthTexture", shadowTex);
        sp.setTexture(st);
        dqm.setRenderState(shader);
        dqm.setRenderState(sp);
         
        dqm.setLightCombineMode(LightState.OFF);
        
        dview.setMaterial(dqm);
        dview.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        Mesh dvm = new Mesh("depthview");
        dvm.setCullMode(SceneElement.CullMode.NEVER);
        dvm.setBatch(dview);
        dvm.getLocalTranslation().set(128,148,0);
        dvm.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        dvm.setRenderPassMode(RenderPass.PassFilter.Ortho.value);
        app.rootNode.attachChild(dvm);
        
        
        //CameraNode vcamn = (CameraNode) app.rootNode.getChild("Camera Node");
        //app.rootNode.detachChild(vcamn);
        //app.cam.lookDirection(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        //vcamn.setCamera(app.cam);
        //vcamn.getLocalRotation().lookAt(boxLocation, Vector3f.UNIT_Y);
        //app.cam = lightCamera;
        //ln.attachChild(vcamn);
        //app.frame[0].startFrame();
        
        
    }

    DepthTexturePass dp;
    //ViewCamera lightCamera;
    Shadow shadow;
    Texture shadowTex;
    //CameraNode lightCameraNode;
    
    private void setupShadowMap() {
        lin = createLight();
        app.rootNode.attachChild(lin);
        lin.updateWorldVectors(app.nullUpdate);
        

        // create camera
        dp = new DepthTexturePass();
        
        
        // create shadow render pass
        dp.setId(app.genPassId());
        // no material flags
        dp.setUsedMaterialFlags(0);
        // gather into a new queue
        dp.setQueueNo(RenderQueue.StandardQueue.Opaque.queuId);
        // dont gather anything
        dp.setQueueFilter(RenderQueue.QueueFilter.None.value);
        dp.setDimension(256);
        //dp.setRenderer(app.display.getRenderer());
        // create the shadow
        
        /*
        ((LightBatch)lin.getBatch()).createShadow(true);
        shadow = ((LightBatch)lin.getBatch()).getShadow();
        shadow.setPerspectiveSplits(1);
        ShadowPart part = shadow.getPerspectiveSplit(0);
        ViewCamera lightCamera = new ViewCamera();
        lightCamera.setFrustumPerspective(60f, 1f, 10f, 8000f);
        part.setCamera(lightCamera);
        dp.setShadowPart(part);
         */
        depthCamera.setFrustumPerspective(60f, 1f, 10f, 8000f);
         
        shadowTex = new Texture();
        shadowTex.setApply(Texture.AM_REPLACE);
        dp.setTexture(shadowTex);
        
        //part.setTexture(shadowTex);
    }

    float[] angles = new float[3];

    @Override
    protected void simpleUpdate(Frame f) {
        rottime += f.getUpdateContext().time;
        if(rottime>FastMath.PI*2)
            rottime -= FastMath.PI*2;
        
        lc.getLocalRotation().fromAngleAxis(rottime, Vector3f.UNIT_Y);
        lc.getLocalRotation().mult(lookcenter, ln.getLocalTranslation());
        ln.getLocalRotation().lookAt(ln.getLocalTranslation().subtract(boxLocation), Vector3f.UNIT_Y);
        lc.updateGeometricState(f.getUpdateContext(), true);
        //app.lightState.setNeedsRefresh(true);
        
        if(dp.isSupported()) {
            dp.setEnabled(true);
            // shadowmap code, move this to ShadowPass
            //reposition light camera
            
            //Transform lightTrans = //shadow.getParent().getWorldTransForm(f.getFrameId());
            //Vector3f lightPos = ln.getWorldTranslation();//lightTrans.getTranslation();
            //Quaternion lightRot = ln.getWorldRotation();//lightTrans.getRotation();
            //Vector3f lightDir = lightRot.getRotationColumn(2, null);
            
            //ViewCamera lightCamera = shadow.getPerspectiveSplit(0).getCamera();
            //lightCamera.setLocation(lightPos);
            //lightCamera.lookDirection(lightDir, Vector3f.UNIT_Y);
            //lightCamera.getLocation().set(ln.getWorldTranslation());
            // rotate light camera
            //ln.getLocalRotation().toAngles(angles);
            
            //lightCamera.lookDirection(ln.getWorldRotation()., lookcenter)
            //lightCamera.lookAt(boxLocation, Vector3f.UNIT_Y);
            //lightCamera.getDirection().set(angles).normalizeLocal();
            //lightCamera.update();

            // set camera and texture into the shadow pass
            app.cam.copy(depthCamera);
            depthCamera.update();
            dp.setCamera(depthCamera);//shadow.getSplit(0).getCamera());
            dp.setTexture(shadowTex);//shadow.getSplit(0).getTexture());

            // add the pass to cull the scene
            
            f.getPasses().addPass(dp);
            f.getPasses().setPassOrder(dp.getId(), RenderPass.StandardPass.Opaque.passId);
            // initialize the queue for the pass
            //f.getQueueManager().createQueue(dp.getQueueNo(), null);

        }
    }

    public static void main(String[] args) {
        Test034DepthView test = new Test034DepthView();
        test.start();
    }
}
