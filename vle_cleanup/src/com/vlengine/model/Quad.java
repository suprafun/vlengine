/*
 * Copyright (c) 2003-2007 jMonkeyEngine, 2008 VL Engine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
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

package com.vlengine.model;

import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.nio.FloatBuffer;

/**
 * 
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Quad extends Geometry {


    public Quad() {
        
    }

    public Quad(float width, float height) {
                long format = 0;
                format = VertexFormat.setRequested(VertexAttribute.USAGE_POSITION);
                format = VertexFormat.setRequested(format, VertexAttribute.USAGE_NORMAL);
                format = VertexFormat.setRequested(format, VertexAttribute.USAGE_TEXTURE0);
                
		initialize(width, height, format, true);
	}
    
	/**
	 * Constructor creates a new <code>Quade</code> object with the provided
	 * width and height.
	 * 
	 * @param width
	 *            the width of the <code>Quad</code>.
	 * @param height
	 *            the height of the <code>Quad</code>.
	 */
	public Quad(float width, float height, long format, boolean interleave) {
		initialize(width, height, format, interleave);
	}

	/**
	 * <code>resize</code> changes the width and height of the given quad by
	 * altering its vertices.
	 * 
	 * @param width
	 *            the new width of the <code>Quad</code>.
	 * @param height
	 *            the new height of the <code>Quad</code>.
	 */
	public void resize(float width, float height) {
            VertexBuffer buf = this.getAttribBuffer(VertexAttribute.USAGE_POSITION);
            FloatBuffer vbf = buf.getDataBuffer();
            int stride = buf.getFormat().getSize();
            int pos = this.startVertexIndex[VertexAttribute.USAGE_POSITION.id]
                    + buf.getFormat().getAttribute(VertexAttribute.USAGE_POSITION).startbyte;

            vbf.position(pos);
            vbf.put(-width / 2f).put(height / 2f).put(0);
            pos+=stride;
            vbf.position(pos);
            vbf.put(-width / 2f).put(-height / 2f).put(0);
            pos+=stride;
            vbf.position(pos);
            vbf.put(width / 2f).put(-height / 2f).put(0);
            pos+=stride;
            vbf.position(pos);
            vbf.put(width / 2f).put(height / 2f).put(0);
            vbf.position(0);
	}

    /**
     * 
     * <code>initialize</code> builds the data for the <code>Quad</code>
     * object.
     * 
     * 
     * @param width
     *            the width of the <code>Quad</code>.
     * @param height
     *            the height of the <code>Quad</code>.
     */
    public void initialize(float width, float height, long format, boolean interleave) {
        
        boolean position = VertexFormat.isRequested(format, VertexAttribute.USAGE_POSITION);
        boolean normal = VertexFormat.isRequested(format, VertexAttribute.USAGE_NORMAL);
        boolean tex0 = VertexFormat.isRequested(format, VertexAttribute.USAGE_TEXTURE0);

        FloatBuffer vertBuf=null;
        FloatBuffer normBuf=null;
        FloatBuffer tex0Buf=null;

        int vertCount = 4;
        
        if(interleave) {
            VertexBuffer vb=new VertexBuffer();
            VertexFormat vtxFormat = VertexFormat.getDefaultFormat(format);
            vb.setFormat(vtxFormat);
            vb.setVertexCount(vertCount);
            // we have 4 vertices, and each is 32 bytes long, or /4 in float length
            FloatBuffer buf = vb.createDataBuffer();
                    //BufferUtils.createFloatBuffer(vertCount*vtxFormat.getSize());
            vb.setDataBuffer(buf);
            addAttribBuffer(vb, 0);
            if(position)
                vertBuf = buf;
            if(normal)
                normBuf = buf;
            if(tex0)
                tex0Buf = buf;
        } else {
            // not interleaved
            VertexFormat vtxFormat;
            VertexBuffer vb;
            if(position) {
                // position
                vtxFormat = VertexFormat.getDefaultFormat(VertexFormat.setRequested(0, VertexAttribute.USAGE_POSITION));
                vb=new VertexBuffer();
                vb.setFormat(vtxFormat);
                vb.setVertexCount(vertCount);
                vertBuf = vb.createDataBuffer();
                //vertBuf = BufferUtils.createFloatBuffer(4*(vtxFormat.getLength()/4));
                vb.setDataBuffer(vertBuf);
                addAttribBuffer(vb, 0);
            }
            
            if(normal) {
                vtxFormat = VertexFormat.getDefaultFormat(VertexFormat.setRequested(0, VertexAttribute.USAGE_NORMAL));
                vb=new VertexBuffer();
                vb.setFormat(vtxFormat);
                normBuf = BufferUtils.createFloatBuffer(4*vtxFormat.getSize());
                vb.setDataBuffer(normBuf);
                addAttribBuffer(vb, 0);
            }
            if(tex0) {
                vtxFormat = VertexFormat.getDefaultFormat(VertexFormat.setRequested(0, VertexAttribute.USAGE_TEXTURE0));
                vb=new VertexBuffer();
                vb.setFormat(vtxFormat);
                tex0Buf = BufferUtils.createFloatBuffer(4*vtxFormat.getSize());
                vb.setDataBuffer(tex0Buf);
                addAttribBuffer(vb, 0);
            }
        }

        // we only need a short buffer for indices
        IndexBuffer indexBuffer = IndexBuffer.createBuffer(2 * 3, vertCount, null);

        setIndexBuffer(indexBuffer);

        if(vertBuf!=null)
            vertBuf.clear();
        if(normBuf!=null)
            normBuf.clear();
        if(tex0Buf!=null)
            tex0Buf.clear();

        // vertex 0
        if(vertBuf!=null)
            vertBuf.put(-width / 2f).put(height / 2f).put(0);
        // normal
        if(normBuf!=null)
            normBuf.put(0).put(0).put(1);
        // texture
        if(tex0Buf!=null)
            tex0Buf.put(0).put(1);

        if(vertBuf!=null)
            vertBuf.put(-width / 2f).put(-height / 2f).put(0);
        if(normBuf!=null)
            normBuf.put(0).put(0).put(1);
        if(tex0Buf!=null)
            tex0Buf.put(0).put(0);

        if(vertBuf!=null)
            vertBuf.put(width / 2f).put(-height / 2f).put(0);
        if(normBuf!=null)
            normBuf.put(0).put(0).put(1);
        if(tex0Buf!=null)
            tex0Buf.put(1).put(0);

        if(vertBuf!=null)
            vertBuf.put(width / 2f).put(height / 2f).put(0);
        if(normBuf!=null)
            normBuf.put(0).put(0).put(1);
        if(tex0Buf!=null)
            tex0Buf.put(1).put(1);
        
        if(vertBuf!=null)
            vertBuf.rewind();
        if(normBuf!=null)
            normBuf.rewind();
        if(tex0Buf!=null)
            tex0Buf.rewind();
        
        indexBuffer.clear();
        indexBuffer.put(0);
        indexBuffer.put(1);
        indexBuffer.put(2);
        indexBuffer.put(0);
        indexBuffer.put(2);
        indexBuffer.put(3);
        indexBuffer.rewind();
    }

}