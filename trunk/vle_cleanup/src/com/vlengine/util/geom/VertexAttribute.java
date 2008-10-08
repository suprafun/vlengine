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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class VertexAttribute {

    public static enum Usage {
        Position("p", 0, 3*4, GL11.GL_VERTEX_ARRAY),
        Normal("n", 1, 3*4, GL11.GL_NORMAL_ARRAY),
        Color("c", 2, 4*4, GL11.GL_COLOR_ARRAY),
        Fog("f", 3, 1*4, GL14.GL_FOG_COORDINATE_ARRAY),
        Texture0uv("t0uv", 4, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture1uv("t1uv", 5, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture2uv("t2uv", 6, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture3uv("t3uv", 7, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture4uv("t4uv", 8, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture5uv("t5uv", 9, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture6uv("t6uv", 10, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        Texture7uv("t7uv", 11, 2*4, GL11.GL_TEXTURE_COORD_ARRAY),
        // TODO: add 3 componenet texcoords usages
        
        Tangent("tn", 12, 3*4),
        Binormal("bn", 13, 3*4),
        BinormalFlip("bf", 14, 1*4),
        Weight("wt", 15, 4*4),
        WeightIndex("wx", 16, 1*4),
        WeightNumber("wn", 17, 1*4),
                
        ;
        // code name of the attribute
        public final String name;
        // the id used in arrays
        public final int id;
        // the bytes this attribute takes
        public final int bytes;
        // the floats this attribute type takes
        public final int floats;
        // the OpenGL state
        public final int glArrayState;
        // the texture coord numberm if this is a texcoord
        public final int texcoordNo;
        // is this a shader attribute?
        public final boolean isShaderAttrib;

        Usage(String nam, int i, int byt, int glArrayStat) {
            name = nam;
            id = i;
            bytes = byt;
            floats = byt / 4;
            glArrayState = glArrayStat;
            if(glArrayState == GL11.GL_TEXTURE_COORD_ARRAY) {
                // TODO: hhardcoded id of Texture0uv
                texcoordNo = id - 4;
            } else {
                texcoordNo = -1;
            }
            isShaderAttrib = false;
        }

        Usage(String nam, int i, int byt) {
            name = nam;
            id = i;
            bytes = byt;
            floats = byt / 4;
            // this is shader attribute, id does not have an array state OGL name
            glArrayState = -1;
            if(glArrayState == GL11.GL_TEXTURE_COORD_ARRAY) {
                // TODO: hhardcoded id of Texture0uv
                texcoordNo = id - 4;
            } else {
                texcoordNo = -1;
            }
            isShaderAttrib = true;
        }
        
        public static int getQuantity() {
            return values().length;
        }
        
        public static Usage getById(int id) {
            return Usage.values()[id];
        }
    }
    
    public static final Usage USAGE_POSITION = Usage.Position;
    public static final Usage USAGE_NORMAL = Usage.Normal;
    public static final Usage USAGE_COLOR = Usage.Color;
    public static final Usage USAGE_FOG = Usage.Fog;
    public static final Usage USAGE_TEXTURE0 = Usage.Texture0uv;
    public static final Usage USAGE_TANGENT = Usage.Tangent;
    public static final Usage USAGE_BINORMAL = Usage.Binormal;
    //public static final Usage USAGE_BINORMAL_FLIP = USAGE_TEXTURE7 + 3;
    public static final Usage USAGE_WEIGHTS = Usage.Weight;
    public static final Usage USAGE_WEIGHTINDICES = Usage.WeightIndex;
    public static final Usage USAGE_NUMWEIGHTS = Usage.WeightNumber;
    
    
    public static final int USAGE_MAX = Usage.getQuantity();
    
/*    
    public static final String[] DEFAULT_ATTRIB_NAMES = new String[] {
        "p",
        "n",
        "c",
        "f",
        "t0x",
        "t1x",
        "t2x",
        "t3x",
        "t4x",
        "t5x",
        "t6x",
        "t7x",
        "tn",
        "bn",
        "bf",
        "wt",
        "wx",
        "wn"
    };
    
    // length of attributes in bytes
    public static final int[] DEFAULT_ATTRIB_BYTES = new int[] {
        3*4, // position
        3*4, // normal
        4*4, // color
        1*4, // fog
        2*4, // texture 0
        2*4, // texture 1
        2*4, // texture 2
        2*4, // texture 3
        2*4, // texture 4
        2*4, // texture 5
        2*4, // texture 6
        2*4, // texture 7
        3*4, // tangent
        3*4, // binormal
        1*4, // binormal flip
        4*4, // weigth
        1*4, // weight index
        1*4  // number of weights
    };
    
    public static final int ARRAY_STATE[] = new int[] { 
        GL11.GL_VERTEX_ARRAY,
        GL11.GL_NORMAL_ARRAY,
        GL11.GL_COLOR_ARRAY,
        GL14.GL_FOG_COORDINATE_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY,
        GL11.GL_TEXTURE_COORD_ARRAY
    };
    
    public static final int USAGE_POSITION = 0;
    public static final int USAGE_NORMAL = 1;
    public static final int USAGE_COLOR = 2;
    public static final int USAGE_FOG = 3;
    public static final int USAGE_TEXTURE0 = 4;
    
    public static final int USAGE_TEXTURE7 = USAGE_TEXTURE0 + 7;

    public static final int USAGE_TANGENT = USAGE_TEXTURE7 + 1;
    public static final int USAGE_BINORMAL = USAGE_TEXTURE7 + 2;
    public static final int USAGE_BINORMAL_FLIP = USAGE_TEXTURE7 + 3;
    public static final int USAGE_WEIGHTS = USAGE_TEXTURE7 + 4;
    public static final int USAGE_WEIGHTINDICES = USAGE_TEXTURE7 + 5;
    public static final int USAGE_NUMWEIGHTS = USAGE_TEXTURE7 + 6;
    
    public static final int USAGE_MAX = USAGE_NUMWEIGHTS + 1;
*/
    
    // usage type of the attribute
    public final Usage type;
    // the lenght of the attribute in floats
    public final int floats;
    // the start of the attribute in terms of floats
    public final int startfloat;
    
    // TODO: remove reference to these
    
    // start offset of attribute
    public final int startbyte;
    // the length of this attribute in bytes
    public final int bytes;


    // TODO: remove reference to this constructor
/*    
    public VertexAttribute(Usage t, int c, int s, int l) {
        type = t;
        floats = c;
        startfloat = s/4;
        
        startbyte = s;
        bytes = l;
    }
 */
    
    public VertexAttribute(Usage type, int startFloat) {
        this.type = type;
        floats = type.floats;
        this.startfloat = startFloat;
        
        startbyte = startFloat*4;
        bytes = type.bytes;
    }
    
}
