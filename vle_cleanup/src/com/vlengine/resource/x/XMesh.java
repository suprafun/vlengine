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

package com.vlengine.resource.x;

import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;

/**
 * Data structure temporarily holding data on a mesh
 * @author vear (Arpad Vekas)
 */
public class XMesh {
    // name of the mesh
    public String name;

    // number of vertices
    public int numVertex = 0;
    // the buffer for vertex position data
    public FastList<Vector3f> vertexPosition;
    // indices
    public IntList indices;
    // the texture coords
    public FastList<Vector2f> textureCoords;
    // the normals
    public FastList<Vector3f> normals;
    // the colors
    public FastList<ColorRGBA> colors;
    // the weight index and value
    public FastList<int[]> weightIndex;
    // max weights per vertex
    public int maxWeights = 0;
    public FastList<float[]> weightValue;
    // the mesh frame matrix
    public XFrame meshFrame;

    // data specific to main mesh
    // number of faces (3 or 4 componenet)
    public int numFaceLines = 0;
    // indice start for faces
    public IntList indiceStart;
    public int numMatIndices;
    // the material indices
    public IntList matIndices;
    // the materials
    public FastList<String> meshMaterials;
    // the normals table
    public int numNormals;
    public FastList<Vector3f> normalsTable;
    
    // data specific to mesh part
    // the name of the material
    public String materialName;
    public IntList boneMapping;
}
