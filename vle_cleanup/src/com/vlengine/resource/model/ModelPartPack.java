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

import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.resource.FileResource;
import com.vlengine.resource.ParameterMap;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.IndexBufferInt;
import com.vlengine.util.geom.IndexBufferShort;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import com.vlengine.util.xml.Element;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashSet;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class ModelPartPack {
    // the (file) name of the modelpack
    // there are multiple files associated with the pack:
    //      _v.dat for vertex attributes data (VertexBuffer)
    //          interleaved vertex attributes
    //      _is.dat or _ii.dat for vertex indices data (IndexBuffer)
    //              
    //      modelpack information
    //              name
    //              display list mode
    //              VBO mode
    //              vertex format
    //              

    protected String name;

    // the vertex format for models in this pack
    protected VertexFormat format;
    
    // VBO mode, only objects with same VBO mode can be put into a modelpack
    // note that static geometry (display lists), cannot have VBO enabled
    // if geometry is to be put into display lists, use VBOMode 0
    protected int VBOMode;
    
    protected VertexBuffer vb;
    
    // the index buffer with short indices
    protected IndexBufferShort ibs;
    // the index buffer with int indices
    protected IndexBufferInt ibi;
    
    // the total number of vertices
    protected int vertices = 0;
    // the number of short indices
    protected int indicess = 0;
    // the number of int indices
    protected int indicesi = 0;
    
    // the model parts to be processed in this pack
    protected FastList<ModelPart> parts = new FastList<ModelPart>();

    // the file names for this pack
    // 0-vertices
    // 1-indices short
    // 2-indices int
    String[] fileNames = new String[3];
    
    public ModelPartPack() {
    }

    public void save(Element parent, String path) {
        fileNames[0] =null; fileNames[1] =null; fileNames[2] =null;
        
        parent.addContent(new Element("name").setText(name));
        parent.addContent(new Element("format_signature").setText(format.getBinarySignature()));
        parent.addContent(new Element("vbo_mode").setText(VBOMode));
        // save the vertices
        String filename = name + "_v.dat";
        parent.addContent(new Element("vertices").setText(filename));
        FloatBuffer fb = vb.getDataBuffer();
        fb.rewind();
        // if the target folder does not exists, create it
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        
        String filepath = path+"/"+filename;
        if(!FileResource.save(filepath, fb)) {
            // could not save file
            throw new VleException("Could not save file "+filepath);
        }
        
        fileNames[0] = filepath;
        
        // the short index buffer
        if(ibs != null) {
            filename = name + "_is.dat";
            parent.addContent(new Element("indices_short").setText(filename));
            ShortBuffer sb = (ShortBuffer) ibs.getBuffer();
            sb.rewind();
            filepath = path+"/"+filename;
            if(!FileResource.save(filepath, sb)) {
                throw new VleException("Could not save file "+filepath);
            }
            
            fileNames[1] = filepath;
        }
        // the int index buffer
        if(ibi != null) {
            filename = name + "_ii.dat";
            parent.addContent(new Element("indices_int").setText(filename));
            IntBuffer sb = (IntBuffer) ibi.getBuffer();
            sb.rewind();
            filepath = path+"/"+filename;
            if(!FileResource.save(filepath, sb)) {
                throw new VleException("Could not save file "+filepath);
            }
            
            fileNames[2] = filepath;
        }
    }
    
    public void load(Element parent, String path) {
        fileNames[0] =null; fileNames[1] =null; fileNames[2] =null;

        name = parent.getChildText("name");
        long vsig = parent.getChild("format_signature").getTextlong();
        format = VertexFormat.getDefaultFormat(vsig);
        VBOMode = parent.getChildint("vbo_mode");
        // load the vertices
        String filename = parent.getChildText("vertices");
        String filepath = path+"/"+filename;
        fileNames[0] = filepath;

        // TODO: handle preload VBO (mapped VBO)
        FloatBuffer fb = FileResource.loadFloatBuffer(filepath, ParameterMap.DIRECTBUFFER);
        if( fb == null ) {
            throw new VleException("Could not load file "+filepath);
        }
        // number of vertices in the pack
        vertices = fb.limit() / format.getSize();
        // create the vertex buffer
        vb = new VertexBuffer();
        vb.setFormat(format);
        vb.setDataBuffer(fb);
        
        // set VBO if needed
        if(VBOMode != 0) {
            VBOAttributeInfo vi = new VBOAttributeInfo();
            //vi.useVBO = true;
            vb.setVBOInfo(vi);
        }

        // load indices short
        filename = parent.getChildText("indices_short");
        if(filename != null ) {
            filepath = path+"/"+filename;
            // store the file name for later
            fileNames[1] = filepath;

            ShortBuffer sb = FileResource.loadShortBuffer(filepath, ParameterMap.DIRECTBUFFER);
            if( sb == null) {
                throw new VleException("Could not load file "+filepath);
            }
            indicess = sb.limit();
            ibs = IndexBuffer.createBuffer(sb, ibs);
            if(VBOMode != 0) {
                VBOAttributeInfo vi = new VBOAttributeInfo();
                //vi.useVBO = true;
                ibs.setVBOInfo(vi);
            }
        }

        // load indices int
        filename = parent.getChildText("indices_int");
        if(filename != null ) {
            filepath = path+"/"+filename;
            
            // store the file name for later
            fileNames[2] = filepath;
            
            IntBuffer sb = FileResource.loadIntBuffer(filepath, ParameterMap.DIRECTBUFFER);
            if( sb == null ) {
                throw new VleException("Could not load file "+filepath);
            }
            indicesi = sb.limit();
            ibi = IndexBuffer.createBuffer(sb, ibi);
            if(VBOMode != 0) {
                VBOAttributeInfo vi = new VBOAttributeInfo();
                //vi.useVBO = true;
                ibi.setVBOInfo(vi);
            }
        }
    }

    public HashSet<String> getFiles(HashSet<String> fls) {
        if(fls == null ) {
            fls = new HashSet<String>();
        }
        
        if(fileNames[0]!=null) {
            fls.add(fileNames[0]);
        }
        if(fileNames[1]!=null) {
            fls.add(fileNames[1]);
        }
        if(fileNames[2]!=null) {
            fls.add(fileNames[2]);
        }
        
        return fls;
    }
}
