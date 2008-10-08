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
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.bounding.CollisionTree;
import com.vlengine.bounding.CollisionVolume;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.util.IntList;
import com.vlengine.util.xml.Element;

/**
 * A ModelPart holds information on a part of the model,
 * a single geometry that can hold a single material.
 * If named, a modelpart traslates to a Mesh in the scene,
 * unnamed ModelParts will be translated to a TriBatch.
 * 
 * @author vear (Arpad Vekas)
 */
public class ModelPart {
    
    private static final float[] farr = new float[3];
    private static final Vector3f fvec = new Vector3f();

    // the name of the part, if the part has no name
    // it is created as TriBatch
    protected String name;

    // the name of the part pack the geometry is saved to
    protected String partpack;
    // the geometry information in the pack for the part
    protected int startVertex;
    protected int numVertex;
    // use int-s for indices, default false
    protected boolean intIndex = false;
    protected int startIndex;
    protected int numIndex;
    // display list mode
    protected int listMode;
    // we store generic information about the material as
    protected String materialname;
    // the bound type 0-no, 1-box, ...
    protected BoundingVolume bound;
    
    // runtime information
    protected ModelMaterial modelMaterial;
    // geometry information of the part
    protected Geometry geom;
    // collision tree for the part
    protected boolean hasColTree;
    protected CollisionTree colTree;
    
    protected boolean hasColVolume;
    protected CollisionVolume colVol;
    
    // bone mapping for XWeightedGeometry
    protected IntList boneMapping;

    protected void save(Element parte) {
        if(name!=null) {
            parte.addContent(new Element("name").setText(name));
        }
        parte.addContent(new Element("pack").setText(partpack));
        parte.addContent(new Element("listMode").setText(listMode));
        parte.addContent(new Element("startVertex").setText(startVertex));
        parte.addContent(new Element("numVertex").setText(numVertex));
        if(intIndex)
            parte.addContent(new Element("intIndex").setText(intIndex));
        parte.addContent(new Element("startIndex").setText(startIndex));
        parte.addContent(new Element("numIndex").setText(numIndex));
        // the bound
        if(bound!=null) {
            Element boe = new Element("bound");
            parte.addContent(boe);

            int boundtype = 0;
            boe.addContent(new Element("center").setText(bound.getCenter().toArray(farr)));
            if(bound instanceof BoundingBox) {
                boundtype = 1;
                boe.addContent(new Element("xExtent").setText(((BoundingBox)bound).xExtent));
                boe.addContent(new Element("yExtent").setText(((BoundingBox)bound).yExtent));
                boe.addContent(new Element("zExtent").setText(((BoundingBox)bound).zExtent));
            }
            boe.addContent(new Element("type").setText(boundtype));
        }
        // the collision tree
        if(hasColTree) {
            Element coe = new Element("collision");
            parte.addContent(coe);
            // TODO: actualy save collision tree to binary format?
        }
        // collision volume
        if(hasColVolume) {
            Element coe = new Element("collisionvolume");
            parte.addContent(coe);
        }
        // the bone mapping
        if(boneMapping!=null) {
            Element bme = new Element("bones");
            bme.setText(boneMapping);
        }
        // save the material id
        parte.addContent(new Element("material").setText(modelMaterial.id));
    }

    protected void load(Element parte) {
        name = parte.getChildText("name");
        partpack = parte.getChildText("pack");
        listMode = parte.getChildint("listMode");
        startVertex = parte.getChildint("startVertex");
        numVertex = parte.getChildint("numVertex");
        intIndex = parte.getChildboolean("intIndex");
        startIndex = parte.getChildint("startIndex");
        numIndex = parte.getChildint("numIndex");
        materialname = parte.getChildText("material");
        Element boe = parte.getChild("bound");
        int boundtype = 0;
        if( boe != null) {
            Vector3f center = fvec.set(boe.getChildfloat("center", farr));
            boundtype = boe.getChildint("type");
            if(boundtype==1) {
                // bounding box
                float xExtent = boe.getChildfloat("xExtent");
                float yExtent = boe.getChildfloat("yExtent");
                float zExtent = boe.getChildfloat("zExtent");
                bound = new BoundingBox(center, xExtent, yExtent, zExtent);
            }
        }
        Element coe = parte.getChild("collision");
        if(coe!=null) {
            // we have a collision tree
            // TODO: load the tree from binary file
            // if we got parameter
            hasColTree = true;
        }
        Element cove = parte.getChild("collisionvolume");
        if(cove!=null) {
            // we have a collision tree
            // TODO: load the tree from binary file
            // if we got parameter
            hasColVolume = true;
        }
        // does it has bone mapping
        Element bme = parte.getChild("bones");
        if(bme!=null) {
            // load it
            boneMapping = bme.getTextintlist(null);
        }
    }

    public void setGeometry(Geometry geom) {
        this.geom = geom;        
    }
    
    public Geometry getGeometry() {
        return geom;
    }
    
    public void setMaterial(ModelMaterial modmat) {
        this.modelMaterial = modmat;
    }

    public ModelMaterial getMaterial() {
        return modelMaterial;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
