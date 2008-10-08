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

package com.vlengine.util.geom;

import com.vlengine.renderer.VBOAttributeInfo;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class VertexBuffer {
    
    // the actual data in this buffer
    FloatBuffer data;
    
    // the data types contained in this buffer
    VertexFormat format;
        
    // the number of vertices in the buffer
    int numVertex;
    
    // the vbo info for this buffer
    private VBOAttributeInfo vboInfo;
    
    public int getVertexLenght() {
        return format.getBytes();
    }
    
    public FloatBuffer createDataBuffer() {
        data = BufferUtils.createFloatBuffer( numVertex * format.getSize() );
        data.clear();
        return data;
    }
    
    public FloatBuffer getDataBuffer() {
            return data;
    }

    public void setDataBuffer(FloatBuffer vertBuf) {
        this.data = vertBuf;
        if (data != null)
            numVertex = data.limit() / format.getSize();
        else
            numVertex = 0;
    }

    public void removeDataBuffer() {
        this.data = null;
    }
    
    public int getVertexCount() {
            return numVertex;
    }

    public void setVertexCount(int vertQuantity) {
            this.numVertex = vertQuantity;
    }
    
    public void setFormat(VertexFormat format) {
        this.format = format;
    }
    
    public VertexFormat getFormat() {
        return format;
    }
    
    public void setVBOInfo( VBOAttributeInfo info ) {
        vboInfo = info;
    }
    
    public VBOAttributeInfo getVBOInfo() {
            return vboInfo;
    }
    
    public static VertexBuffer createSingleBuffer(VertexAttribute.Usage attributeType, int vertCount) {
        VertexBuffer vb = new VertexBuffer();
        VertexFormat vtxFormat = VertexFormat.getDefaultFormat(VertexFormat.setRequested(attributeType));
        vb.setFormat(vtxFormat);
        vb.setVertexCount(vertCount);
        vb.createDataBuffer();
        return vb;
    }
    
    public FloatBuffer extractAttributeDataBuffer(VertexAttribute.Usage attributeType, int startVertex, int numVertex, FloatBuffer store) {
        FloatBuffer fb = getDataBuffer();
        if(fb==null) {
            if(store!=null) {
                store.clear();
                store.limit(0);
            }
            return store;
        }
        // get the position data in the buffer
        VertexAttribute posAtt = getFormat().getAttribute(attributeType);
        // if the buffer does not contain position data, return
        if( posAtt == null) {
            if(store!=null) {
                store.clear();
                store.limit(0);
            }
            return store;
        }
        
        // determine the postion and stride, in floats
        int stride = getFormat().getSize();
        // the start, in floats
        int start = posAtt.startfloat;
        
        fb.position(start + startVertex*stride);
        
        if (fb.remaining() <= posAtt.floats) {// we dont have enugh data
            if(store!=null) {
                store.clear();
                store.limit(0);
            }
            return store;
        }

        // ensure that the buffer is enough to hold all the data
        int numFloats = numVertex*posAtt.floats;
        
        FloatBuffer buf = store;
        if(buf == null || buf.capacity() < numFloats) {
            buf = BufferUtils.createFloatBuffer(numFloats);
        }
        
        buf.clear();
        // go over and extract all the points
        
        int len = fb.remaining() / stride;
        if( len > numVertex)
            len = numVertex;
        for (int i = 0; i < len; i++) {
            int index = (startVertex+ i)*stride + start;
            for(int j=0; j<posAtt.floats; j++) {
                buf.put(fb.get(index+j));
            }
        }
        buf.rewind();
        return buf;
    }
}
