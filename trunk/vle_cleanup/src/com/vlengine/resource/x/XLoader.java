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

import com.vlengine.app.AppContext;
import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.BoundingSphere;
import com.vlengine.bounding.BoundingVolume;
import com.vlengine.image.Image;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Geometry;
import com.vlengine.model.XWeightedGeometry;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelMaterial;
import com.vlengine.resource.model.ModelMaterialPart;
import com.vlengine.resource.model.ModelPart;
import com.vlengine.resource.obj.Tokens;
import com.vlengine.scene.animation.Bone;
import com.vlengine.scene.animation.x.XAnimationFrame;
import com.vlengine.scene.animation.x.XBoneAnimation;
import com.vlengine.scene.animation.x.XBoneAnimationPack;
import com.vlengine.system.VleException;
import com.vlengine.util.BufferInputStream;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads X (DirectX mesh+animation) files
 * The loader is spefically written to load X models
 * exported from Maya 2008 with the XExporter(); script
 * @author vear (Arpad Vekas)
 */
public class XLoader {
    private static final Logger log = Logger.getLogger(XLoader.class.getName());
    

    
    private AppContext app;
    
    // parameters used in creating the model
    private ParameterMap params;
    
    // 0- root
    // 1- Material
    // 2- Frame
    // 3- FrameTransformMatrix
    // 4- Mesh
    // 5- MeshTextureCoords
    // 6- MeshNormals
    // 7- MeshVertexColors
    // 8- MeshMaterialList
    // 9- VertexDuplicationIndices
    // 10- DeclData
    // 11- XSkinMeshHeader
    // 12- SkinWeights
    // 13- AnimationSet
    // 14- Animation
    // 15- AnimationKey
    // 16- AnimTicksPerSecond
    private int section = 0;
    // the stack of sections, store section in this list
    // when calling asubsection
    // the called section will return to the section
    // in the last element in the list
    private IntList sectionStack = new IntList();
    
    private BufferedReader inFile;
    
    private Model obj;
    private ModelMaterial currentMaterial;
    private int materialLine = 0;
    
    // all the frames in the file
    private FastList<XFrame> frames = new FastList<XFrame>();
    // the current frame
    private XFrame currentFrame;
    private XFrame rootFrame;
    // the meshes
    private FastList<XMesh> meshes = new FastList<XMesh>();
    // the processed meshes
    private FastList<FastList<XMesh>> processedMeshes;
    // the current mesh
    private XMesh currentMesh;
    // the lines we read in mesh section
    private int meshLines = 0;
    // did we process the geometry?
    private boolean geomProcessed = false;
    
    private boolean processnormals = true;
    private boolean processcolors = true;
    
    // handling of weights
    private XFrame currentBone = null;
    public int currentBoneWeights;
    public int[] currentBoneVertIndices;
    
    // animation ticks per second
    private int animTicksPerSecond = 30;
    // the animation pack
    private XBoneAnimationPack anims;
    // the current animation
    private XBoneAnimation currentAnimation;
    // processed bones
    private boolean[] boneProcessed;
    
    // the currently animated bone
    private XFrame animatedBone;
    // the current animated bones transforms
    private FastList<Matrix4f> animatedBoneTransforms;
    
    // the set of bone mappings
    // the bitset contains bone id-s, the shortmap contains
    // mapping from boen id-s to local bone ids
    FastList<IntList> boneSets;

    public XLoader(AppContext app) {
        this.app = app;
    }
    
    public Model convert(ByteBuffer modelData, ParameterMap params) {
        try {

            // get a stream to our buffer
            InputStream format = new BufferInputStream(modelData);
            
            obj = new Model();
            this.params = params;
            processnormals = params.getBoolean("processnormals", true);
            processcolors = params.getBoolean("processcolors", true);

            inFile = new BufferedReader(new InputStreamReader(format));
            String in;

            // reset state data
            section = 0;
            currentMaterial = null;
            currentBone = null;
            geomProcessed = false;
            currentBoneVertIndices = null;
            meshes.clear();
            frames.clear();
            boneSets = null;
            processedMeshes = new FastList<FastList<XMesh>>();

            while ((in = inFile.readLine()) != null) {
                processLine(in);
            }
            inFile.close();
            
            // create the final batches
            // based on used materials and bones
            createModel();

        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
            obj = null;
        }
        
        Model robj = obj;
        obj= null;
        meshes.clear();
        frames.clear();
        currentMaterial = null;
        currentFrame = null;
        currentMesh = null;
        this.params = null;
        boneSets = null;
        this.params = null;
        
        return robj;
    }

    private int popSection() {
        int lastsec = sectionStack.get(sectionStack.size()-1);
        sectionStack.removeElementAt(sectionStack.size()-1);
        return lastsec;
    }
    
    private void processLine(String line) {
        if (line == null || line.length() == 0) return;
        
        // tokens with proper delimiters
        Tokens tokens = new Tokens(line, " ;,");
        if (tokens.isEmpty()) return;
        String token = tokens.pop();
        
        if (token.charAt(0) == '#') {
            return;
        } else if(token.startsWith("//")) {
            return;
        } else if(section==0) {
            // at root
            handleRoot(token, tokens);
            return;
        } else if(section==1) {
            // material section
            handleMaterial(token, tokens);
            return;
        } else if(section==2) {
            // frame section
            handleFrame(token, tokens);
            return;
        } else if(section==3) {
            //FrameTransformMatrix
            if("}".equals(token)) {
                // we fall back to section 2
                section = 2;
                return;
            }
            setMatrixValue(currentFrame.transformMatrix, 0, Float.parseFloat(token));
            // read in the rest 15 values
            for(int i=1; i<16; i++) {
                // check row or column major!
                setMatrixValue(currentFrame.transformMatrix, i, Float.parseFloat(tokens.pop()));
            }
            return;
        } else if(section==4) {
            // in the Mesh section
            handleMesh(token, tokens);
            return;
        } else if(section==5) {
            // texture coords
            if("}".equals(token)) {
                // texcoord section finished
                // go back to mesh section
                section = 4;
                return;
            } else if(meshLines == 0) {
                // get number of texcoords
                int numTexcoords = Integer.parseInt(token);
                // it should be the same as numVertices
                if(numTexcoords != currentMesh.numVertex) {
                    throw new VleException("Mesh with "+currentMesh.numVertex+" vertices but with "+numTexcoords+" texture coords");
                }
                // allocate float buffer for texture coords
                currentMesh.textureCoords = new FastList<Vector2f>(numTexcoords);
                currentMesh.textureCoords.clear();
            } else {
                // get U,V
                Vector2f texc = new Vector2f();
                texc.x=FastMath.abs(Float.parseFloat(token));
                texc.y=FastMath.abs(Float.parseFloat(tokens.pop()));
                currentMesh.textureCoords.add(texc);
            }
            meshLines++;
        } else if(section==6) {
            // normals
            handleNormals(token, tokens);
            return;
        } else if(section==7) {
            // colors
            if("}".equals(token)) {
                // colors section finished
                // go back to mesh section
                section = 4;
                return;
            } else if(meshLines == 0) {
                // get number of texcoords
                int numColors = Integer.parseInt(token);
                // it should be the same as numVertices
                if(numColors != currentMesh.vertexPosition.size()) {
                    throw new VleException("Mesh with "+currentMesh.vertexPosition.size()+" vertices but with "+numColors+" colors");
                }
                // allocate float buffer for colors
                currentMesh.colors = new FastList<ColorRGBA>(numColors);
                currentMesh.colors.clear();
            } else {
                int colindex = Integer.parseInt(token);
                ColorRGBA col = new ColorRGBA();
                
                col.r=Float.parseFloat(tokens.pop());
                col.g=Float.parseFloat(tokens.pop());
                col.b=Float.parseFloat(tokens.pop());
                col.a=Float.parseFloat(tokens.pop());
                currentMesh.colors.set(colindex, col);
            }
            meshLines++;
        } else if(section==8) {
            handleMeshMaterials(token, tokens);
            return;
        } else if(section==9) {
            // VertexDuplicationIndices
            if("}".equals(token)) {
                // VertexDuplicationIndices section finished
                // go back to mesh section
                section = 4;
                return;
            }
            // we ignore this section
        } else if(section==10) {
            // DeclData 
            if("}".equals(token)) {
                // DeclData section finished
                // go back to mesh section
                section = 4;
                return;
            }
            // this section possibly contains tangents and binormals
            // number of vertex elements
            // vertex declatation[n]
            // number of data elements, as DWORD
            
        } else if(section==11) {
            // XSkinMeshHeader 
            if("}".equals(token)) {
                // XSkinMeshHeader  section finished
                // go back to mesh section
                section = 4;
                return;
            }           
        } else if(section==12) {
            // SkinWeights  
            handleSkinWeights(token, tokens);
            return;
        } else if(section==13) {
            // AnimationSet
            if("}".equals(token)) {
                // end of animation set
                currentAnimation = null;
                // go back to root
                section = 0;
                return;
            } else if("Animation".equals(token)) {
                // go to Animation section
                meshLines = 0;
                section=14;
            }
            return;
        } else if(section==14) {
            handleAnimation(token, tokens);
            return;
        } else if(section==15) {
            handleAnimationKey(token, tokens);
        } else if(section==16) {
            // AnimTicksPerSecond
            if("}".equals(token)) {
                // section finished
                // go back to root
                section = 0;
                return;
            } else {
                this.animTicksPerSecond=Integer.parseInt(token);
            }
        }
    }

    private void handleAnimationKey(String token, Tokens tokens) {
        // AnimationKey
        if("}".equals(token)) {
            // end of animation key
            // go back to animation
            section = 14;
            return;
        } else if(meshLines == 0) {
            int keyType = Integer.parseInt(token);
            if(keyType != 4) {
                throw new VleException("AimationKey type "+keyType+", only matrix type supported");
            }
        } else if(meshLines==1) {
            // get number of keyframes
            int numFrames = Integer.parseInt(token);

            animatedBoneTransforms = new FastList<Matrix4f>(numFrames);
        } else {
            int numFrame = meshLines-2;
            
            int fnumFrame = Integer.parseInt(token)-1;
            // the number of values in the matrix
            int matValues = Integer.parseInt(tokens.pop());
            if(matValues != 16) {
                throw new VleException("AimationKey frame with "+matValues+" matrix values");
            }
            // create matrix
            Matrix4f loadMatrix = new Matrix4f();
            
            for(int i=0; i<16; i++) {
                // check row or column major!
                setMatrixValue(loadMatrix, i, Float.parseFloat(tokens.pop()));
            }
            animatedBoneTransforms.set(fnumFrame, loadMatrix);
        }
        meshLines++;
    }

    private void handleAnimation(String token, Tokens tokens) {
        // Animation
        if("}".equals(token)) {
            // process animation
            //animatedBoneTransforms
            if(animatedBoneTransforms!=null
                    && animatedBone !=null) {
                processAnimation();
            }
            // end of animation
            animatedBone = null;
            animatedBoneTransforms = null;
            // go back to animation set
            section = 13;
            return;
        } else if("{".equals(token)) {
            // get the bone name
            String boneName = tokens.pop();
            // find the animated bone
            for(int i=0; i<frames.size() && animatedBone==null; i++) {
                XFrame f = frames.get(i);
                if(f==null)
                    continue;
                if(f.name.equals(boneName)) {
                    // set the bone to be the animated bone
                    animatedBone = f;
                }
            }
        } else if("AnimationKey".equals(token)) {
            meshLines = 0;
            // go to section 15
            section = 15;
        }
    }

    private void processAnimation() {
        int numframes = animatedBoneTransforms.size();
        if(currentAnimation.frames==null) {
            // allocate the frames if they are not yet
            currentAnimation.frames = new XAnimationFrame[numframes];
        } else if(currentAnimation.frames.length != numframes) {
            throw new VleException("AimationKey with "+numframes+" but was "+currentAnimation.frames.length);
        }
        // store previous keyframe number
        int prevKeyFrame = -1;
        
        for(int i=0; i<numframes; i++) {
            if(currentAnimation.frames[i]==null) {
                currentAnimation.frames[i] = new XAnimationFrame();
                // create transform matrix for each bone
                currentAnimation.frames[i].transform = new Matrix4f[this.frames.size()];
            }
            Matrix4f trans = animatedBoneTransforms.get(i);
            if(trans!=null) {
                currentAnimation.frames[i].transform[animatedBone.id] = trans;
                // if we dont have frames from frame 0
                if(prevKeyFrame==-1 && i>0) {
                    // fill in with the matrix
                    for(int j=0; j<i; j++) {
                        currentAnimation.frames[j].transform[animatedBone.id] = trans;
                    }
                } else if(prevKeyFrame!=i-1) {
                    // if previous keyframe was not the previous keyframe
                    // interpolate frames between previous keyframe and this one
                    Matrix4f prevTrans = currentAnimation.frames[prevKeyFrame].transform[animatedBone.id];
                    for(int j=prevKeyFrame+1; j<i; j++) {
                        // calculate interpolation
                        float interpolation = (float)(j-prevKeyFrame)/(float)(i-prevKeyFrame);
                        Matrix4f interpolated = new Matrix4f().set(prevTrans);
                        interpolated.interpolate(trans, interpolation);
                        currentAnimation.frames[j].transform[animatedBone.id] = interpolated;
                    }
                }
                prevKeyFrame = i;
            }
        }
        if(prevKeyFrame<numframes-1) {
            // fill in the last frames with the last keyframe
            Matrix4f trans = animatedBoneTransforms.get(prevKeyFrame);
            for(int j=prevKeyFrame+1; j<numframes; j++) {
                currentAnimation.frames[j].transform[animatedBone.id] = trans;
            }
        }
    }

    private void handleMeshMaterials(String token, Tokens tokens) {
        // material indices
        if("}".equals(token)) {
            // material indices section finished
            // go back to mesh section
            section = 4;
            return;
        } else if(meshLines == 0) {
            // get number of materials
            int numMaterials = Integer.parseInt(token);
            // create array for material names
            currentMesh.meshMaterials = new FastList(numMaterials);
        } else if(meshLines == 1) {
            // get number of material indices
            currentMesh.numMatIndices = Integer.parseInt(token);
            // it should be the same as numVertices
            if(currentMesh.numMatIndices != currentMesh.numFaceLines) {
                throw new VleException("Mesh with "+currentMesh.numFaceLines+" faces but with "+currentMesh.numMatIndices+" material indices");
            }
            // create array for material indices
            currentMesh.matIndices = new IntList(currentMesh.numMatIndices);
        } else if(meshLines<currentMesh.numMatIndices+2) {
            int matindex = Integer.parseInt(token);
            // put it into matindices
            currentMesh.matIndices.add(matindex);
        } else {
            if("Material".equals(token)) {
                // inline material
                handleMaterialHeader(token, tokens);
                // add it to materials
                currentMesh.meshMaterials.add(currentMaterial.getId());
            } else {
                // reference
                // material name
                String matname = null;
                matname = token.substring(1, token.length()-1);                
                // add it to materials
                currentMesh.meshMaterials.add(matname);
            }
        }
        meshLines++;
    }
    
    private XFrame createFrame(String frameName) {
        XFrame f = findFrame(frameName);
        if(f!=null)
            return f;
        // the root has no parent frame
        f = new XFrame();
        // set and id on the new frame (starting from 1)
        f.id = frames.size()==0?1:+frames.size();
        // read the name
        f.name = frameName;
        // add it to the frames
        frames.set(f.id, f);
        return f;
    }

    private void handleRoot(String token, Tokens tokens) {
        if("xof".equals(token)) {
            // file header
            return;
        } else if("AnimTicksPerSecond".equals(token)) {
            section = 16;
            return;
        } else if("Material".equals(token)) {
            handleMaterialHeader(token, tokens);
            return;
        } else if("Frame".equals(token)) {
            String name = tokens.pop();
            // the root has no parent frame
            currentFrame = createFrame(name);
            // is this the root frame?
            if(currentFrame.name.endsWith("root"))
                rootFrame = currentFrame;
            // frame section start
            section = 2;
            return;
        } else if("AnimationSet".equals(token)) {
            // starting an animationset section
            // check if we have an animation pack?
            if(anims == null) {
                anims = new XBoneAnimationPack();
                // prepare the bones of the model
                processBones();
                obj.setAnimationPack(anims);
            }

            // create a new animation
            currentAnimation = new XBoneAnimation();
            String animName = tokens.pop();
            if("{".equals(animName)) {
                // no name for the animation, generate one
                animName = "Animation"+anims.getNumAnimations();
            }
            // get the name of the animation
            currentAnimation.name = animName;
            
            currentAnimation.frameRate = animTicksPerSecond;
            // add the animation to the pack
            anims.addAnimation(currentAnimation);
            section = 13;
            return;
        }
    }

    private void handleMaterialHeader(String token, Tokens tokens) {
        // get the name of the material
        String matName = tokens.pop();
        if("{".equals(matName)) {
            // no name for material
            // are we not at root?
            if(section==0)
                matName = "";
            else {
                // the case of the inline material, generate name
                matName = "Material"+currentMesh.meshMaterials.size();
            }
        }
        // create a new material
        currentMaterial = new ModelMaterial();
        currentMaterial.setId(matName);
        obj.getMaterials().put(matName, currentMaterial);
        // push the current section
        sectionStack.add(section);
        section = 1;
        materialLine = 0;
    }

    private void handleMaterial(String token, Tokens tokens) {
        if("}".equals(token)) {
            // Material section finished
            currentMaterial = null;
            // pop the section from stack
            section = popSection();
        } else {
            ColorRGBA col;
            switch(materialLine) {
                case 0: // parse color
                    col = new ColorRGBA(Float.parseFloat(token)
                            , Float.parseFloat(tokens.pop())
                            , Float.parseFloat(tokens.pop())
                            , Float.parseFloat(tokens.pop())
                            );
                    if(col.r != 0 || col.g !=0 || col.b != 0)
                        currentMaterial.setAmbient(col);
                    // this should be used as default color and diffuse too?
                    break;
                case 1: // parse power, is it realy shiness?
                    currentMaterial.setShininess(Float.parseFloat(token));
                    break;
                case 2: // parse specular
                    col = new ColorRGBA(Float.parseFloat(token)
                            , Float.parseFloat(tokens.pop())
                            , Float.parseFloat(tokens.pop())
                            , 1f
                            );
                    if(col.r != 0 || col.g !=0 || col.b != 0)
                        currentMaterial.setSpecular(col);
                    break;
                case 3: // parse emmisive
                    col = new ColorRGBA(Float.parseFloat(token)
                            , Float.parseFloat(tokens.pop())
                            , Float.parseFloat(tokens.pop())
                            , 1f
                            );
                    if(col.r != 0 || col.g !=0 || col.b != 0)
                        currentMaterial.setEmissive(col);
                    break;
                default: // parse texture
                    if("texturefilename".equalsIgnoreCase(token)) {
                        handleTextureFilename(token, tokens);
                    }
                    break;
            }
            materialLine++;
        }
    }

    private void handleTextureFilename(String token, Tokens tokens) {
        
        //String filename = null;
        
        boolean end = false;
        while(!end) {
            if(tokens.isEmpty()) {
                // read in a new line
                String line = null;
                try {
                    line = inFile.readLine();
                } catch(Exception e) {

                }
                if(line==null) {
                    end=true;
                    continue;
                }
                tokens = new Tokens(line, " ;,");
                if (tokens.isEmpty()) {
                    end=true;
                    continue;
                }
            }
            String tf = tokens.pop();
            if(tf.endsWith("}")) {
                end=true;
                tf = tf.substring(0, tf.length()-1);
            }
            //TextureFileName {".\\clr_dumpster.png";}
            if(tf.startsWith("{")) {
                tf = tf.substring(1, tf.length());
            }
            if(tf.startsWith("\"")) {
                tf = tf.substring(1, tf.length());
            }
            if(tf.endsWith("\"")) {
                tf = tf.substring(0, tf.length()-1);
            }
            // remove beginning pathname, leave only the file name
            int bpos = tf.lastIndexOf("\\");
            if(bpos != -1) {
                tf = tf.substring(bpos+1, tf.length());
            }
            if(!"".equals(tf)) {
                // we have a file name
                // create a submaterial
                ModelMaterialPart subm = new ModelMaterialPart();
                // default set as diffuse
                subm.setType(ModelMaterialPart.TextureType.Diffuse);
                subm.setTextureName(tf);
                
                currentMaterial.addTexture(subm);
            }
        }
    }

    private void handleFrame(String token, Tokens tokens) {
        if("}".equals(token)) {
            // Frame section finished
            currentFrame = currentFrame.parentFrame;
            // if no parent, exit to root section
            if(currentFrame == null)
                section = 0;
            return;
        } else if("FrameTransformMatrix".equals(token)) {
            // put transform matrix into current frame
            currentFrame.transformMatrix = new Matrix4f();
            section = 3;
        } else if("Frame".equals(token)) {
            String name = tokens.pop();
            // a new frame inside the frame
            XFrame f = createFrame(name);
            // set the current frame as parent
            f.parentFrame = currentFrame;
            // set the new frame as current
            currentFrame = f;
            // we stay in frames section
            section = 2;
        } else if("Mesh".equals(token)) {
            // create a new mesh
            currentMesh = new XMesh();
            // get name of mesh
            currentMesh.name = tokens.pop();
            // set the current frame into the mesh
            currentMesh.meshFrame = currentFrame;
            // add the mesh to the meshes
            meshes.add(currentMesh);
            // we are entering a mesh section
            section = 4;
            // counting lines read
            meshLines = 0;
            // max weights per vertex
            currentMesh.maxWeights = 0;
            // mesh isnt yet processed
            geomProcessed = false;
        }
    }

    private void handleMesh(String token, Tokens tokens) {
        if("}".equals(token)) {
            // Mesh section finished
            // process the mesh
            processMesh();
            // add differently materialed parts as modelparts

            // no active mesh
            currentMesh = null;
            // go back to frame section
            section = 2;
            return;
        } else if(!geomProcessed) {
            // we need to load in the mesh first
            if(meshLines==0) {
                // we need to load in the number of vertices
                currentMesh.numVertex = Integer.parseInt(token);
                // create float buffer (non-direct) for vertices
                currentMesh.vertexPosition = new FastList<Vector3f>(currentMesh.numVertex);
                currentMesh.vertexPosition.clear();
            } else if(meshLines<=currentMesh.numVertex) {
                // read in vertex data
                Vector3f pos = new Vector3f();
                pos.x=Float.parseFloat(token);
                pos.y=Float.parseFloat(tokens.pop());
                pos.z=Float.parseFloat(tokens.pop());
                currentMesh.vertexPosition.add(pos);
            } else if(meshLines==currentMesh.numVertex+1) {
                // read in number of faces
                currentMesh.numFaceLines = Integer.parseInt(token);
                // allocate array for indices
                currentMesh.indices = new IntList(currentMesh.numFaceLines*3*2);
                // allocate array for face start
                currentMesh.indiceStart = new IntList(currentMesh.numFaceLines);
            } else if(meshLines<=currentMesh.numVertex+currentMesh.numFaceLines+1) {
                // read in indices in the face
                int faceIndices = Integer.parseInt(token);
                // put in the start of the face
                currentMesh.indiceStart.add(currentMesh.indices.size());
                // read int the indices
                int first = Integer.parseInt(tokens.pop());
                int second = Integer.parseInt(tokens.pop());
                int third = Integer.parseInt(tokens.pop());
                currentMesh.indices.add(first);
                currentMesh.indices.add(second);
                currentMesh.indices.add(third);

                // more than 3 indices
                if(faceIndices==4) {
                    int fourth = Integer.parseInt(tokens.pop());
                    currentMesh.indices.add(first);
                    currentMesh.indices.add(third);
                    currentMesh.indices.add(fourth);
                }
            }
            meshLines++;
            if(meshLines>currentMesh.numVertex+currentMesh.numFaceLines+1) {
                // we are ready to handle other section types
                geomProcessed = true;
            }
            return;
        } else {
            // process other sections
            if("MeshTextureCoords".equals(token)) {
                meshLines = 0;
                section=5;
            } else if("MeshNormals".equals(token)) {
                meshLines = 0;
                section=6;                    
            } else if("MeshVertexColors".equals(token)) {
                meshLines = 0;
                section=7;
            } else if("MeshMaterialList".equals(token)) {
                meshLines = 0;
                section=8;
            } else if("VertexDuplicationIndices".equals(token)) {
                meshLines = 0;
                section=9;
            } else if("DeclData".equals(token)) {
                meshLines = 0;
                section=10;
            } else if("XSkinMeshHeader".equals(token)) {
                meshLines = 0;
                section=11;
            } else if("SkinWeights".equals(token)) {
                meshLines = 0;
                section=12;
            }
        }
    }

    private void handleNormals(String token, Tokens tokens) {
        if("}".equals(token)) {
            // normalize all the normals
            if(this.processnormals) {
                for(int i=0; i<currentMesh.normals.size(); i++) {
                    currentMesh.normals.get(i).normalizeLocal();
                }
            }
            // normals section finished
            // go back to mesh section
            section = 4;
            return;
        } else if(this.processnormals) {
            if(meshLines == 0) {
                // get number of texcoords
                currentMesh.numNormals = Integer.parseInt(token);

                // allocate float buffer for texture coords
                currentMesh.normalsTable = new FastList<Vector3f>(currentMesh.numNormals);
                currentMesh.normalsTable.clear();

                // allocate normals list
                currentMesh.normals = new FastList<Vector3f>(currentMesh.vertexPosition.size());

            } else if(meshLines<currentMesh.numNormals+1) {
                Vector3f norm = new Vector3f();
                norm.x=Float.parseFloat(token);
                norm.y=Float.parseFloat(tokens.pop());
                norm.z=Float.parseFloat(tokens.pop());
                currentMesh.normalsTable.add(norm);
            } else if(meshLines==currentMesh.numNormals+1) {
                // get number of normals faces
                int normalsFaces = Integer.parseInt(token);
                if(normalsFaces!=currentMesh.numFaceLines) {
                    throw new VleException("Mesh with "+currentMesh.numFaceLines+" faces but with "+normalsFaces+" normals faces");
                }
            } else {
                // the corresponding face start index
                int faceIndex = meshLines-currentMesh.numNormals-2;
                // get start index
                int startIndex = currentMesh.indiceStart.get(faceIndex);
                int endIndex;
                // if its the last one
                if(faceIndex==currentMesh.indiceStart.size()-1) {
                    // last one
                    endIndex = currentMesh.indices.size()-1;
                } else {
                    // not last, next-1
                    endIndex = currentMesh.indiceStart.get(faceIndex+1)-1;
                }
                // get number of normals face indices
                int normIndices = Integer.parseInt(token);
                int vertIndices = endIndex-startIndex+1;
                if(normIndices!=3 && normIndices!=4) {
                    throw new VleException("Mesh face normal "+faceIndex+" with "+normIndices);
                }
                // it should match the number of vertex position indices
                if((normIndices==3 && vertIndices!=3)
                        ||(normIndices==4 && vertIndices!=6)
                        ) {
                    // does not match
                    throw new VleException("Mesh face normal "+faceIndex+" with "+normIndices+" not match vertex face with "+(endIndex-startIndex+1)+" components");
                }
                // go over indices, read in data
                for(int i=0; i<normIndices; i++) {
                    int normIndex = Integer.parseInt(tokens.pop());
                    int idxIndex = startIndex;
                    if(i<3) {
                        idxIndex += i;
                    } else {
                        idxIndex = endIndex;
                    }
                    int vertIndex = currentMesh.indices.get(idxIndex);
                    // get normal
                    Vector3f norm = currentMesh.normalsTable.get(normIndex);
                    // add it to vertex's normal
                    Vector3f vertNorm = currentMesh.normals.get(vertIndex);
                    if(vertNorm==null) {
                        vertNorm= new Vector3f();
                        currentMesh.normals.set(vertIndex, vertNorm);
                    }
                    // add normal to vert normal
                    vertNorm.addLocal(norm);
                }
            }
            meshLines++;
        }
    }
    
    private XFrame findFrame(String frameName) {
        for(int i=0; i<this.frames.size() && currentBone==null; i++) {
            XFrame f = frames.get(i);
            if(f==null)
                continue;
            if(f.name.equals(frameName)) {
                return f;
            }
        }
        return null;
    }

    private void handleSkinWeights(String token, Tokens tokens) {
        if("}".equals(token)) {
            // SkinWeights section finished
            // go back to mesh section
            currentBone = null;
            section = 4;
            return;
        }
        if(meshLines==0) {
            // bone name
            String boneName = token.substring(1, token.length()-1);
            // find the bone
            currentBone = findFrame(boneName);
            
            if(currentBone == null) {
                // no bone yet, create it
                currentBone = createFrame(boneName);
            }
        } else if(currentBone!=null) {
            if(meshLines==1) {
                // number of weights
                currentBoneWeights = Integer.parseInt(token);
                // ensure that the mesh has weights
                if(currentMesh.weightIndex==null) {
                    currentMesh.weightIndex = new FastList<int[]>(currentMesh.vertexPosition.size());
                }
                if(currentMesh.weightValue==null) {
                    currentMesh.weightValue = new FastList<float[]>(currentMesh.vertexPosition.size());
                }
                // allocate vertex indices
                currentBoneVertIndices = new int[currentBoneWeights];
            } else if(meshLines<currentBoneWeights+2) {
                int weightIndex = meshLines-2;
                // read in vertex index
                currentBoneVertIndices[weightIndex] = Integer.parseInt(token);
            } else if(meshLines<currentBoneWeights*2+2) {
                int weightIndex = meshLines-(currentBoneWeights+2);
                // weight value
                float weightValue = Float.parseFloat(token);
                // put the weight into vertex's weights
                // sort them on the way, so most influential comes first
                int vertIndex = currentBoneVertIndices[weightIndex];
                int[] vertWeightIndex = currentMesh.weightIndex.get(vertIndex);
                if(vertWeightIndex==null) {
                    vertWeightIndex=new int[4];
                    currentMesh.weightIndex.set(vertIndex, vertWeightIndex);
                }
                float[] vertWeightValue = currentMesh.weightValue.get(vertIndex);
                if(vertWeightValue==null) {
                    vertWeightValue=new float[4];
                    currentMesh.weightValue.set(vertIndex, vertWeightValue);
                }
                // find the spot where to put the new weight
                int weightPos = 0;
                while(weightPos<4 // we only collect 4 weights
                        && vertWeightIndex[weightPos]!=0  // weight position is occupied
                        && vertWeightValue[weightPos]>weightValue  // weight is more influential, then what we have
                        ) {
                    // skip one position
                    weightPos++;
                }
                if(weightPos+1>currentMesh.maxWeights) {
                    currentMesh.maxWeights = weightPos+1;
                }
                if(weightPos<4) {
                    // we have a position for the new weight
                    if(weightPos<4-1) {
                        // make room for the new weight
                        System.arraycopy(vertWeightIndex, weightPos, vertWeightIndex, weightPos+1, (4-1)-weightPos);
                        System.arraycopy(vertWeightValue, weightPos, vertWeightValue, weightPos+1, (4-1)-weightPos);
                    }
                    // put in the weight index and value
                    vertWeightIndex[weightPos] = currentBone.id;
                    vertWeightValue[weightPos] = weightValue;
                    //if(weightPos>3) {
                       // log.log(Level.WARNING, "Vertice with "+(weightPos+1)+" weights");
                    //}
                } else {
                    //log.log(Level.WARNING, "Vertice with more that 4 weights");
                }
            } else {
                // the matrix
                if(currentBone.matrixOffset==null) {
                    currentBone.matrixOffset = new Matrix4f();
                }
                
                setMatrixValue(currentBone.matrixOffset, 0, Float.parseFloat(token));
                for(int i=1; i<16; i++) {
                    setMatrixValue(currentBone.matrixOffset, i, Float.parseFloat(tokens.pop()));
                }
            }
        }
        
        // 
        meshLines++;
    }

    private void setMatrixValue(Matrix4f matrix, int index, float value) {
        // check row or coulmn major?
        // TODO: this should be reversed, or the X exporter changed
        matrix.set(index/4, index%4, value);
    }
    
    /**
     * Create a geometry and modelpart objects from imported data
     */
    private void processMesh() {
        // the meshes separated by each material
        FastList<XMesh> meshByMaterial = new FastList<XMesh>(currentMesh.meshMaterials.size());

        // the map of all the vertices, where a vertice from the mesh is in a part mesh
        FastList<int[]> vertexIndices = new FastList<int[]>(currentMesh.meshMaterials.size());
        
        // go over all the face materials
        for(int i=0; i<currentMesh.matIndices.size(); i++) {
            int matIndex = currentMesh.matIndices.get(i);
            // get the mesh that holds part for the material
            XMesh matMesh = meshByMaterial.get(matIndex);
            if(matMesh==null) {
                // no mesh yet, create it
                matMesh = new XMesh();
                // set material name
                matMesh.materialName = currentMesh.meshMaterials.get(matIndex);
                // set mesh name
                matMesh.name = currentMesh.name;
                // copy the frame
                matMesh.meshFrame = currentMesh.meshFrame;
                // reset num max weights
                matMesh.maxWeights = 0;
                // create the arrays
                int possibleParts = currentMesh.meshMaterials.size();
                // indices with guessed starting size
                matMesh.indices = new IntList(currentMesh.indices.size()/possibleParts);
                // 
                int possibleVertices = currentMesh.vertexPosition.size() / possibleParts;
                matMesh.vertexPosition = new FastList<Vector3f>(possibleVertices);
                if(currentMesh.colors!=null)
                    matMesh.colors = new FastList<ColorRGBA>(possibleVertices);
                matMesh.normals = new FastList<Vector3f>(possibleVertices);
                matMesh.textureCoords = new FastList<Vector2f>(possibleVertices);
                if(currentMesh.weightIndex!=null) {
                    matMesh.weightIndex = new FastList<int[]>(possibleVertices);
                }
                if(currentMesh.weightValue!=null) {
                    matMesh.weightValue = new FastList<float[]>(possibleVertices);
                }
                
                // put it into the list
                meshByMaterial.set(matIndex, matMesh);
            }
            
            // get the start index
            int startIndex = currentMesh.indiceStart.get(i);
            // get the end index
            int endIndex;
            if(i==currentMesh.matIndices.size()-1) {
                // last index range
                endIndex = currentMesh.indices.size()-1;
            } else {
                // not last, its the next-1
                endIndex = currentMesh.indiceStart.get(i+1)-1;
            }
            int[] partIndexMap = vertexIndices.get(matIndex);
            if(partIndexMap==null) {
                // not created yet, create the index map
                partIndexMap = new int[currentMesh.vertexPosition.size()];
                Arrays.fill(partIndexMap, -1);
                vertexIndices.set(matIndex, partIndexMap);
            }
            // put indices into the proper list
            for(int j=startIndex; j<=endIndex; j++) {
                // the index in the original mesh
                int index = currentMesh.indices.get(j);
                // is there already this index in the part?
                int newIndex = partIndexMap[index];
                if(newIndex==-1) {
                    // this vertex is not yet used in the part mesh
                    
                    // new index is the last one
                    newIndex = matMesh.vertexPosition.size();
                    // copy over vertex data
                    matMesh.vertexPosition.set(newIndex, currentMesh.vertexPosition.get(index));
                    if(matMesh.colors!=null)
                        matMesh.colors.set(newIndex, currentMesh.colors.get(index));
                    if(this.processnormals)
                        matMesh.normals.set(newIndex, currentMesh.normals.get(index));
                    matMesh.textureCoords.set(newIndex, currentMesh.textureCoords.get(index));
                    // copy over weights and weight values
                    if(currentMesh.weightIndex!=null) {
                        int[] weightIndex = currentMesh.weightIndex.get(index);
                        matMesh.weightIndex.set(newIndex, weightIndex);
                        // calculate weights in this vertex
                        int w = 0;
                        for(w = 0; w<weightIndex.length && weightIndex[w]!=0; w++){}
                        // if its more than the max, then set it as the max
                        if(w>matMesh.maxWeights)
                            matMesh.maxWeights = w;
                        
                    }
                    if(currentMesh.weightValue!=null) {
                        matMesh.weightValue.set(newIndex, currentMesh.weightValue.get(index));
                    }
                    // mark the used index
                    partIndexMap[index] = newIndex;
                }
                // put the index into part indices
                matMesh.indices.add(newIndex);
            }
        }
        this.processedMeshes.add(meshByMaterial);
    }    
    
    
    private void createModel() {
        // beak up meshes more if they reference more than a limit of bones
        // go over the processed mesh parts, and for each part calculate the used bones
        // break up parts that have more than the limit of bones
        
        // the resulting broken up meshes
        FastList<XMesh> allMeshes = new FastList<XMesh>();
        
        for(int m=0; m<processedMeshes.size(); m++) {
            FastList<XMesh> meshByMaterial = processedMeshes.get(m);
            // go over used meshes
            for(int i=0; i<meshByMaterial.size(); i++) {
                XMesh matMesh = meshByMaterial.get(i);
                if(matMesh==null)
                    continue;
                
                if(this.anims!=null && matMesh.weightIndex!=null) {
                    FastList<XMesh> brokenMeshes = breakupMeshByBones(matMesh);
                    allMeshes.addAll(brokenMeshes);
                } else {
                    // add the mesh to the result
                    allMeshes.add(matMesh);
                }
            }
        }

        // go over used meshes
        for(int i=0; i<allMeshes.size(); i++) {
            XMesh matMesh = allMeshes.get(i);
            if(matMesh==null)
                continue;

            // create a modelpart
            ModelPart mp = new ModelPart();
            // set name based on mesh name
            mp.setName(matMesh.name);
            // set material
            mp.setMaterial(obj.getMaterials().get(matMesh.materialName));
            // create the geometry
            Geometry geom;
            if(matMesh.boneMapping!=null) {
                // if we have a bone mapping, create the proper geometry to hald that data
                geom = new XWeightedGeometry();
                ((XWeightedGeometry)geom).setBoneMapping(matMesh.boneMapping);
            } else {
                geom = new Geometry();
            }
            mp.setGeometry(geom);

            int vbomode = BaseGeometry.VBO_NO;
            // if we have params, set VBO mode into the geom
            if(params!=null) {
                int listmode = params.getInt("listmode", BaseGeometry.LIST_NO);
                if(listmode!=BaseGeometry.LIST_NO) {
                    geom.setDisplayListMode(listmode);
                } else {
                    vbomode = params.getInt("vbomode", BaseGeometry.VBO_NO);
                    if(vbomode!=BaseGeometry.VBO_NO) {
                        geom.setVBOMode(vbomode);
                    }
                }
            }
            
            if(matMesh.weightIndex!=null) {
                geom.setDisplayListMode(BaseGeometry.LIST_NO);
                if(geom.getVBOMode()==BaseGeometry.VBO_NO) {
                    vbomode = BaseGeometry.VBO_LONGLIVED;
                    geom.setVBOMode(vbomode);
                }
            }
            int numVertex = matMesh.vertexPosition.size();
            // create the final indexbuffer
            IndexBuffer idx = IndexBuffer.createBuffer(matMesh.indices.size(), numVertex, null);
            // set index buffer
            geom.setIndexBuffer(idx);
            idx.put(matMesh.indices);
            idx.rewind();
            // create position buffer
            VertexBuffer posB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_POSITION, numVertex);
            if(vbomode!=BaseGeometry.VBO_NO) {
                posB.setVBOInfo(new VBOAttributeInfo());
            }
            geom.addAttribBuffer(posB, 0);
            FloatBuffer posFb = posB.getDataBuffer();
            posFb.clear();
            // create normal buffer
            VertexBuffer normB=null;
            FloatBuffer normFb=null;
            if(processnormals) {
                normB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_NORMAL, numVertex);
                if(vbomode!=BaseGeometry.VBO_NO) {
                    normB.setVBOInfo(new VBOAttributeInfo());
                }
                geom.addAttribBuffer(normB, 0);
                normFb = normB.getDataBuffer();
                normFb.clear();
            }
            // create texture buffer
            VertexBuffer texB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_TEXTURE0, numVertex);
            if(vbomode!=BaseGeometry.VBO_NO) {
                texB.setVBOInfo(new VBOAttributeInfo());
            }
            geom.addAttribBuffer(texB, 0);
            FloatBuffer texFb = texB.getDataBuffer();
            texFb.clear();
            // create color buffer
            FloatBuffer colorFb = null;
            if(matMesh.colors != null) {
                VertexBuffer colorB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_COLOR, numVertex);
                if(vbomode!=BaseGeometry.VBO_NO) {
                    colorB.setVBOInfo(new VBOAttributeInfo());
                }
                geom.addAttribBuffer(colorB, 0);
                colorFb = colorB.getDataBuffer();
                colorFb.clear();
            }
            // if we have weights, handle those
            VertexBuffer widxB = null;
            FloatBuffer widxFb = null;
            
            VertexBuffer wnumB = null;
            FloatBuffer wnumFb = null;
            
            if(matMesh.weightIndex!=null) {
                widxB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_WEIGHTINDICES, numVertex);
                if(vbomode!=BaseGeometry.VBO_NO) {
                    widxB.setVBOInfo(new VBOAttributeInfo());
                }
                geom.addAttribBuffer(widxB, 0);
                widxFb = widxB.getDataBuffer();
                widxFb.clear();
                
                // buffer for number of weights
                wnumB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_NUMWEIGHTS, numVertex);
                if(vbomode!=BaseGeometry.VBO_NO) {
                    wnumB.setVBOInfo(new VBOAttributeInfo());
                }
                geom.addAttribBuffer(wnumB, 0);
                wnumFb = wnumB.getDataBuffer();
                wnumFb.clear();
            }
            VertexBuffer wvalB = null;
            FloatBuffer wvalFb = null;
            if(matMesh.weightValue!=null) {
                wvalB = VertexBuffer.createSingleBuffer(VertexAttribute.USAGE_WEIGHTS, numVertex);
                if(vbomode!=BaseGeometry.VBO_NO) {
                    wvalB.setVBOInfo(new VBOAttributeInfo());
                }
                geom.addAttribBuffer(wvalB, 0);
                wvalFb = wvalB.getDataBuffer();
                wvalFb.clear();
            }

            // set vertex and index counts
            geom.setNumVertex(numVertex);
            geom.setStartIndex(0);
            geom.setNumIndex(matMesh.indices.size());

            // TODO: binormal and tangent
            // go over vertices and fill the buffers
            for(int j=0; j<numVertex; j++) {
                // position
                Vector3f pos = matMesh.vertexPosition.get(j);
                posFb.put(pos.x);posFb.put(pos.y);posFb.put(pos.z);
                // normal
                if(processnormals) {
                    Vector3f norm = matMesh.normals.get(j);
                    normFb.put(norm.x);normFb.put(norm.y);normFb.put(norm.z);
                }
                // texcoord
                Vector2f tex = matMesh.textureCoords.get(j);
                texFb.put(tex.x);texFb.put(tex.y);
                
                if(colorFb!=null) {
                    // color
                    ColorRGBA col = matMesh.colors.get(j);
                    colorFb.put(col.r);colorFb.put(col.g);colorFb.put(col.b);colorFb.put(col.a);
                }

                // TODO: put in tangent and binormals

                // put in vertex weigt indices
                if(widxFb!=null) {
                    // get the indices for this vertex
                    int[] vertWIdx = matMesh.weightIndex.get(j);
                    // we collect the number of indices
                    int numIdx = 0;
                    // create an int holding the 4 byte values
                    if(vertWIdx!=null) {
                        int widx = 0;
                        for(int k=0; k<4; k++) {
                            //widxFb.put((float)vertWIdx[k]);
                            int cidx = vertWIdx[k];
                            if(cidx!=0)
                                numIdx++;
                            widx |= cidx<<(k*8);
                        }
                        widxFb.put(Float.intBitsToFloat(widx));
                    } else {
                        // this shouldnt happen, bu still handle it
                        //for(int k=0; k<4; k++) {
                        //    widxFb.put(0);
                        //}
                        widxFb.put(0);
                    }
                    // put in the number of indices
                    wnumFb.put(Float.intBitsToFloat(numIdx));
                }
                // put in vertex weigt values
                if(wvalFb!=null) {
                    // get the indices for this vertex
                    float[] vertWVal = matMesh.weightValue.get(j);
                    if(vertWVal!=null) {
                        // we need to normalize values, so that the sum is 1
                        // calculate sum
                        float sum = 0;
                        for(int k=0; k<4; k++) {
                            sum += vertWVal[k];
                        }
                        if(sum!=0) {
                            // calculate a fix-up multiplication value
                            sum = 1.0f / sum;
                        }
                        
                        for(int k=0; k<4; k++) {
                            // TODO: is this ok?
                            wvalFb.put(vertWVal[k]);//*sum);
                        }
                    } else {
                        // this shouldnt happen, bu still handle it
                        for(int k=0; k<4; k++) {
                            wvalFb.put(0f);
                        }
                    }
                }
            }
            posFb.rewind();
            if(normFb!=null)
                normFb.rewind();
            if(texFb!=null)
                texFb.rewind();
            if(colorFb!=null)
                colorFb.rewind();
            if(widxFb!=null)
                widxFb.rewind();
            if(wvalFb!=null)
                wvalFb.rewind();
            if(wnumFb!=null)
                wnumFb.rewind();

            int boundType = params.getInt("bound", BoundingVolume.BOUNDING_BOX);
            if(boundType==BoundingVolume.BOUNDING_SPHERE) {
                geom.setModelBound(new BoundingSphere());
            } else {
                geom.setModelBound(new BoundingBox());
            }
            geom.updateModelBound();

            // set the part into the model as lod 0
            obj.addPart(0, mp);
        }
        
        // check all materials, and put in associated textures
        for(ModelMaterial mm : obj.getMaterials().values() ) {
            FastList<ModelMaterialPart> textures = mm.getTextures();
            // if we only have one texture
            if(textures.size()==1) {
                ModelMaterialPart mp = textures.get(0);
                // do we have a color texture?
                String texname = mp.getTextureName();
                String basename = texname.substring(4, texname.length());
                if(texname.startsWith("clr_")) {
                    // we have a color texture, check if we also have normal or bumpmap
                    checkTexture(basename, ModelMaterialPart.TextureType.BumpMap, textures);
                    checkTexture(basename, ModelMaterialPart.TextureType.NormalMap, textures);
                } else if(texname.startsWith("bum_")) {
                    // we have a bump texture, check if we also have color or normal
                    checkTexture(basename, ModelMaterialPart.TextureType.Diffuse, textures);
                    checkTexture(basename, ModelMaterialPart.TextureType.NormalMap, textures);
                } else if(texname.startsWith("nor_")) {
                    // we have a normal texture, check if we also have color or bump
                    checkTexture(basename, ModelMaterialPart.TextureType.Diffuse, textures);
                    checkTexture(basename, ModelMaterialPart.TextureType.BumpMap, textures);
                }
            }
        }
    }
    
    /**
     * Based on texture name tryes to find another related texture. This is necessary,
     * because X files only hold one texture/material.
     * @param basename
     * @param type
     * @param textures
     */
    private void checkTexture(String basename, ModelMaterialPart.TextureType type, FastList<ModelMaterialPart> textures) {
        String typeS=null;
        switch(type) {
            case Diffuse : typeS = "clr_"; break;
            case BumpMap : typeS = "bum_"; break;
            case NormalMap : typeS = "nor_"; break;
        }
        if(typeS==null)
            return;
        Image img = app.getResourceFinder().getImage(typeS+basename, ParameterMap.MAP_EMPTY);
        if(img!=null) {
            // we have a bumpmap, create a texture
            ModelMaterialPart bump = new ModelMaterialPart();
            bump.setType(type);
            bump.setTextureName(typeS+basename);
            textures.add(bump);
        }
    }
    
    /**
     * This method is responsible for processing the bones.
     */
    private void processBones() {
        
        /*
        XFrame rootFrame = null;
        // find the root frame
        for(int i=0; i<frames.size() && rootFrame == null; i++) {
            XFrame cf = frames.get(i);
            if(cf==null)
                continue;
            if(cf.name.endsWith("root")) {
                // this is the proper root
                rootFrame = cf;
            }
        }
         */
        
        if(rootFrame!=null) {
            // go over, and set root on all frames that dont have parent
            for(int i=0; i<frames.size(); i++) {
                XFrame cf = frames.get(i);
                if(cf==null)
                    continue;
                if(cf.parentFrame==null && cf != rootFrame) {
                    cf.parentFrame = rootFrame;
                }
            }
        }
         
            
        // get the bones list
        FastList<Bone> bones = anims.getBones();
        bones.clear();

        // reset list of processed bones
        boneProcessed = new boolean[frames.size()];
        // go over the frames, and sort them based on the hierarchy
        // find the root bone
        
        for(int i=0; i<frames.size(); i++) {
            XFrame frame = frames.get(i);
            if(frame==null)
                continue;
            if(!boneProcessed[frame.id]) {
                // process al the children
                processBone(frame, bones);
            }
        }
        
        boneProcessed = null;
    }
    
    private void processBone(XFrame frame, FastList<Bone> bones) {
        // if already processed, leave it out
        if(boneProcessed[frame.id]) {
            return;
        }
        // if we have an uprocessed parent, then process that first
        if(frame.parentFrame!=null && !boneProcessed[frame.parentFrame.id]) {
            // we got a parent frame, which is not yet processed, process it
            processBone(frame.parentFrame, bones);
        }
        // create bone
        createBone(frame, bones);
        // mark it as processed
        boneProcessed[frame.id] = true;
    }
    
    private void createBone(XFrame frame, FastList<Bone> bones) {
        frame.bone = new Bone();
        frame.bone.id = frame.id;
        frame.bone.name = frame.name;
        frame.bone.matrixOffset = frame.matrixOffset;
        frame.bone.frameMatrix = frame.transformMatrix;
        
        // parent is frames parent
        if(frame.parentFrame!=null) {
            frame.bone.parent = frame.parentFrame.bone;
        }
        bones.set(frame.bone.id, frame.bone);
    }
    
    /**
     * Breaks up a mesh, so that it only references atmost Config.p_bone_limit 
     * bones.
     * @return
     */
    private FastList<XMesh> breakupMeshByBones(XMesh mesh) {
        
        // the bone limit to impose
        int boneLimit = app.conf.p_bone_limit;

        FastList<XMesh> store = new FastList<XMesh>();
        
        
        // go over the triagles of the mesh, and create triangles
        FastList<int[]> triangles = new FastList<int[]>();
        // the weights used in each triangle
        FastList<IntList> triangleWeightIndices = new FastList<IntList>();
        
        for(int i=0; i<mesh.indices.size(); i+=3) {
            // create a triangle
            int[] triangle = new int[3];
            IntList weightIndices = new IntList(4);
            for(int j=0; j<3; j++) {
                triangle[j] = mesh.indices.get(i+j);
                // go over the weight indices of the vertex
                int[] vertWeightIndex = mesh.weightIndex.get(triangle[j]);
                for(int k=0; k<vertWeightIndex.length; k++) {
                    int weightIndex = vertWeightIndex[k];
                    if(weightIndex==0)
                        continue;
                    // add the index to the triangles weight indices
                    if(!weightIndices.contains(weightIndex))
                        weightIndices.add(weightIndex);
                }
            }
            // add the triangle and weight indices to the lists
            triangles.add(triangle);
            triangleWeightIndices.add(weightIndices);
        }
                
        boolean finish = false;
        // while we still have triangles to process
        while(!finish) {
            // we count how much bones we have gathered
            int numGatheredBones = 0;
            // we note the collected bones
            IntList boneMap = new IntList();
            // the bone map always contains reference to the null bone
            boneMap.add(0);
            // the collected result triangles
            FastList<int[]> newMeshTriangles = new FastList<int[]>();
            // go over all the triangles
            for(int k=0; k<triangles.size(); k++) {
                int[] triangle = triangles.get(k);
                // we skip already processed triangles
                if(triangle==null)
                    continue;
                // either we already have all the bones used by the triangle
                // or we are under the bone limit
                
                // check how many new bones this triangle has
                int numNewBones = 0;
                IntList weightIndices = triangleWeightIndices.get(k);
                for(int l=0; l<weightIndices.size(); l++) {
                    int boneId = weightIndices.get(l);
                    if(!boneMap.contains(boneId))
                        numNewBones++;
                }
                // would we go over the limit with the new bones?
                if(numGatheredBones+numNewBones>boneLimit)
                    continue;
                
                // we can fit the new triangle
                
                // set the bone as in use
                for(int l=0; l<weightIndices.size(); l++) {
                    int boneId = weightIndices.get(l);
                    if(!boneMap.contains(boneId)) {
                        numGatheredBones++;
                        // create local ids map for the bones
                        boneMap.add(boneId);
                    }
                }
                
                // add the triangle to the finished
                // add it to the finished 
                newMeshTriangles.add(triangle);
                
                // remove triangle from the list
                triangles.set(k, null);
                // remove weight indices from the list
                triangleWeightIndices.set(k, null);
            }
            // if we did not collect anything
            if(numGatheredBones==0) {
                finish = true;
                continue;
            }
                
            
            IntList exact = null;
            // check if the bone set is also storen in another mesh
            if(boneSets==null) {
                boneSets = new FastList<IntList>();
            } else {
                for(int i=0; i<boneSets.size() && exact == null; i++) {
                    IntList boneset = boneSets.get(i);
                    if(boneMap.containsAll(boneset) && boneset.containsAll(boneMap)) {
                        exact = boneSets.get(i);
                    }
                }
            }
            if(exact!=null) {
                boneMap = exact;
            } else {
                // this bone configuration is not yet found, add it to the list
                boneSets.add(boneMap);
            }
            
            // we have a new mesh to construct
            XMesh broken = new XMesh();
            // copy some data over
            broken.name = mesh.name;
            broken.materialName = mesh.materialName;
            broken.boneMapping = boneMap;
            broken.maxWeights = 0;
            
            // create indices
            broken.indices = new IntList(newMeshTriangles.size()*3);
            
            // calculate the probable number of vertices, based on ratio of triangles we collected
            int possibleVertices = (int) ((float) mesh.vertexPosition.size() * (((float) newMeshTriangles.size() * 3.0f) / (float)mesh.indices.size()));
            
            // create vertex buffers
            broken.vertexPosition = new FastList<Vector3f>(possibleVertices);
            if(this.processcolors && mesh.colors != null)
                broken.colors = new FastList<ColorRGBA>(possibleVertices);
            if(this.processnormals && mesh.normals != null)
                broken.normals = new FastList<Vector3f>(possibleVertices);
            broken.textureCoords = new FastList<Vector2f>(possibleVertices);
            // we are sure we will need these
            broken.weightIndex = new FastList<int[]>(possibleVertices);
            if(mesh.weightValue!=null) {
                broken.weightValue = new FastList<float[]>(possibleVertices);
            }
            
            // the map of vertices in the new mesh
            int[] partIndexMap = new int[mesh.vertexPosition.size()];
            Arrays.fill(partIndexMap, -1);
            
            // go over triangles
            for(int i=0; i<newMeshTriangles.size(); i++) {
                int[] triangle = newMeshTriangles.get(i);
                for(int j=0; j<3; j++) {
                    int index = triangle[j];
                    // is there already this index in the part?
                    int newIndex = partIndexMap[index];
                    if(newIndex==-1) {
                        // this vertex is not yet used in the part mesh

                        // new index is the last one
                        newIndex = broken.vertexPosition.size();
                        // copy over vertex data
                        broken.vertexPosition.set(newIndex, mesh.vertexPosition.get(index));
                        if(broken.colors!=null)
                            broken.colors.set(newIndex, mesh.colors.get(index));
                        if(broken.normals!=null)
                            broken.normals.set(newIndex, mesh.normals.get(index));
                        broken.textureCoords.set(newIndex, mesh.textureCoords.get(index));
                        // TODO: binormal and tangent

                        // create weight indices as mapped indices
                        int[] oldWeightIndex = mesh.weightIndex.get(index);
                        int[] newWeightIndex = new int[oldWeightIndex.length];
                        int l=0;
                        for(l=0; l<newWeightIndex.length && oldWeightIndex[l]!=0; l++) {
                            newWeightIndex[l] = boneMap.indexOf(oldWeightIndex[l]);
                        }
                        // if we have more weights than max
                        if(l>broken.maxWeights)
                            broken.maxWeights = l;
                        
                        broken.weightIndex.set(newIndex, newWeightIndex);
                        // copy over weight values
                        broken.weightValue.set(newIndex, mesh.weightValue.get(index));
                        // mark the used index
                        partIndexMap[index] = newIndex;
                    }
                    // put the index into part indices
                    broken.indices.add(newIndex);
                }
            }
            broken.numVertex = broken.vertexPosition.size();
            store.add(broken);
        }
        
        return store;
    }
}
