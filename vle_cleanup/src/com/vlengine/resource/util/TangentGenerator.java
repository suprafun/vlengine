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

package com.vlengine.resource.util;

import com.vlengine.math.FastMath;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Geometry;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.GeometryIterator;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexIterator;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates tangents for CompositeGeometry. Note that (for now),
 * only non-interleaved data buffers can be processed. 
 * @author lex (Aleksey Nikiforov)
 * @author vear (Arpad Vekas) reworked for VL engine - NOT TESTED
 */
public class TangentGenerator {
    private static final Logger log = Logger.getLogger(TangentGenerator.class.getName());
    
    /** each vertex has a data list - one data entry for each triangle */
    protected FastList<VertexData> verticesData;
    /** list of triangles in the same order they are found in the index array */
    private FastList<TriangleData> orderedTriangles;

    // the tolerance data for creating tangents
    private float toleranceAngle = 45;
    private float rebuildTolerance;
    protected boolean tangentFollowsX = true;
    private boolean rebuild = true;
    
    // generate debug data?
    private final boolean generateDebugData = true;
    private FastList<Vector3f> problemVertices;
    private FastList<Vector3f[]> problemTriangles;
    
    // the data of the geometry mesh
    private Geometry geom;
    
    private IndexBuffer srcIndex;
    private GeometryIterator iter;
    
    private VertexIterator posIterator;
    private VertexIterator texIterator;
    private VertexIterator normIterator;
    
    /*
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer usedTextureBuffer;
    private FastList<FloatBuffer> textureBuffers;
    private FloatBuffer colorBuffer;
     */
    
    private Vector3f v0 = new Vector3f();
    private Vector3f v1 = new Vector3f();
    private Vector3f v2 = new Vector3f();

    private Vector3f dv10 = new Vector3f();
    private Vector3f dv20 = new Vector3f();

    private Vector2f t0 = new Vector2f();
    private Vector2f t1 = new Vector2f();
    private Vector2f t2 = new Vector2f();
    
    private Vector2f dt10 = new Vector2f();
    private Vector2f dt20 = new Vector2f();
    
    private Vector3f generatedTangent = new Vector3f();
    private Vector3f generatedBinormal = new Vector3f();
    private Vector3f generatedNormal = new Vector3f();
    private Vector3f givenNormal = new Vector3f();

    public TangentGenerator() {
        this.verticesData = new FastList<VertexData>();
        this.orderedTriangles = new FastList<TriangleData>();
        
        setToleranceAngle(toleranceAngle);
    }
    
    /**
     * @param mesh mesh that will be modified to include tangent space
     * @param textureUnit texture coordinates will be taken for this textureUnit
     */
    public Geometry generateTangents(Geometry g, int textureUnit) {
        if (generateDebugData) {
                problemVertices = new FastList<Vector3f>();
                problemTriangles = new FastList<Vector3f[]>();
        }
        geom = g;

        
        srcIndex = geom.getIndexBuffer();
        
        iter = new GeometryIterator(geom);
        posIterator = iter.getIterator(VertexAttribute.USAGE_POSITION);
        texIterator = iter.getIterator(VertexAttribute.Usage.getById(VertexAttribute.USAGE_TEXTURE0.id+textureUnit));
        normIterator = iter.getIterator(VertexAttribute.USAGE_NORMAL);
                
        for (int i = 0, mi=geom.getNumVertex(); i < mi; i++) {
            verticesData.add(new VertexData(i));
        }

        switch (geom.getMode()) {
        case Geometry.TRIANGLES:
                processTriangles();
                break;
        case Geometry.TRIANGLE_STRIP:
                processTriangleStrip();
                break;
        case Geometry.TRIANGLE_FAN:
                processTriangleFan();
                break;
        }

        processTriangleData();
        if (geom.getNumVertex() < verticesData.size())
            geom = rebuild();
        saveGeneratedData();
        
        // allow gc to release the memory
        iter = null;
        posIterator = null;
        texIterator = null;
        normIterator = null;
        
        orderedTriangles.clear();
        verticesData.clear();
        
        return geom;
    }

    private void processTriangles() {
        if(generateDebugData)
            log.log(Level.FINE, "Processing triangles");
        
        for (int i = 0, mi=geom.getTriangleCount(); i < mi; i++) {
            int index0 = srcIndex.get(i*3);
            int index1 = srcIndex.get(i*3 + 1);
            int index2 = srcIndex.get(i*3 + 2);

            posIterator.get(index0, v0);
            posIterator.get(index1, v1);
            posIterator.get(index2, v2);

            texIterator.get(index0, t0);
            texIterator.get(index1, t1);
            texIterator.get(index2, t2);

            processTriangle(index0, index1, index2);
        }
    }

    private void processTriangleStrip() {
        log.log(Level.FINE, "Processing triangle strip");

        int index0 = srcIndex.get(0);
        int index1 = srcIndex.get(1);

        posIterator.get(index0, v0);
        posIterator.get(index1, v1);
        
        texIterator.get(index0, t0);
        texIterator.get(index1, t1);

        for (int i = 2; i < geom.getNumVertex(); i++) {

                int index2 = srcIndex.get(i);
                posIterator.get(index2, v2);
                texIterator.get(index2, t2);
                
                processTriangle(index0, index1, index2);

                Vector3f vTemp = v0;
                v0 = v1;
                v1 = v2;
                v2 = vTemp;

                Vector2f tTemp = t0;
                t0 = t1;
                t1 = t2;
                t2 = tTemp;

                index0 = index1;
                index1 = index2;
        }
    }

    private void processTriangleFan() {
        log.log(Level.FINE, "Processing triangle fan");

        int index0 = srcIndex.get(0);
        int index1 = srcIndex.get(1);

        posIterator.get(index0, v0);
        posIterator.get(index1, v1);

        texIterator.get(index0, t0);
        texIterator.get(index1, t1);

        for (int i = 2; i < geom.getNumVertex(); i++) {
            int index2 = srcIndex.get(i);
            posIterator.get(index2, v2);
            texIterator.get(index2, t2);

            processTriangle(index0, index1, index2);

            Vector3f vTemp = v1;
            v1 = v2;
            v2 = vTemp;

            Vector2f tTemp = t1;
            t1 = t2;
            t2 = tTemp;

            index1 = index2;
        }
    }
        
    /**
     * Will generate the tangent, binormal and normal for a given triangle
     * and store the result.
     * <p>
     * pre: v0, v1, v2, t0, t1 and t2 must be set before calling
     * this method.<br/>
     * </p>
     */
    private void processTriangle(int index0, int index1, int index2) {
        if (generateDebugData && log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Processing triangle [{0}, {1}, {2}].",
                                new Object[]{ index0, index1, index2 });
        }
            
        t1.subtract(t0, dt10);
        t2.subtract(t0, dt20);
        float det = dt10.x*dt20.y - dt10.y*dt20.x;
        
        if (Math.abs(det) < FastMath.ZERO_TOLERANCE) {
            if (generateDebugData && log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER,
                                    "Discarding traingle [{0}, {1}, {2}]: " +
                                    "linearly dependent texture coordinates.",
                                    new Object[]{ index0, index1, index2 });
            }

            addProblemTriangle(index0, index1, index2);
            return;
        }

        v1.subtract(v0, dv10);
        v2.subtract(v0, dv20);

        generatedTangent.set(dv10);
        generatedTangent.normalizeLocal();
        generatedBinormal.set(dv20);
        generatedBinormal.normalizeLocal();

        if (Math.abs(Math.abs(generatedTangent.dot(generatedBinormal)) - 1)
                        < FastMath.ZERO_TOLERANCE) {
            if (generateDebugData && log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER,
                                    "Discarding traingle [{0}, {1}, {2}]: " +
                                    "vertecies are on the same line.",
                                    new Object[]{ index0, index1, index2 });
            }

            addProblemTriangle(index0, index1, index2);
            return;
        }

        float factor = 1/det;
        generatedTangent.x = (dt20.y*dv10.x - dt10.y*dv20.x)*factor;
        generatedTangent.y = (dt20.y*dv10.y - dt10.y*dv20.y)*factor;
        generatedTangent.z = (dt20.y*dv10.z - dt10.y*dv20.z)*factor;
        generatedTangent.normalizeLocal();

        generatedBinormal.x = (dt10.x*dv20.x - dt20.x*dv10.x)*factor;
        generatedBinormal.y = (dt10.x*dv20.y - dt20.x*dv10.y)*factor;
        generatedBinormal.z = (dt10.x*dv20.z - dt20.x*dv10.z)*factor;
        generatedBinormal.normalizeLocal();

        if (!tangentFollowsX) {
                Vector3f temp = generatedTangent;
                generatedTangent = generatedBinormal;
                generatedBinormal = temp;
        }

        generatedTangent.cross(generatedBinormal, generatedNormal);

        TriangleData data = new TriangleData(
                        generatedTangent.clone(),
                        generatedNormal.clone(),
                        index0, index1, index2);

        verticesData.get(index0).triangles.add(data);
        verticesData.get(index1).triangles.add(data);
        verticesData.get(index2).triangles.add(data);

        orderedTriangles.add(data);
    }
    
    private void processTriangleData() {		
        processVertex:
        for (int i = 0; i < verticesData.size(); i++) {
            VertexData currentVertex = verticesData.get(i);
            FastList<TriangleData> triangles = currentVertex.triangles;

            if (triangles.size() == 0) {
                if(generateDebugData)
                    log.log(Level.WARNING, "No triangles found for vertex {0}.", i);
                addProblemVertex(i);
                continue;
            }
            
            if(rebuild) {
                // check if a vertex should be separated
                int lastVertex = verticesData.size();
                generatedTangent.set(triangles.get(0).tangent);
                
                for (int j = 1; j < triangles.size(); j++) {
                    
                    TriangleData triangleData = triangles.get(j);
                    float dot = generatedTangent.dot(triangleData.tangent);
                    
                    if (dot < rebuildTolerance) {
                        if (lastVertex == verticesData.size()) {
                            verticesData.add(new VertexData(currentVertex));
                        }

                        VertexData newVertex = verticesData.get(lastVertex);
                        triangles.set(j, null);
                        triangleData.replace(i, lastVertex);
                        newVertex.triangles.add(triangleData);
                    }
                }
            }

            // check normal
            normIterator.get(currentVertex.index, givenNormal);
            float normalLength = givenNormal.length();

            if (Math.abs(normalLength - 1) > FastMath.ZERO_TOLERANCE) {
                if(generateDebugData)
                    log.log(Level.WARNING,
                            "The normal for vertex {0} is not unit length " +
                            "and will be renormalized.", i);
                
                givenNormal.divideLocal(normalLength);
                normIterator.put(currentVertex.index, givenNormal);
            }

            // find average tangent
            int flipBinormal = 0;
            generatedTangent.set(0, 0, 0);

            for (int j = 0; j < triangles.size(); j++) {

                TriangleData triangleData = triangles.get(j);
                if (triangleData == null) continue;

                generatedTangent.addLocal(triangleData.tangent);

                if (givenNormal.dot(triangleData.normal) < 0) {
                    if (flipBinormal == 1) {
                        logAddProblemTriangle(triangleData, i);
                        continue processVertex;
                    }
                    flipBinormal = -1;
                } else {
                    if (flipBinormal == -1) {
                        logAddProblemTriangle(triangleData, i);
                        continue processVertex;
                    }
                    flipBinormal = 1;
                }
            }

            if (!rebuild) {
                float tangentLength = generatedTangent.length();
                if (tangentLength < FastMath.ZERO_TOLERANCE) {
                    if(generateDebugData)
                        log.log(Level.WARNING,
                                "Shared tangent is zero for vertex {0}.",i);
                        
                        addProblemVertex(i);
                        continue;
                }
                generatedTangent.divideLocal(tangentLength);
            } else {
                generatedTangent.normalizeLocal();
            }

            if (Math.abs(Math.abs(generatedTangent.dot(givenNormal)) - 1)
                            < FastMath.ZERO_TOLERANCE) {
                if(generateDebugData)
                    log.log(Level.WARNING,
                            "Normal and tangent are parallel for vertex {0}.", i);
                
                addProblemVertex(i);
            }

            givenNormal.cross(generatedTangent, generatedBinormal);
            generatedBinormal.cross(givenNormal, generatedTangent);
            generatedTangent.normalizeLocal();
            // TODO: maybe recalculate binormal, and flip it if needed?
            if(flipBinormal<0) {
                generatedTangent.cross(givenNormal, generatedBinormal);
            }
            generatedBinormal.normalizeLocal();

            // store the computed values
            currentVertex.tangent.set(generatedTangent);
            //currentVertex.flipBinormal = (flipBinormal < 0);
            currentVertex.binormal.set(generatedBinormal);
        }
    }

    private Geometry rebuild() {
        log.log(Level.INFO, "Rebuilding mesh.");

        final int newSize = verticesData.size();

        Geometry target = new Geometry();

        target.setIndexBuffer(
                    IndexBuffer.createBuffer(geom.getTriangleCount()*3,
                    newSize, null));
        target.setMode(BaseGeometry.TRIANGLES);

        IndexBuffer indexBuffer = target.getIndexBuffer();
        for (int j = 0; j < orderedTriangles.size(); j++) {
            TriangleData triangle = orderedTriangles.get(j);
            indexBuffer.put(j*3, triangle.index0);
            indexBuffer.put(j*3 + 1, triangle.index1);
            indexBuffer.put(j*3 + 2, triangle.index2);
        }

        // create buffer for every attribute the source has
        FastList<VertexAttribute> attributes = geom.getAllAttributes(null);
        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute vatt = attributes.get(i);
            // create the data buffer
            VertexBuffer vb = VertexBuffer.createSingleBuffer(vatt.type, newSize);
            target.addAttribBuffer(vb, 0);
        }
        
        // create iterator for the target geometry
        GeometryIterator titer = target.createIterator();

        // rebuild all vertex attributes
        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute vatt = attributes.get(i);
            VertexIterator srci = iter.getIterator(vatt.type);
            VertexIterator trgi = titer.getIterator(vatt.type);
            // populate newly created buffers
            for (int j = 0; j < newSize; j++) {
                VertexData vertexData = verticesData.get(j);
                int srcvrt = vertexData.index;
                trgi.put(j, srci, srcvrt);
            }
        }

        return target;
    }

    // save the generated tangents
    protected void saveGeneratedData() {
        int size = geom.getNumVertex();

        VertexBuffer tb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_TANGENT, size);
        geom.addAttribBuffer(tb, 0);
        FloatBuffer tangentBuffer = tb.getDataBuffer();
        tangentBuffer.clear();

        VertexBuffer bb = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_BINORMAL, size);
        geom.addAttribBuffer(bb, 0);
        FloatBuffer binormalBuffer = bb.getDataBuffer();
        binormalBuffer.clear();

        for (int i = 0; i < size; i++) {
            VertexData vertexData = verticesData.get(i);
            BufferUtils.setInBuffer(vertexData.tangent, tangentBuffer, i);
            BufferUtils.setInBuffer(vertexData.binormal, binormalBuffer, i);
        }
    }

    private void logAddProblemTriangle(TriangleData triangleData, int atVertex) {
        log.log(Level.WARNING, "Opposite normals for different triangles " +
                    "of the shared vertex {0}", atVertex);
        
        addProblemTriangle(
                        triangleData.index0,
                        triangleData.index1,
                        triangleData.index2);
    }

    private void addProblemTriangle(int index0, int index1, int index2) {
        if (generateDebugData) {
            posIterator.get(index0, v0);
            posIterator.get(index1, v1);
            posIterator.get(index2, v2);
            
            problemTriangles.add(new Vector3f[] {
                            v0.clone(), v1.clone(), v2.clone()});
        }
    }
	
    private void addProblemVertex(int index) {
        if (generateDebugData) {
            posIterator.get(index, v0);
            problemVertices.add(v0.clone());
        }
    }
        
    /**
     * If two tangents on different triangles of the shared vertex have
     * an angle above the tolerance angle, then a copy of the vertex is
     * created and each of the triangles uses a separate copy of the vertex.
     * <p>
     * In other words, this setting prevents artifacts caused by approximation
     * of many different tangents by a single tangent of the shared vertex. This
     * is achieved by adding extra vertices to the mesh.
     * </p><p>
     * Setting the angle 0 will cause every triangle to have it's own vertex.
     * Setting the angle close to 180 might result in visual artifacts.
     * Default is 45 degrees.
     * </p>
     * @param angle in degrees, must be between 0 and 179
     */
    public void setToleranceAngle(float angle) {
        if (angle < 0 || angle > 179) {
                throw new IllegalArgumentException(
                            "The angle must be between 0 and 179 degrees.");
        }
        rebuildTolerance = FastMath.cos(angle*FastMath.DEG_TO_RAD);
        toleranceAngle = angle;
    }

    protected class VertexData {
        public final int index;
        public Vector3f tangent = new Vector3f();
        //public boolean flipBinormal;
        public Vector3f binormal = new Vector3f();
        public FastList<TriangleData> triangles;

        public VertexData(int index) {
            this.index = index;
            triangles = new FastList<TriangleData>();
        }

        public VertexData(VertexData data) {
            this(data.index);
        }
    }
    
    protected static class TriangleData {
        public Vector3f tangent;
        public Vector3f normal;
        public int index0;
        public int index1;
        public int index2;

        public TriangleData(Vector3f tangent, Vector3f normal,
                        int index0, int index1, int index2) {
            this.tangent = tangent;
            this.normal = normal;
            this.index0 = index0;
            this.index1 = index1;
            this.index2 = index2;
        }

        public void replace(int currentIndex, int newIndex) {
            if (index0 == currentIndex) index0 = newIndex;
            if (index1 == currentIndex) index1 = newIndex;
            if (index2 == currentIndex) index2 = newIndex;
        }
    }
}
