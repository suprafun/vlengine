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

package com.vlengine.resource.obj;

import com.vlengine.bounding.BoundingBox;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.Geometry;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelMaterial;
import com.vlengine.resource.model.ModelPart;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ObjModel {

    /** Every vertex in the file */
    protected ArrayList<Vector3f> vertexList = new ArrayList<Vector3f>();
    /** Every texture coordinate in the file */
    protected ArrayList<Vector2f> textureList = new ArrayList<Vector2f>();
    /** Every normal in the file */
    protected ArrayList<Vector3f> normalList = new ArrayList<Vector3f>();
    /** Generated normals */
    protected ArrayList<Vector3f> genNormalList = new ArrayList<Vector3f>();
    /** Last 'Object' name in the file */
    protected String curObjectName = null;
    /** Default material group for groups without a material */
    protected ModelMaterial defaultMaterialGroup;

    /** Maps Materials to their vertex usage * */
    protected HashMap<ModelMaterial, ArraySet> materialSets = new HashMap<ModelMaterial, ArraySet>();
    // the parameters for creating the object
    protected ParameterMap params;
    // the material library
    protected ObjMtlLib mllib;
    
    public ObjModel(ParameterMap params) {
        this.params = new ParameterMap();
        this.params.putAll(params);
        
        defaultMaterialGroup = new ModelMaterial();
        vertexList.clear();
        textureList.clear();
        normalList.clear();
        genNormalList.clear();
        materialSets.clear();
        
        materialSets.put(defaultMaterialGroup, new ArraySet());
    }
    
    public String getName() {
        return curObjectName;
    }
    
    public void setName(String name) {
        curObjectName = name;
    }
    
    private void nullAll(){
        vertexList.clear();
        textureList.clear();
        normalList.clear();
        genNormalList.clear();
        materialSets.clear();

        defaultMaterialGroup = null;
    }
    
    /**
     * Converts the structures of the .obj file to a scene to write
     * 
     * @return The TriMesh or Node that represents the .obj file.
     */
    public Model buildModel() {
        int vbomode = params.getInt("vbomode", 0);
        int listmode = params.getInt("listmode", 0);
        
        Model mdl = new Model();
        
        Object[] o = materialSets.keySet().toArray();
        for (int i = 0; i < o.length; i++) {
            ModelMaterial thisGroup = (ModelMaterial) o[i];
            ArraySet thisSet = materialSets.get(thisGroup);
            if (thisSet.indexes.size() < 3)
                continue;

            Vector3f[] vert = new Vector3f[thisSet.sets.size()];
            Vector3f[] norm = new Vector3f[vert.length];
            Vector2f[] text = new Vector2f[vert.length];
            boolean hasNorm = false, hasTex = false;

            int j = 0;
            for (IndexSet set : thisSet.sets) {
                vert[j] = vertexList.get(set.vIndex);
                if (set.nIndex >= 0) {
                    norm[j] = normalList.get(set.nIndex);
                    hasNorm = true;
                } else if (set.nIndex < -1) {
                    norm[j] = genNormalList.get((-1*set.nIndex)-2);
                    hasNorm = true;
                }
                if (set.tIndex >= 0) {
                    text[j] = textureList.get(set.tIndex);
                    hasTex = true;
                }
                j++;
            }

            int[] indexes = new int[thisSet.indexes.size()];
            for (j = 0; j < thisSet.indexes.size(); j++)
                indexes[j] = thisSet.indexes.get(j);

            // create the geometry
            Geometry geom = new Geometry();

            // create the vertex buffer
            VertexBuffer vertBuf = new VertexBuffer();
            VertexFormat vertFormat = VertexFormat.getDefaultFormat(
                    VertexFormat.setRequested(VertexAttribute.USAGE_POSITION));
            vertBuf.setFormat(vertFormat);
            vertBuf.setDataBuffer(BufferUtils.createFloatBuffer(vert));
            if(vbomode> 0) {
                VBOAttributeInfo vertVBO = new VBOAttributeInfo();
                //vertVBO.useVBO = true;
                vertBuf.setVBOInfo(vertVBO);
            }
            geom.addAttribBuffer(vertBuf, 0);

            if(hasNorm) {
                // create the normal buffer
                VertexBuffer normBuf = new VertexBuffer();
                VertexFormat normFormat = VertexFormat.getDefaultFormat(
                        VertexFormat.setRequested(VertexAttribute.USAGE_NORMAL));
                normBuf.setFormat(normFormat);
                normBuf.setDataBuffer(BufferUtils.createFloatBuffer(norm));
                if(vbomode> 0) {
                    VBOAttributeInfo normVBO = new VBOAttributeInfo();
                    //normVBO.useVBO = true;
                    normBuf.setVBOInfo(normVBO);
                }
                    
                geom.addAttribBuffer(normBuf, 0);
            }

            if(hasTex) {
                // create texture buffer
                VertexBuffer texBuf = new VertexBuffer();
                VertexFormat texFormat = VertexFormat.getDefaultFormat(
                        VertexFormat.setRequested(VertexAttribute.Usage.Texture0uv));
                texBuf.setFormat(texFormat);
                texBuf.setDataBuffer(BufferUtils.createFloatBuffer(text));
                if(vbomode> 0) {
                    VBOAttributeInfo texVBO = new VBOAttributeInfo();
                    //texVBO.useVBO = true;
                    texBuf.setVBOInfo(texVBO);
                }
                geom.addAttribBuffer(texBuf, 0);
            }

            // create index buffer
            IndexBuffer idx = IndexBuffer.createBuffer(indexes, vert.length, null);
            idx.clear();

            if(vbomode> 0) {
                VBOAttributeInfo idxVBO = new VBOAttributeInfo();
                //idxVBO.useVBO = true;
                idx.setVBOInfo(idxVBO);
            }

            geom.setIndexBuffer(idx);

            // this has to be done before assigning states,
            // because the bumpHandler might add duplicate vertices to
            // relief the tangent space
            if (params.getBoolean("minimizeVertices", false)) {
                // this is good for static geometry that is in one chunk
                // its not good for animated or models with bones
                // and also bad for objects that extend too much in one direction
                //GeometryTool.minimizeVerts(mesh,
                //		GeometryTool.MV_SAME_COLORS
                //		| GeometryTool.MV_SAME_NORMALS
                //		| GeometryTool.MV_SAME_TEXS);
            }

            geom.setModelBound(new BoundingBox());
            // implemet it for CompositeGeometry
            geom.updateModelBound();

            // VBO and display list options
            geom.setVBOMode(vbomode);
            if(vbomode == 0)
                geom.setDisplayListMode(listmode);
                
            // create the modelpart
            ModelPart mprt = new ModelPart();
            mprt.setGeometry(geom);
            mprt.setMaterial(thisGroup);
            
            // put it into the Model into lod 0
            mdl.addPart(0, mprt);
        }
        return mdl;
    }
}
