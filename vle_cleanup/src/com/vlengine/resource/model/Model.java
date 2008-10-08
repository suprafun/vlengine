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

package com.vlengine.resource.model;

import com.vlengine.app.AppContext;
import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.BoundingSphere;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Geometry;
import com.vlengine.model.MD5WeightedGeometry;
import com.vlengine.model.XWeightedGeometry;
import com.vlengine.renderer.material.MatParameters;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.material.MaterialLib;
import com.vlengine.resource.ParameterMap;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.SetNode;
import com.vlengine.scene.animation.Bone;
import com.vlengine.scene.animation.x.XAnimatedItem;
import com.vlengine.scene.animation.x.XBoneAnimation;
import com.vlengine.scene.animation.x.XBoneAnimationController;
import com.vlengine.scene.animation.x.XBoneAnimationPack;
import com.vlengine.scene.animation.x.XGPUAnimatedItem;
import com.vlengine.scene.animation.x.XSoftSkinner;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.shader.ShaderVariableMatrix4Buffer;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.xml.Element;
import java.util.HashMap;

/**
 * A Model object holds information about a disctinct entity that can be
 * re-used in the scene. The most basic example of a model is a Geometry object,
 * more advanced have more geometry parts, materials. All these are stored in 
 * the model object.
 * 
 * @author vear (Arpad Vekas)
 */
public class Model {
    //      MDL model file (possibly XML) for all models in the pack (Mesh, LodMesh, Node)
    //              name of the model
    //              LOD levels of the model
    //              for each LOD level
    //                  submodel parts of the model
    //                      name of the part (unnamed parts will be TriBatch, named ones will be Mesh+TriBatch)
    //                      geometry to use (CompositeGeometry)
    //                          startvertex
    //                          numvertex
    //                          startindex
    //                          numindex
    //                      material information (Material)
    //                          mat_func (method to use from material library)
    //                          alpha, cull, material-state
    //                          submaterials (textures) (Texture)
    //                      bounds
    //              bounds

    // the unique name of this model
    protected String name;
    
    // all the parts of the model
    protected FastList<FastList<ModelPart>> modelLod = new FastList<FastList<ModelPart>>();
    
    // the materials in this model
    protected HashMap<String, ModelMaterial> materials = new HashMap<String, ModelMaterial>();
    
    // the lights in this model
    protected FastList<ModelLight> ligts = new FastList<ModelLight>();

    // the animations pack for this model
    // TODO: this assumes that all the lods and all the animations for the model
    // use the same bones
    // TODO: saving and loading of the animation pack
    protected XBoneAnimationPack animations;
    
    // the cell size
    protected Vector3f cellSize;
    // the collision volume
    protected CollisionVolume collVolume;
    
    protected void save(Element mode) {
        mode.addContent(new Element("name").setText(name));
        // save the materials
        Element mates = new Element("materials");
        mode.addContent(mates);
        
        for(ModelMaterial mma : materials.values()) {
            Element mate = new Element("material");
            mates.addContent(mate);
            mma.save(mate);
        }
        // save the parts
        for(int lod=0; lod<modelLod.size(); lod++) {
            Element lode = new Element("lod");
            mode.addContent(lode);
            lode.addContent(new Element("index").setText(lod));
            FastList<ModelPart> plist = modelLod.get(lod);
            for(int mi = 0; mi<plist.size(); mi++) {
                Element parte = new Element("mesh");
                lode.addContent(parte);
                
                ModelPart prt = plist.get(mi);
                prt.save(parte);
            }
        }
        
        // 
        if(cellSize!=null) {
            mode.setChild("collcellsize").setText(cellSize);
        }
        
        // TODO: save the animations
        if(animations != null) {
            // element for animations
            Element ae = new Element("animations");
            mode.addContent(ae);
            // save bones
            Element boe = new Element("bones");
            ae.addContent(boe);
            FastList<Bone> bones = animations.getBones();
            for(int i=0; i<bones.size(); i++) {
                Bone b = bones.get(i);
                Element be = new Element("bone");
                boe.addContent(be);
                be.addContent(new Element("id").setText(b.id));
                be.addContent(new Element("name").setText(b.name));
                if(b.parent!=null) {
                    be.addContent(new Element("parent").setText(b.parent.id));
                }
                if(b.frameMatrix!=null) {
                    be.addContent(new Element("framematrix").setText(b.frameMatrix));
                }
                if(b.matrixOffset!=null) {
                    be.addContent(new Element("mmatrixoffset").setText(b.matrixOffset));
                }
            }
            // save animations
            FastList<XBoneAnimation> anims = animations.getAnimations();
            for(int i=0; i<anims.size(); i++) {
                XBoneAnimation anim = anims.get(i);
                if(anim!=null) {
                    Element ane = new Element("animation");
                    ae.addContent(ane);
                    ane.addContent(new Element("id").setText(i));
                    ane.addContent(new Element("name").setText(anim.name));
                    ane.addContent(new Element("numframes").setText(anim.numFrames));
                    ane.addContent(new Element("framerate").setText(anim.frameRate));
                    
                }
            }
        }
    }

    protected void load(Element mode) {
        name = mode.getChildText("name");
        // load the materials
        Element mats = mode.getChild("materials");
        if( mats != null ) {
            FastList<Element> matlist = mats.getChildren();
            for(int i=0; i<matlist.size(); i++) {
                Element mat = matlist.get(i);
                ModelMaterial mma = new ModelMaterial();
                mma.load(mat);
                materials.put(mma.id, mma);
            }
        }
        FastList<Element> lodlist = mode.getChildren("lod");
        for(int i=0; i<lodlist.size(); i++) {
            Element lode=lodlist.get(i);
            int index = lode.getChildint("index");
            modelLod.ensureCapacity(index);
            FastList<ModelPart> mplist = modelLod.get(index);
            if(mplist == null) {
                mplist = new FastList<ModelPart>();
                modelLod.set(index, mplist);
            }
            FastList<Element> meshlist = lode.getChildren("mesh");
            for(int j=0; j<meshlist.size(); j++) {
                Element parte = meshlist.get(j);
                ModelPart prt = new ModelPart();
                prt.load(parte);
                mplist.add(prt);
            }
        }
        // load cell size
        Element cse = mode.getChild("collcellsize");
        if(cse!=null) {
            this.cellSize = cse.getText(cellSize);
        }

        
        Element ane = mode.getChild("animations");
        if(ane!=null) {
            // TODO: load animations
        }
    }

    public void addPart(int lod, ModelPart part) {
        modelLod.ensureCapacity(lod);
        FastList<ModelPart> mlod = modelLod.get(lod);
        if( mlod == null ) {
            mlod = new FastList<ModelPart>();
            modelLod.set(lod, mlod);
        }
        mlod.add(part);
        // collect the materail also
        ModelMaterial mm = part.modelMaterial;
        if(mm.id == null)
            mm.id = "";
        materials.put(mm.id, mm);
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get a new instance of the model. The instance can then be attached to the scene.
     * 
     * @param app       The application context
     * @param params    Parameters to be used in creation of the model:
     *                  boolean BINDPOS    dont attach animation controllers, but create a boned model
     *                                     in its bindpose frame
     *                  int vbomode        override VBO mode of the model and use the provided VBO mode
     *                  int listmode       override display list mode of the model and use the provided display list mode
     * 
     * @return
     */
    public SetNode getInstance(AppContext app, ParameterMap params) {
        boolean bindpose = params.getBoolean("BINDPOSE", false);
        
        boolean hasAnimated = false;
        XBoneAnimationController xAnimC = null;
        // do we have an X animation?
        if(animations!=null && !bindpose) {
            xAnimC = new XBoneAnimationController("BoneAnim");
            // set in the animations
            xAnimC.setAniamtionPack(animations);
        }
        // parameters to pass down to material
        MatParameters matParams = new MatParameters();
        
        // sort all the parts based on their name
        // with a special group for those with no name
        HashMap<String,LodMesh> namedparts = new HashMap<String,LodMesh>();
        for(int i=0; i<modelLod.size(); i++) {
            FastList<ModelPart> lod = modelLod.get(i);
            for(int j=0; j<lod.size(); j++) {
                ModelPart mp = lod.get(j);
                if(mp.name==null)
                    mp.name = "";
                LodMesh mesh = namedparts.get(mp.name);
                if(mesh==null ) {
                    mesh = new LodMesh(mp.name);
                    mesh.setCullMode(SceneElement.CullMode.DYNAMIC);
                    namedparts.put(mp.name, mesh);
                }
                // create the batch for the mesh
                TriBatch tb = new TriBatch();
                Geometry geom = null;
                XAnimatedItem animItem=null;
                if(mp.geom instanceof XWeightedGeometry && xAnimC!=null) {
                    // handle X animated geometry
                    
                        
                    // depending on what kind of bone animation we are doing
                    if(app.conf.boneanim_type == 1) {
                        // software skinning
                        // we always create a new skinner
                        XSoftSkinner sa = new XSoftSkinner();
                        sa.setModel((XWeightedGeometry) mp.geom);
                        
                        animItem = sa;
                        xAnimC.getAnimatedItems().add(animItem);
                        xAnimC.forceUpdate();
                        
                        // the geom is the skinner
                        geom = sa;
                        
                    } else {
                        // the geom is the same always
                        geom = mp.geom;
                        
                        // GPU skinning
                        // we reuse items with same bones
                        // get the bone signature
                        IntList bones = ((XWeightedGeometry)geom).getBoneMapping();
                        // check if we have an item already for this
                        FastList<XAnimatedItem> exItems = xAnimC.getAnimatedItems();

                        for(int k=0, mk=exItems.size(); k<mk && animItem==null; k++) {
                            XAnimatedItem exitem = exItems.get(k);
                            if(exitem.getBoneMapping() == bones) {
                                // this bone setup is already in the controller, 
                                // no need to add it again
                                animItem = exitem;
                            }
                        }

                        if(animItem==null) {

                            // we need to create and add another animated item
                            XGPUAnimatedItem ga = new XGPUAnimatedItem();

                            ga.setBoneMapping(bones);
                            // create shader matrix buffer
                            ShaderVariableMatrix4Buffer matBuf = new ShaderVariableMatrix4Buffer();

                            matBuf.allocate(bones.size(), false);
                            // put in an empty matrix at position 0
                            matBuf.set(0, new Matrix4f().zero());

                            ga.setMatrixBuffer(matBuf);

                            animItem = ga;
                            // add the animated item to the controller
                            xAnimC.getAnimatedItems().add(animItem);
                            xAnimC.forceUpdate();

                        }
                    }

                } else if(mp.geom instanceof MD5WeightedGeometry) {
                    // handle MD5 bone animated geoms
                    // if we are to produce bindpose geom
                    if(bindpose) {
                        // create the geom
                        geom = ((MD5WeightedGeometry)mp.geom).createFrameGeom(null);
                        // apply bindpos transform on weights
                        ((MD5WeightedGeometry)mp.geom).jointTransform();
                        // apply the weights on the geom
                        ((MD5WeightedGeometry)mp.geom).apply(geom);
                    } else {
                        // TODO: animated geom
                        // did we get an animation controller?
                        
                        // create a geom, and allocate a mapped buffer for
                        // vertices, to hold its future animation
                    }
                } else
                    geom = mp.geom;
                // override vbo and display list mode
                geom.setVBOMode(params.getInt("vbomode", geom.getVBOMode()));
                geom.setDisplayListMode(params.getInt("listmode", geom.getDisplayListMode()));

                if(animItem!=null && geom.getVBOMode()==BaseGeometry.VBO_NO) {
                    geom.setDisplayListMode(BaseGeometry.LIST_NO);
                    geom.setVBOMode(BaseGeometry.VBO_LONGLIVED);
                }
                if(geom.getVBOMode()!=BaseGeometry.VBO_NO) {
                    geom.createVBOInfos();
                }
                // if the geometry does not yet has collision tree, and we got a parametere, to
                // ensure its existence, then create it
                if(params.getBoolean("coltree", false) && geom.getCollisionTree() == null) {
                    geom.createCollisionTree();
                    geom.setCollidable(true);
                }
                /*
                if(params.getBoolean("colvolume", false) && geom.getCollisionVolume() == null) {
                    if(geom instanceof XWeightedGeometry 
                            || geom instanceof XSoftSkinner ) {
                        geom.createCollisionVolume(CollisionVolume.DEFAULT_CELLSIZE_ANIMATED);
                    } else {
                        geom.createCollisionVolume(CollisionVolume.DEFAULT_CELLSIZE_STATIC);
                    }
                    geom.setCollidable(true);
                }
                 */

                tb.setModel(geom);
                tb.setCullMode(SceneElement.CullMode.DYNAMIC);
                //tb.setCullMode(SceneElement.CULL_NEVER);
                // if we have animation, mark that we need skinning shaders
                matParams.clear();
                if(animItem!=null) {
                    if(animItem instanceof XGPUAnimatedItem) {
                        matParams.put(MaterialLib.BONES, animItem);
                    }
                    hasAnimated = true;
                    matParams.put(MatParameters.ParamKey.Nospecular, true);
                }
                // check if we got normal, binormal and tangent
                if(geom.getAttribBuffer(VertexAttribute.Usage.Normal)==null
                        || geom.getAttribBuffer(VertexAttribute.Usage.Binormal)==null
                        || geom.getAttribBuffer(VertexAttribute.Usage.Tangent)==null
                        ) {
                    // no bump
                    matParams.put(MatParameters.ParamKey.NoTNB, true);
                }
                // TODO: more batch materials, get material type id from the material map
                Material mat = mp.modelMaterial.getInstance(app, matParams);
                if(mat != null) {
                    tb.setMaterial(mat);
                    tb.setRenderQueueMode(mat.getRenderQueueMode());
                    tb.setLightCombineMode(mat.getLightCombineMode());
                }
                mesh.addBatch(i, tb);
            }
        }

        // create the node
        SetNode n = new SetNode(name);
        for(LodMesh mesh:namedparts.values()) {
            n.attachChild(mesh);
        }
        n.setCullMode(SceneElement.CullMode.DYNAMIC);
        
        // do we need collision volume?
        if(params.getBoolean("colvolume", false) && this.collVolume == null) {
            // do we have a scale
            Vector3f csize = cellSize;
            if(csize==null) {
                if(xAnimC!=null && hasAnimated) {
                    // if this is animated, use appropriate cellsize
                    csize = CollisionVolume.DEFAULT_CELLSIZE_ANIMATED;
                } else {
                    csize = CollisionVolume.DEFAULT_CELLSIZE_STATIC;
                }
            }
            // create the volume
            collVolume = new CollisionVolume();
            collVolume.buildVolume(this, csize);
        }

        // if we have a collision volume, attach it to the node
        if(collVolume!=null) {
            n.setCollisionVolume(collVolume);
        }

        // if we have an animation controller attach it
        if(xAnimC!=null && hasAnimated) {
            n.addController(xAnimC);
        }

        //clear out all the material instances from the materials
        //so that the new instance will have new Material instances
        for(ModelMaterial mm:materials.values()) {
            mm.material = null;
        }
        
        return n;
    }

    public FastList<FastList<ModelPart>> getLods() {
        return modelLod;
    }
    
    public HashMap<String, ModelMaterial> getMaterials() {
        return materials;
    }
    
    public FastList<ModelLight> getLights() {
        return ligts;
    }

    public XBoneAnimationPack getAnimations() {
        return animations;
    }
    
    public void setAnimationPack(XBoneAnimationPack anims) {
        animations = anims;
    }
    
    public void setModelBound(int boundType) {
        // go over all the lods
        for(int i=0; i<modelLod.size(); i++) {
            FastList<ModelPart> lod = modelLod.get(i);
            if(lod==null)
                continue;
            for(int j=0; j<lod.size(); j++) {
                ModelPart mp = lod.get(j);
                if(mp==null)
                    continue;
                Geometry geom = mp.geom;
                if(geom==null)
                    continue;
                if(geom.getModelBound().getType()==boundType)
                    continue;
                if(boundType==BoundingVolume.BOUNDING_SPHERE) {
                    geom.setModelBound(new BoundingSphere());
                    // TODO: other bound types
                } else {
                    geom.setModelBound(new BoundingBox());
                }
                // update the model bound
                geom.updateModelBound();
            }
        }
    }
}
