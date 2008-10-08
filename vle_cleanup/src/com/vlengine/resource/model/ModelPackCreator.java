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
import com.vlengine.model.Geometry;
import com.vlengine.model.XWeightedGeometry;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.resource.util.TangentGenerator;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.nio.FloatBuffer;


/**
 * ModelPackCreator is usable for creating model packs,
 * which is tightly packed into geometry files. Those files
 * can be read back into ModelPack objects, and individual
 * models can be requested from those for placement into the scene.
 * 
 * @author vear (Arpad Vekas)
 */
public class ModelPackCreator {

    // the models yet to be processed
    protected FastList<Model> unpacked = new FastList<Model>();
    
    // modelpack we are creating now
    protected ModelPack pack;
    
    public ModelPackCreator() {
        
    }

    public void addModel(Model rawModel) {
        unpacked.add(rawModel);
    }

    public void addAll(ModelPack mp) {
        // get all models and put them in
        unpacked.addAll(mp.getModels());
    }

    public ModelPack createPack(String name) {
        if(unpacked.size()==0)
            return null;

        pack = new ModelPack();
        pack.setName(name);

        // go though all the models, and pack them together
        // this operation is memory consuming, and should be used
        // in a preprocessing step before releasing the files
        for(int i=0, mx=unpacked.size(); i<mx; i++) {
            Model mdl = unpacked.get(i);
            // put the model into the pack
            pack.models.put(mdl.name, mdl);
            // the id we will use for generated material names
            int matid = 1;

            for(int lod=0; lod<mdl.modelLod.size(); lod++) {
                FastList<ModelPart> mlod = mdl.modelLod.get(lod);
                for(int mp=0; mp<mlod.size(); mp++) {
                    ModelPart mprt = mlod.get(mp);
                    // before counting parts, generate tangents and binormals
                    checkTangentBinormal(mprt);

                    // process this part, replacing data in the modelpart
                    countParts(mprt);
                    // get the material of the part
                    ModelMaterial mm = mprt.modelMaterial;
                    if( mm !=null) {
                        if( mm.id == null) {
                            // assign an id to this material
                            mm.setId(mdl.name + "_" + matid);
                            matid++;
                        }
                        // put the material into the map for the pack
                        mdl.materials.put(mm.id, mm);
                    }
                }
            }
        }
        
        // process all the retrieved modelpackparts
        for(ModelPartPack mpprt: pack.createdPacks.values()) {
            processPart(mpprt);
        }
        return pack;
    }

    protected void checkTangentBinormal(ModelPart mprt) {
        // fo all the bumpmapped/normalmapped geometryes
        ModelMaterial mm = mprt.getMaterial();
        if(mm.hasBumpMap() || mm.hasNormalMap()) {
            // does the geometry has tangents and binormals?
            if(mprt.geom.getAttribBuffer(VertexAttribute.Usage.Binormal) == null || mprt.geom.getAttribBuffer(VertexAttribute.Usage.Tangent)==null) {
                //k, we need to generate tangents
                TangentGenerator tg = new TangentGenerator();
                // TODO: default 0 texcoord unit
                mprt.geom = tg.generateTangents(mprt.geom, 0);
                mprt.geom.setDisplayListMode(Geometry.LIST_NO);
                mprt.geom.setVBOMode(Geometry.VBO_LONGLIVED);
            }
        }
        
    }
    
    private void countParts(ModelPart mprt) {
        // get geometry from the part
        Geometry geom = mprt.geom;
        // make a signature of the contained vertex attributes
        FastList<VertexBuffer> buffers = geom.getBuffers();
        long signature = 0;
        
        // get all buffers in the geometry
        for(int i=0; i<buffers.size(); i++) {
            VertexBuffer vb = buffers.get(i);
            // get all the attributes in this buffer
            FastList<VertexAttribute> attrS = vb.getFormat().getAttributes();
            for(int j=0; j<attrS.size(); j++) {
                VertexAttribute atr = attrS.get(j);
                signature = VertexFormat.setRequested(signature, atr.type);
            }
        }
        
        // request the desired format
        VertexFormat vfm = VertexFormat.getDefaultFormat(signature);
        // see if we already have a pack for that format?
        int vbomode = geom.getVBOMode();

        ModelPartPack partpack = pack.getPartPack(vfm, vbomode);
        // add the number of vertices
        partpack.vertices += geom.getNumVertex();
        // add the number of indices to the counter that will hold
        // indices for this part
        if( IndexBuffer.isShortBufferPossible(geom.getNumVertex()) ) {
            partpack.indicess += geom.getNumIndex();
            mprt.intIndex = false;
        } else {
            partpack.indicesi += geom.getNumIndex();
            // using int-s for indices
            mprt.intIndex = true;
        }
        // add the part to be processed
        partpack.parts.add(mprt);
        // store the name of part pack into the part
        mprt.partpack = partpack.name;
    }

    private void processPart( ModelPartPack mprt ) {

        // create the index for attributes
        // the map of all attributes
        VertexFormat vf = mprt.format;
        FastList<VertexAttribute> patr = vf.getAttributes();

        // create the vertex buffer of proper size
        mprt.vb = new VertexBuffer();
        mprt.vb.setFormat(vf);
        mprt.vb.setVertexCount(mprt.vertices);
        if(mprt.VBOMode>0) {
            VBOAttributeInfo vbi = new VBOAttributeInfo();
            //vbi.useVBO = true;
            mprt.vb.setVBOInfo(vbi);
        }

        // create the buffer to hold all the vertices
        FloatBuffer vertBuf = mprt.vb.createDataBuffer();
        
        // create the indexbuffers of proper size
        if( mprt.indicesi > 0) {
            // create the int index buffer
            mprt.ibi = IndexBuffer.createIntBuffer(mprt.indicesi, null);
            if(mprt.VBOMode>0) {
                VBOAttributeInfo vbi = new VBOAttributeInfo();
                //vbi.useVBO = true;
                mprt.ibi.setVBOInfo(vbi);
            }
        }
        if( mprt.indicess > 0) {
            // create the short index buffer
            mprt.ibs = IndexBuffer.createShortBuffer(mprt.indicess, null);
            if(mprt.VBOMode>0) {
                VBOAttributeInfo vbi = new VBOAttributeInfo();
                //vbi.useVBO = true;
                mprt.ibs.setVBOInfo(vbi);
            }
        }
        
        // the counters for vertices and indices
        int startVertex = 0;
        int startIndexS = 0;
        int startIndexI = 0;
        
        // the stride for the target buffer
        int stride = mprt.vb.getFormat().getSize();

        // go over each model and put it into the proper buffer
        for(int i=0; i<mprt.parts.size(); i++) {
            ModelPart part = mprt.parts.get(i);
            // get geometry from the part
            Geometry geom = part.geom;
            // get the number of vertices and indices
            part.numVertex = geom.getNumVertex();
            part.numIndex = geom.getNumIndex();
            
            // get the list mode
            part.listMode = geom.getDisplayListMode();
            // the model bound
            part.bound = geom.getModelBound();
            // the collision tree
            if(geom.getCollisionTree()!=null)
                part.hasColTree = true;
            if(geom.getCollisionVolume()!=null)
                part.hasColVolume = true;
            // the bone mapping
            if(geom instanceof XWeightedGeometry) {
                part.boneMapping = ((XWeightedGeometry)geom).getBoneMapping();
            }

            // vertex start position for the part
            part.startVertex = startVertex;
            // increase the start vertex by vertex count
            startVertex += part.numVertex;
            // use either the short or int indexbuffer?
            IndexBuffer idx;
            if( !part.intIndex ) {
                // the short buffer
                idx = mprt.ibs;
                part.startIndex = startIndexS;
                startIndexS += part.numIndex;
            } else {
                // use int indexbuffer
                idx = mprt.ibi;
                part.startIndex = startIndexI;
                startIndexI += part.numIndex;
            }

            // make a map of the contained vertex attributes
            FastList<VertexBuffer> buffers = geom.getBuffers();
            // the start vertex for buffers
            IntList bufferStartVertex = geom.getBufferStartVertex();
            
            // the map of all attributes in that buffer
            VertexAttribute[] attr = new VertexAttribute[VertexAttribute.USAGE_MAX];
            // the map of all vertex buffers a given attribute is in
            VertexBuffer[] bfr = new VertexBuffer[VertexAttribute.USAGE_MAX];
            // the map of all start vertices based on attribute
            int[] attrStartVertex = new int[VertexAttribute.USAGE_MAX];

            for(int j=0; j<buffers.size(); j++) {
                VertexBuffer vb = buffers.get(j);
                // get all the attributes in this buffer
                FastList<VertexAttribute> attrS = vb.getFormat().getAttributes();
                for(int k=0; k<attrS.size(); k++) {
                    VertexAttribute atr = attrS.get(k);
                    // put the attribute into map
                    attr[atr.type.id] = atr;
                    bfr[atr.type.id] = vb;
                    attrStartVertex[atr.type.id] = bufferStartVertex.get(j);
                }
            }
            
            // go over all the vertices, and weave them into the new buffer
            for(int vn=0; vn<part.numVertex; vn++) {
                int fstartDst = (vn+part.startVertex) * stride;
                //int vstartSrc = part.geom.getStartVertex()+vn;
                // go over all the target attributes
                for(int ac=0; ac<patr.size(); ac++ ) {
                    // we need this attribute
                    VertexAttribute vaDst = patr.get(ac);
                    int an = vaDst.type.id;
                    
                    // find the buffer that it is in the source
                    FloatBuffer fbSrc = bfr[an].getDataBuffer();
                    int strideSrc = bfr[an].getFormat().getSize();
                    // find the position that the data is in source
                    VertexAttribute vaSrc = attr[an];
                    // how many floats we transfer?
                    int num = vaSrc.floats;
                    // destination postion
                    vertBuf.position(fstartDst + vaDst.startfloat);
                    // source position
                    fbSrc.position((attrStartVertex[vaSrc.type.id] +vn)* strideSrc + vaSrc.startfloat);
                    // copy data
                    for(int fcount = 0; fcount < num; fcount++) {
                        vertBuf.put(fbSrc.get());
                    }
                }
            }
            
            // transfer the indices to the proper index buffer
            idx.position(part.startIndex);
            IndexBuffer srcidx = geom.getIndexBuffer();
            srcidx.position(geom.getStartIndex());
            idx.put(srcidx, part.numIndex);
            
            // change the part
            
            Geometry cg = null;
            if(part.boneMapping != null) {
                cg = new XWeightedGeometry();
                ((XWeightedGeometry)cg).setBoneMapping(part.boneMapping);
            } else {
                cg = new Geometry();
            }
            cg.addAttribBuffer(mprt.vb, part.startVertex);
            //cg.setStartVertex(part.startVertex);
            cg.setNumVertex(part.numVertex);
            cg.setVBOMode(mprt.VBOMode);
            if(mprt.VBOMode==0) {
                cg.setDisplayListMode(part.listMode);
            }
            
            cg.setIndexBuffer(idx);
            cg.setStartIndex(part.startIndex);
            cg.setNumIndex(part.numIndex);
            if(part.bound!=null) {
                cg.setModelBound(part.bound);
            } else {
                part.bound = new BoundingBox();
                cg.setModelBound(part.bound);
                cg.updateModelBound();
            }
            part.geom = cg;
        }
    }
}
