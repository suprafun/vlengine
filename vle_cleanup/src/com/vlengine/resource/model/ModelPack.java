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

import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.model.Geometry;
import com.vlengine.model.XWeightedGeometry;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.VertexFormat;
import com.vlengine.util.xml.Element;
import com.vlengine.util.xml.XMLFile;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ModelPack {
    // the name of the modelpack, its used for the
    // file names also.
    protected String name;
    // the path for this modelpack
    protected String path;
            
    // the models in this pack
    protected HashMap<String, Model> models = new HashMap<String, Model>();

    // create modelpack for each vertexformat we encounter
    // and every VBO mode we encounter
    protected HashMap<String, ModelPartPack> createdPacks = new HashMap<String, ModelPartPack>();

    

    public ModelPack() {};

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public ModelPartPack getPartPack(VertexFormat vfm, int vboMode) {
        String packname = name+"_"+vfm.toString()+"_"+vboMode;
        
        // get the desired pack
        ModelPartPack pack = createdPacks.get(packname);
        if( pack == null ) {
            // create the pack
            pack = new ModelPartPack();
            pack.VBOMode = vboMode;
            pack.format = vfm;
            pack.name = packname;
            //pack.vb = new VertexBuffer();
            
            // store it
            createdPacks.put(packname, pack);
        }
        return pack;
    }

    /**
     * This method saves the files composing the modelpack
     * @param path The path to save the files of the modelpack
     */
    public void save(String path) {
        
        
        // create the XML document for the modelpack
        Element doc = new Element("modelpack");
        // create section for pack parts
        Element packparts = new Element("packparts");
        doc.addContent(packparts);

        // go trough the parts add to document and save files
        for(ModelPartPack mpp : createdPacks.values()) {
            Element packpart = new Element("packpart");
            packparts.addContent(packpart);
            mpp.save(packpart, path);
        }

        // save the models
        Element modele = new Element("models");
        doc.addContent(modele);

        for(Model mo : models.values()) {
            Element mode = new Element("model");
            modele.addContent(mode);
            
            mo.save(mode);
        }

        // save the XML file
        String filepath = path +"/" + name+".pack.gz";
        this.path = filepath;
        
        XMLFile.toXML(filepath, doc, true);
    }

    public void load(String path) {
        
        String filepath = path +"/" + name+".pack.gz";
        this.path = filepath;
        
        Element doc = XMLFile.fromXML(filepath, true);
        
        Element packparts = doc.getChild("packparts");
        if(packparts!=null) {
            FastList<Element> packlist = packparts.getChildren("packpart");
            for(int i=0; i<packlist.size(); i++) {
                Element pe = packlist.get(i);
                ModelPartPack pp = new ModelPartPack();
                pp.load(pe, path);
                createdPacks.put(pp.name, pp);
            }
        }

        // load the models
        Element mods = doc.getChild("models");
        if( mods != null) {
            FastList<Element> modlist = mods.getChildren();
            for(int i=0; i<modlist.size(); i++) {
                Element mode = modlist.get(i);
                Model mo = new Model();
                mo.load(mode);
                models.put(mo.name, mo);
            }
        }

        // process the model parts
        for(Model mo : models.values()) {
            for(int mlod=0; mlod<mo.modelLod.size(); mlod++) {
                FastList<ModelPart> mprtlist = mo.modelLod.get(mlod);
                for(int i=0; i<mprtlist.size(); i++) {
                    ModelPart mprt = mprtlist.get(i);
                    // put reference on materials into the parts
                    mprt.modelMaterial = mo.materials.get(mprt.materialname);

                    // create the geometry for the part
                    // is it bone mapped?
                    Geometry geom = null;
                    if(mprt.boneMapping!=null) {
                        geom = new XWeightedGeometry();
                        // set the bone mapping
                        ((XWeightedGeometry)geom).setBoneMapping(mprt.boneMapping);
                    } else {
                        geom = new Geometry();
                    }
                    // get the partpack for the part
                    ModelPartPack pp = createdPacks.get(mprt.partpack);
                    // set the vertex buffer
                    geom.addAttribBuffer(pp.vb, mprt.startVertex);
                    //geom.setStartVertex(mprt.startVertex);
                    geom.setNumVertex(mprt.numVertex);
                    // set the proper index buffer
                    if(mprt.intIndex) {
                        geom.setIndexBuffer(pp.ibi);
                    } else {
                        geom.setIndexBuffer(pp.ibs);
                    }
                    geom.setStartIndex(mprt.startIndex);
                    geom.setNumIndex(mprt.numIndex);
                    
                    // set the vbo mode
                    geom.setVBOMode(pp.VBOMode);
                    // if vbomode is 0, set the list mode
                    // we cannot have VBO and list enabled the same time,
                    // so, VBO mode takes precedence
                    if(pp.VBOMode == 0) {
                        geom.setDisplayListMode(mprt.listMode);
                    }
                    
                    // create bounding box for the model
                    //TODO: based on saved information
                    if(mprt.bound!=null)
                        geom.setModelBound(mprt.bound);
                    else {
                        geom.setModelBound(new BoundingBox());
                        geom.updateModelBound();
                    }
                    if(mprt.hasColTree) {
                        if(mprt.colTree!=null) {
                            geom.setCollisionTree(mprt.colTree);
                        } else {
                            // calculate collision tree
                            geom.createCollisionTree();
                        }
                    }
                    if(mprt.hasColVolume) {
                        if(mprt.colVol!=null) {
                            geom.setCollisionVolume(mprt.colVol);
                        } else {
                            // calculate collision volume
                            if(geom instanceof XWeightedGeometry) {
                                geom.createCollisionVolume(CollisionVolume.DEFAULT_CELLSIZE_ANIMATED);
                            } else {
                                geom.createCollisionVolume(CollisionVolume.DEFAULT_CELLSIZE_STATIC);
                            }
                        }
                    }
                    mprt.geom = geom;
                }
            }
        }
    }
    
    public Model getModel(String name) {
        return models.get(name);
    }
    
    public FastList<Model> getModels() {
        FastList<Model> ms = new FastList<Model>();
        for(Model m: models.values()) {
            ms.add(m);
        }
        return ms;
    }
    
    /**
     * Returns the list of files used by this modelpack:
     * geometry and texture files
     * @return
     */
    public HashSet<String> getFileList() {
        // get all the files of the parts
        HashSet<String> files = new HashSet<String>();
        if(path==null)
            return null;
        // add the main file
        files.add(path);
        
        // add the files of pack parts
        for(ModelPartPack pp:createdPacks.values()) {
            pp.getFiles(files);
        }

        HashSet<String> textures = new HashSet<String>();
        // go over every model
        for(Model m:models.values()) {
            for(ModelMaterial mm:m.getMaterials().values()) {
                FastList<ModelMaterialPart> mmparts = mm.getTextures();
                for(int i=0; i<mmparts.size(); i++) {
                    // add the textures used in materials
                    ModelMaterialPart mmpart = mmparts.get(i);
                    String txname = mmpart.getTextureName().toLowerCase();
                    if(txname!=null) {
                        textures.add(txname);
                    }
                }
            }
        }

        files.addAll(textures);

        return files;
    }
}
