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

import java.util.logging.Logger;

/**
 * @author Mark Powell
 * @author Joshua Slack
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class Circle extends Geometry {
    
    public Circle() {
        mode = Circle.LINE_SEGMENTS;
        isCollidable = false;
        castsShadows = false;
    }
    
    /**
     * Puts a circle into vertex and normal buffer at the current buffer position. The buffers are enlarged and copied
     * if they are too small.
     * @param radius radius of the circle
     * @param x x coordinate of circle center
     * @param y y coordinate of circle center
     * @param segments number of line segments the circle is built from
     * @param insideOut false for normal winding (ccw), true for clockwise winding
     */
    /*
    public void appendCircle( float radius, float x, float y, int segments,
                              boolean insideOut ) {
        int requiredFloats = segments * 2 * 3;
        FloatBuffer verts = BufferUtils.ensureLargeEnough( getVertexBuffer( 0 ), requiredFloats );
        setVertexBuffer( 0, verts );
        FloatBuffer normals = BufferUtils.ensureLargeEnough( getNormalBuffer( 0 ), requiredFloats );
        setNormalBuffer( 0, normals );
        float angle = 0;
        float step = FastMath.PI * 2 / segments;
        for ( int i = 0; i < segments; i++ ) {
            float dx = FastMath.cos( insideOut ? - angle : angle ) * radius;
            float dy = FastMath.sin( insideOut ? - angle : angle ) * radius;
            if ( i > 0 ) {
                verts.put( dx + x ).put( dy + y ).put( 0 );
                normals.put( dx ).put( dy ).put( 0 );
            }
            verts.put( dx + x ).put( dy + y ).put( 0 );
            normals.put( dx ).put( dy ).put( 0 );
            angle += step;
        }
        verts.put( radius + x ).put( y ).put( 0 );
        normals.put( radius ).put( 0 ).put( 0 );
        generateIndices(0);
    }
     */
    
}
