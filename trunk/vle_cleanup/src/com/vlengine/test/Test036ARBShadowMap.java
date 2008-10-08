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
import com.vlengine.light.LightSorterGameState;
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Box;
import com.vlengine.model.Quad;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.pass.DepthTexturePass;
import com.vlengine.renderer.pass.RenderPass;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LightNode;
import com.vlengine.scene.Mesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.Spatial;
import com.vlengine.scene.batch.LightBatch;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.LightState;
import com.vlengine.scene.state.MaterialState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.shader.ShaderTexture;
import com.vlengine.light.Shadow;
import com.vlengine.light.ShadowPart;
import com.vlengine.renderer.RenderQueue;
import com.vlengine.system.VleException;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.VertexAttribute;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test036ARBShadowMap extends MainGame {

    Node lc;
    Node ln;
    LightNode lin;
    Vector3f lightcenter = new Vector3f(-100,500f,100f);
    float rottime = 0;
    Vector3f boxLocation = new Vector3f(0,200,0);
    int dimension = 256;
     Vector3f[] verts;

    @Override
    protected void simpleInitGame(AppContext app) {
        
        if(app.conf.lightmode==0) {
            app.conf.lightmode = 1;
            LightSorterGameState lightManagement = new LightSorterGameState();
            lightManagement.setActive(true);
            app.getGameStates().attachChild(lightManagement);
            //app.lightState.setEnabled(false);
            //app.lightEffect.setEnabled(false);
        }
        
        // set up stuff for shadowmap
        setupShadowMap();

        Box b = new Box(new Vector3f(0,0,0), 50,50,50);
        // store vertices for camera zoom-in
        FloatBuffer vertsBuf = b.getAttribBuffer(VertexAttribute.USAGE_POSITION).getDataBuffer();
        verts = BufferUtils.getVector3Array(vertsBuf);
        TriBatch t = new TriBatch();
        t.setModel(b);
        // this is casting shadow
        long shadowCaster = (RenderQueue.FILTER_OPAQUE)|dp.getQueueFilter();
        t.setRenderQueueMode(shadowCaster);
        t.setCullMode(Spatial.CULL_NEVER);
        
        Texture tex = app.getResourceFinder().getTexture("tangents.jpg", ParameterMap.MAP_EMPTY);

        if ( tex == null ) {
            throw new VleException("Cannot load texture");
        }
        
        // create texturestate
        TextureState txs = new TextureState();
        txs.setTexture(tex,0);
        txs.setTexture(shadowTex, 1);
        txs.setEnabled(true);
        
        // crate material
        Material mat = new Material();
        mat.setRenderState(txs);
        
        // create materialstate
        MaterialState mats = new MaterialState();
        mats.setAmbient(ColorRGBA.makeIntensity(0.2f));
        mats.setDiffuse(ColorRGBA.gray);
        mats.setEmissive(ColorRGBA.black);
        mats.setSpecular(ColorRGBA.gray);
        
        mat.setRenderState(mats);
        mat.setDefaultColor(mats.getAmbient());

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
        txsq.setTexture(tex, 0);
        txsq.setTexture(shadowTex, 1);
        txsq.setEnabled(true);
        
        
        // crate material
        Material matq = new Material();
        matq.setRenderState(txsq);
        matq.setRenderState(mats);
        matq.setDefaultColor(mats.getAmbient());
        
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
        tl.setRenderQueueMode(RenderQueue.FILTER_OPAQUE);
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
        lc.getLocalTranslation().set(0,0f,0);

        //lc.getLocalRotation().mult(lookcenter, ln.getLocalTranslation());
        ln.updateWorldVectors(app.nullUpdate);
        lc.attachChild(ln);
        lc.updateWorldVectors(app.nullUpdate);
        app.rootNode.attachChild(lc);
        
        if(app.conf.lightmode==0)
            app.conf.lightmode = 1;
        
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
        
        shadowViewTex = new Texture();
        shadowViewTex.setApply(Texture.AM_REPLACE);
        ShaderTexture st = new ShaderTexture();
        st.set("depthTexture", shadowViewTex);
        sp.setTexture(st);
        dqm.setRenderState(shader);
        dqm.setRenderState(sp);
         
        dqm.setLightCombineMode(LightState.OFF);
        
        dview.setMaterial(dqm);
        dview.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        Mesh dvm = new Mesh("depthview");
        dvm.setCullMode(SceneElement.CULL_NEVER);
        dvm.setBatch(dview);
        dvm.getLocalTranslation().set(148,148,0);
        dvm.setRenderQueueMode(RenderQueue.QueueFilter.Ortho.value);
        dvm.setRenderPassMode(RenderPass.PassFilter.Ortho.value);
        app.rootNode.attachChild(dvm);
        
        
        //app.lightState.setEnabled(false);
        
    }

    DepthTexturePass dp;
    //ViewCamera lightCamera;
    Shadow shadow;
    // the shadow texture
    Texture shadowTex;
    // the shadow view texture
    Texture shadowViewTex;
    //CameraNode lightCameraNode;
    
    private void setupShadowMap() {
        lin = createLight();
        app.rootNode.attachChild(lin);
        lin.updateWorldVectors(app.nullUpdate);
        

        // create camera
        dp = new DepthTexturePass();
        
        
        // create shadow render pass
        dp.setId(app.genPassId());
        dp.setUsedMaterialFlags(0);
        // gather into a new queue
        dp.setQueueNo(app.genQueId());
        // gather opaque batches
        dp.setQueueFilter(1<<dp.getQueueNo());
        dp.setDimension(dimension);
        //dp.setRenderer(app.display.getRenderer());
        // create the shadow
        
        ((LightBatch)lin.getBatch()).createShadow(true);
        shadow = ((LightBatch)lin.getBatch()).getShadow();
        shadow.setPerspectiveSplits(1);
        ShadowPart part = shadow.getPerspectiveSplit(0);
        ViewCamera lightCamera = new ViewCamera();
        
        lightCamera.setFrustumPerspective(60f, 1f, 1f, 4000f);
        lightCamera.resize(dimension, dimension);
        Vector3f loc = new Vector3f(0.0f, 0.0f, 25.0f);
        Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f dir = new Vector3f(0.0f, 0f, -1.0f);
        /** Move our camera to a correct place and orientation. */
        lightCamera.setFrame(loc, left, up, dir);

        part.setCamera(lightCamera);

        shadowTex = new Texture();
        shadowTex.setApply(Texture.AM_MODULATE);
        
        shadowTex.setDepthCompareMode(Texture.DC_LEQUAL);
        shadowTex.setDepthTextureMode(Texture.DT_INTENSITY);
        
        part.setTexture(shadowTex);
    }

    float[] angles = new float[3];

    Quaternion rot = new Quaternion();
    
    @Override
    protected void simpleUpdate(Frame f) {
        rottime += f.getUpdateContext().time;
        if(rottime>FastMath.PI*2)
            rottime -= FastMath.PI*2;
        
        rot.fromAngleAxis(rottime, Vector3f.UNIT_Y);
        rot.mult(lightcenter, ln.getLocalTranslation());
        //ln.getLocalTranslation().addLocal(boxLocation);
        //lc.updateGeometricState(f.getUpdateContext(), true);
        ln.getLocalRotation().lookAt(ln.getLocalTranslation().subtract(boxLocation), Vector3f.UNIT_Y);
        ln.updateGeometricState(f.getUpdateContext(), true);
        //app.lightState.setNeedsRefresh(true);
        
        if(dp.isSupported()) {
            dp.setEnabled(true);
            // shadowmap code, move this to ShadowPass
            //reposition light camera
            
            Transform lightTrans = shadow.getParent().getWorldTransForm(f.getFrameId());
            Vector3f lightPos = lightTrans.getTranslation();//.mult(1.1f);
            Quaternion lightRot = lightTrans.getRotation();
            
            ViewCamera lightCamera = shadow.getPerspectiveSplit(0).getCamera();
            lightCamera.setLocation(lightPos);
            lightCamera.lookAt(boxLocation, Vector3f.UNIT_Y);
            lightCamera.setFrustumPerspective(45.0f, 1.0f, 1.8f, 50.0f);
            lightCamera.setZoom(verts);
        
            lightCamera.update();
            dp.setShadowPart(shadow.getPerspectiveSplit(0));
            //dp.setCamera(lightCamera);//shadow.getSplit(0).getCamera());
            //dp.setTexture(shadowTex);//shadow.getSplit(0).getTexture());

            // add the pass to cull the scene
            
            f.getPasses().addPass(dp);
            f.getPasses().setPassOrder(RenderPass.StandardPass.Ligh.passId, dp.getId(), RenderPass.StandardPass.Opaque.passId);
            // initialize the queue for the pass
            f.getQueueManager().createQueue(dp.getQueueNo(), null);

            if(shadowTex.getTextureId()!=0) {
                shadowViewTex.setTextureId(shadowTex.getTextureId());
            }
        }
    }

    public static void main(String[] args) {
        Test036ARBShadowMap test = new Test036ARBShadowMap();
        test.start();
    }
}
