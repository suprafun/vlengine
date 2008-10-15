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

package com.vlengine.resource.obj;

import com.vlengine.app.AppContext;
import com.vlengine.image.Image;
import com.vlengine.math.FastMath;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.resource.ParameterMap;
import com.vlengine.resource.model.Model;
import com.vlengine.resource.model.ModelMaterial;
import com.vlengine.resource.model.ModelMaterialPart;
import com.vlengine.util.BufferInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads .obj files into Model classes
 * 
 * @author Jack Lindamood
 * @author Joshua Slack - revamped to improve speed
 * @author lex (Aleksey Nikiforov) - fixed parsing of texturing parameters,
 *         added scaled textures, bumpmaps and bumpmap handler
 * @author vear (Arpad Vekas) reworked for VL engine, loading is independent of
 *         scene objects creation (which is in Model class independent of OBJ)
 */
public class ObjLoader {
    private static final Logger log = Logger.getLogger(ObjLoader.class
            .getName());
    
    private BufferedReader inFile;

    // the object we are currently creating
    private ObjModel obj;
    
    // the material lib we are currently creating
    private ObjMtlLib mtllib;
    
    /** Last 'material' flag in the file */
    protected ModelMaterial curGroup;

    private AppContext app;
    
    private Vector3f translation = new Vector3f();
    private Vector3f defaultTranslation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f();
    private Vector3f defaultScale = new Vector3f(1, 1, 1);
    

    private int smoothpos = 0;
    private int numface = 0;
    private int numflipped = 0;

    public ObjLoader(AppContext app) {
        this.app = app;
    }
    
    public Model convert(ByteBuffer modelData, ParameterMap params) {

        translation.set(defaultTranslation);
        scale.set(defaultScale);

        try {

            // get a stream to our buffer
            InputStream format = new BufferInputStream(modelData);

            
            obj = new ObjModel(params);

            inFile = new BufferedReader(new InputStreamReader(format));
            String in;

            curGroup = obj.defaultMaterialGroup;
            while ((in = inFile.readLine()) != null) {
                processLine(in);
            }
            inFile.close();
            // process the last smooth group
            processSmooth(curGroup, curGroup);
        } catch (IOException ex) {
            Logger.getLogger(ObjLoader.class.getName()).log(Level.SEVERE, null, ex);
            obj = null;
        }

        curGroup = null;
        inFile = null;
        mtllib = null;
        
        Model retobj = null;
        if(obj != null)
            retobj = obj.buildModel();
        obj = null;
        return retobj;
    }
    
    public ObjMtlLib convertMtl(ByteBuffer mtlData, ParameterMap params) {

        translation.set(defaultTranslation);
        scale.set(defaultScale);
        
        try {

            // get a stream to our buffer
            InputStream format = new BufferInputStream(mtlData);

            mtllib = new ObjMtlLib(params);

            inFile = new BufferedReader(new InputStreamReader(format));
            String in;

            //curGroup = obj.defaultMaterialGroup;
            while ((in = inFile.readLine()) != null) {
                processLine(in);
            }
            inFile.close();
        } catch (IOException ex) {
            Logger.getLogger(ObjLoader.class.getName()).log(Level.SEVERE, null, ex);
            mtllib = null;
        }

        curGroup = null;
        inFile = null;
        
        ObjMtlLib retobj = mtllib;
        mtllib = null;
        return retobj;
    }
    
    /**
     * Processes a line of text in the .obj file.
     * 
     * @param line
     *            The line of text in the file.
     * @throws IOException
     */
    private void processLine(String line) throws IOException {
        if (line == null || line.length() == 0) return;
        
        Tokens tokens = new Tokens(line);
        if (tokens.isEmpty()) return;
        String token = tokens.pop();
        
        if (token.charAt(0) == '#') {
            return;
        } else if ("v".equals(token)) {
        	obj.vertexList.add(populate(new Vector3f(), tokens));
            return;
        } else if ("vt".equals(token)) {
        	obj.textureList.add(populate(new Vector2f(), tokens));
            return;
        } else if ("vn".equals(token)) {
        	obj.normalList.add(populate(new Vector3f(), tokens));
            return;
        } else if ("g".equals(token)) {
            // see what the material name is if there isn't a name, assume its
            // the default group
        	setCurrentGroup(tokens);
            return;
        } else if ("f".equals(token)) {
            addFaces(tokens);
            return;
        } else if ("mtllib".equals(token)) {
            loadMaterials(tokens);
            return;
        } else if ("newmtl".equals(token)) {
            addMaterial(tokens.pop());
            return;
        } else if ("usemtl".equals(token)) {
            setCurrentGroup(tokens);
            return;
        } else if ("Ka".equals(token)) {
            ColorRGBA amb = populate(new ColorRGBA(), tokens);
            if(amb.r !=0 || amb.g != 0 || amb.b != 0)
                curGroup.setAmbient( amb );
            return;
        } else if ("Kd".equals(token)) {
            ColorRGBA clr = populate(new ColorRGBA(), tokens);
            if(clr.r !=0 || clr.g != 0 || clr.b != 0)
                curGroup.setDiffuse( clr );
            return;
        } else if ("Ks".equals(token)) {
            ColorRGBA clr = populate(new ColorRGBA(), tokens);
            if(clr.r !=0 || clr.g != 0 || clr.b != 0)
                curGroup.setSpecular(clr);
            return;
        } else if ("Ni".equals(token)) {
            // optical density, how much light is bent when it passes the surface
            if (tokens.isEmpty())
                return;
            Float val = getFloat(tokens.pop());
            float refract_index = val;
            curGroup.setRefractionIndex(refract_index);
            return;
        } else if ("Tf".equals(token)) {
            // color filter, how much of RGB color remains after passing trough the material
            // multiply apply mode?
            ColorRGBA tr = new ColorRGBA();
            curGroup.setTransmissive(populate(tr, tokens));
            // normalize
            float max = 0.01f;
            if(tr.r > max )
                max = tr.r;
            if(tr.g > max )
                max = tr.g;
            if(tr.b > max )
                max = tr.b;
            tr.a = max;
            float m = 1/max;
            tr.r *= m;
            tr.g *= m;
            tr.b *= m;
            return;
        } else if ("Ns".equals(token)) {
        	if (tokens.isEmpty()) {
        		log.log(Level.SEVERE, "Unexpected end of line.");
        		return;
        	}
        	Float val = getFloat(tokens.pop());
        	if (val == null) {
        		log.log(Level.SEVERE, "Unable to parse float.");
        		return;
        	}
            float shine = val;
            if (shine > 128) {
                shine = 128;
            } else if (shine < 0) {
                shine = 0;
            }
            curGroup.setShininess( shine);
            return;
        } else if ("d".equals(token)) {
        	if (tokens.isEmpty()) {
        		log.log(Level.SEVERE, "Unexpected end of line.");
        		return;
        	}
        	Float val = getFloat(tokens.pop());
        	if (val == null) {
        		log.log(Level.SEVERE, "Unable to parse float.");
        		return;
        	}
            curGroup.setAlpha((float) val);
            curGroup.setDissolve((float) val);
            if (val < 1.0f) {
                curGroup.setAlphaTest(true);
                curGroup.setAlphaBlend(true);
            }
            return;
        } else if ("map_d".equals(token)) {
            curGroup.setAlphaTest(true);
            curGroup.setAlphaBlend(true);
            return;
        } else if ("map_Kd".equals(token) || "map_Ka".equals(token)) {
            // opaque texture
            ModelMaterialPart subma = processTexture(line, tokens, ModelMaterialPart.TextureType.Diffuse);
            curGroup.addTexture(subma);
            return;
        } else if ("bump".equals(token)) {
            // bump texture
            ModelMaterialPart subma = processTexture(line, tokens, ModelMaterialPart.TextureType.NormalMap);
            curGroup.addTexture(subma);
            return;
        } else if ("o".equals(token)) {
        	if (tokens.isEmpty()) {
        		log.log(Level.SEVERE, "Unexpected end of line.");
        		return;
        	}
            obj.curObjectName = tokens.pop();
            log.info("Object:" + obj.curObjectName);
            return;
        } else if ("g".equals(token) || "s".equals(token)) {
            processSmooth(curGroup, curGroup);
            return;
        }
    }
    
    private ModelMaterialPart processTexture(String line, Tokens tokens, ModelMaterialPart.TextureType type)
    throws IOException {
    	ModelMaterialPart tex = new ModelMaterialPart();
        tex.setType(type);
        translation.set(defaultTranslation);
        scale.set(defaultScale);
        
        while (!tokens.isEmpty()) {
        	String token = tokens.pop();
        	
        	if (token.equals("-o")) {
                    stuffVector(translation, tokens);
        	} else if (token.equals("-s")) {
                    stuffVector(scale, tokens);
        	} else if (token.equals("-blendu")
        			|| token.equals("-blendv")
        			|| token.equals("-cc")
        			|| token.equals("-clamp")) {
                    tokens.pop();
                    log.log(Level.WARNING,
                                    "Unsupported option \"{0}\" in mtl file.", token);
        	} else if (token.equals("-mm")) {
        		tokens.pop();
        		tokens.pop();
        		log.log(Level.WARNING,
						"Unsupported option \"{0}\" in mtl file.", token);
        	} else if (token.equals("-texres")) {
        		popNumericData(tokens, 1);
        		log.log(Level.WARNING,
						"Unsupported option \"{0}\" in mtl file.", token);
        	} else if (token.equals("-t")) {
        		popNumericData(tokens, 3);
        		log.log(Level.WARNING,
						"Unsupported option \"{0}\" in mtl file.", token);
        	} else {
                    int start = line.indexOf(token);
                    int end = line.indexOf(" -bm", start);
                    if (end > start) {
                        tex.setTextureName(line.substring(start, end));
                    } else {
                        tex.setTextureName(line.substring(start));
                    }
                    // check out if the texture has alpha channel, and is a colormap (type 0)
                    // get param
                    boolean checkAlphaTexture = true;
                    if(this.obj != null)
                        checkAlphaTexture = this.obj.params.getBoolean("obj_check_texture_alpha_chanel", true);
                    else if(this.mtllib != null)
                        checkAlphaTexture = this.mtllib.params.getBoolean("obj_check_texture_alpha_chanel", true);
                    if(type==ModelMaterialPart.TextureType.Diffuse && checkAlphaTexture) {
                        String texname = tex.getTextureName();
                        // load texture
                        Image t = app.getResourceFinder().getImage(texname, ParameterMap.MAP_EMPTY);
                        if (t!=null && t.hasAlpha()) {
                            curGroup.setAlphaTest(true);
                        }
                    }
                    // TODO: automaticaly find bump texture corresponding to normal texture
                    break;
        	}
        }
        if (!translation.equals(defaultTranslation)) {
        	tex.setTranslation(new Vector3f().set(translation));
        }
        if (!scale.equals(defaultScale)) {
        	scale.x = 1/scale.x;
        	scale.y = 1/scale.y;
        	scale.z = 1/scale.z;
        	tex.setScale(new Vector3f().set(scale));
        }
        return tex;
    }

    private void setCurrentGroup(Tokens tokens) {
        // process the last smooth group
        
    	ModelMaterial newGroup = null;
    	
        if (!tokens.isEmpty()) {
            String groupName = tokens.pop();
            if(mtllib == null) {
                newGroup = obj.defaultMaterialGroup;
            }
            if(newGroup==null) {
                newGroup = mtllib.materialNames.get(groupName);
            }
        } else {
            log.log(Level.WARNING, "Unexpected end of line " +
                            "when setting material group.");
        }
        
        if (newGroup == null) {
            newGroup = obj.defaultMaterialGroup;
        }
        
        processSmooth(curGroup, newGroup);
        curGroup = newGroup;
    }
    
    /**
     * This method will extract as many numeric values as possible
     * (up to the vector's capacity).
     * 
     * @param vector
     * @param tokens
     */
    private void stuffVector(Vector3f vector, Tokens tokens) {
    	for (int i = 0; i < 3; i++) {
            Float val = getFloat(tokens.peek());
            if (val == null) return;

            tokens.pop();
            vector.set(i, val);
    	}
    }
    
    private Vector2f populate(Vector2f vector, Tokens tokens) {
    	for (int i = 0; i < 2; i++) {
    		Float val = getFloat(tokens.peek());
    		if (val == null) {
    			log.log(Level.SEVERE, "Unable to fully parse Vector2f.");
    			break;
    		}
    		
    		tokens.pop();
    		if (i == 0) vector.x = val;
    		if (i == 1) vector.y = val;
    	}
    	
    	return vector;
    }
    
    private Vector3f populate(Vector3f vector, Tokens tokens) {
    	for (int i = 0; i < 3; i++) {
    		Float val = getFloat(tokens.peek());
    		if (val == null) {
    			log.log(Level.SEVERE, "Unable to fully parse Vector3f.");
    			break;
    		}
    		
    		tokens.pop();
    		vector.set(i, val);
    	}
    	
    	return vector;
    }
    
    private float[] colors = new float[3];
    private ColorRGBA populate(ColorRGBA color, Tokens tokens) {
    	for (int i = 0; i < 3; i++) {
            Float val = getFloat(tokens.peek());
            if (val == null) {
                log.log(Level.SEVERE, "Unable to fully parse color.");
                break;
            }
            tokens.pop();
            colors[i] = val;
    	}
    	
    	color.set(colors[0], colors[1], colors[2], 1);
    	return color;
    }
    
    private int popNumericData(Tokens tokens, int count) {
    	for (int i = 0; i < count; i++) {
            Float val = getFloat(tokens.peek());
            if (val == null) return i;

            tokens.pop();
    	}
    	
    	return count;
    }
    
    private Float getFloat(String s) {
    	if (s == null) return null;
    	
    	try {
    		return new Float(s);
    	} catch (NumberFormatException e) {
    		return null;
    	}
    }

    private void addMaterial(String matName) {
        ModelMaterial newMat = new ModelMaterial();
        newMat.setId(matName);
        if(mtllib == null) {
            // we got material in obj file?
            obj.mllib = new ObjMtlLib(obj.params);
            mtllib = obj.mllib;
        }
        mtllib.materialNames.put(matName, newMat);
        if(obj!=null)
            obj.materialSets.put(newMat, new ArraySet());
        curGroup = newMat;
    }

    private void loadMaterials(Tokens tokens) {
        while(!tokens.isEmpty()) {
            ObjMtlLib mtlib = app.getResourceFinder().getObjMaterialLib(tokens.pop(), obj.params);
            if(mtlib!=null) {
                if(obj!=null)
                    obj.mllib = mtlib;
                for(ModelMaterial om : mtlib.materialNames.values()) {
                    obj.materialSets.put(om, new ArraySet());
                }
                this.mtllib = mtlib;
            }
        }
    }
    
    private void processMaterialFile(InputStream inputStream)
            throws IOException {
        BufferedReader matFile = new BufferedReader(new InputStreamReader(
                inputStream));
        String in;
        while ((in = matFile.readLine()) != null) {
            processLine(in);
        }
    }

    private void addFaces(Tokens tokens) {
        ArraySet thisMat = obj.materialSets.get(curGroup);
        if (thisMat.objName == null && obj.curObjectName != null)
            thisMat.objName = obj.curObjectName;
        IndexSet first = new IndexSet(tokens.pop());
        int firstIndex = thisMat.findSet(first);
        IndexSet second = new IndexSet(tokens.pop());
        int secondIndex = thisMat.findSet(second);
        while (!tokens.isEmpty()) {
            IndexSet third = new IndexSet(obj);
            third.parseStringArray(tokens.pop());
            int thirdIndex = thisMat.findSet(third);
            // generate face normal
            Vector3f v = new Vector3f(obj.vertexList.get(second.vIndex));
            Vector3f w = new Vector3f(obj.vertexList.get(third.vIndex));
            v.subtractLocal(obj.vertexList.get(first.vIndex));
            w.subtractLocal(obj.vertexList.get(first.vIndex));
            v.crossLocal(w);
            v.normalizeLocal();
            int sec = secondIndex;
            int thr = thirdIndex;
            // if there are already vertex normals, add them together
            if (first.nIndex != -1 && second.nIndex != -1 && third.nIndex != -1 
                    && obj.params.getBoolean("obj_allow_face_flip", true)) {
                int flipped = 0;
                Vector3f n1 = obj.normalList.get(first.nIndex);
                float angle1 = v.angleBetween(n1);
                if(angle1>(FastMath.DEG_TO_RAD * 160)) {
                    flipped++;
                }
                Vector3f n2 = obj.normalList.get(second.nIndex);
                float angle2 = v.angleBetween(n2);
                if(angle2>(FastMath.DEG_TO_RAD * 160)) {
                    flipped++;
                }
                Vector3f n3 = obj.normalList.get(third.nIndex);
                float angle3 = v.angleBetween(n3);
                if(angle3>(FastMath.DEG_TO_RAD * 160)) {
                    flipped++;
                }
                Vector3f fn = new Vector3f();
                fn.set(n1).addLocal(n2).addLocal(n3);
                fn.normalizeLocal();
                // if the angle between normal in the file and the generated are not in
                // the same direction, flip the face
                
                float angle = fn.angleBetween(v);
                if(flipped>2 && angle>(FastMath.DEG_TO_RAD * 160)) {
                    // change second and thrird index
                    this.numflipped++;
                }
            }
            thisMat.indexes.add(firstIndex);
            thisMat.indexes.add(sec);
            thisMat.indexes.add(thr);
            this.numface++;
            if (first.nIndex == -1 || second.nIndex == -1 || third.nIndex == -1) {
                // Generate flat face normal.  TODO: Smoothed normals?

                obj.genNormalList.add(v);
                int genIndex = (-1 * (obj.genNormalList.size() - 1)) - 2;
                if (first.nIndex == -1) {
                    first.nIndex = genIndex;
                }
                if (second.nIndex == -1) {
                    second.nIndex = genIndex;
                }
                if (third.nIndex == -1) {
                    third.nIndex = genIndex;
                }
            }
            secondIndex = thirdIndex; // The second will be the same as the
                                        // last third
        }
    }

    private void processSmooth(ModelMaterial oldmat, ModelMaterial newmat) {
        ArraySet thisMat=null;
        if(oldmat!=null) {
            thisMat = obj.materialSets.get(oldmat);
            if(numface>0 && this.numface == this.numflipped) {
                // the whole group needs to be flipped
                int mpos = thisMat.indexes.size();
                for(int i=this.smoothpos; i<mpos; i+=3) {
                    // change the second and third index
                    int second = thisMat.indexes.get(i+1);
                    int third = thisMat.indexes.get(i+2);
                    thisMat.indexes.set(i+1, third);
                    thisMat.indexes.set(i+2, second);
                }
            }
        }
        numface = 0;
        numflipped = 0;
        smoothpos = 0;
        if(newmat!=null) {
            thisMat = obj.materialSets.get(newmat);
            smoothpos = thisMat.indexes.size();
        }
    }
}
