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

package com.vlengine.renderer.lwjgl;

import com.vlengine.image.Texture;
import com.vlengine.math.FastMath;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Transform;
import com.vlengine.math.Vector3f;
import com.vlengine.model.BaseGeometry;
import com.vlengine.model.Geometry;
import com.vlengine.renderer.ColorRGBA;
import com.vlengine.renderer.Renderer;
import com.vlengine.renderer.CullContext;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.VBOAttributeInfo;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.material.Material;
import com.vlengine.renderer.material.ShaderKey;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.Node;
import com.vlengine.scene.Renderable;
import com.vlengine.scene.SceneElement;
import com.vlengine.scene.Text;
import com.vlengine.scene.batch.TextBatch;
import com.vlengine.scene.batch.TriBatch;
import com.vlengine.scene.state.RenderState;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.ShaderParameters;
import com.vlengine.scene.state.TextureState;
import com.vlengine.scene.state.lwjgl.LWJGLAlphaBlendState;
import com.vlengine.scene.state.lwjgl.LWJGLAlphaTestState;
import com.vlengine.scene.state.lwjgl.LWJGLClipState;
import com.vlengine.scene.state.lwjgl.LWJGLColorMaskState;
import com.vlengine.scene.state.lwjgl.LWJGLCullState;
import com.vlengine.scene.state.lwjgl.LWJGLFogState;
import com.vlengine.scene.state.lwjgl.LWJGLLightState;
import com.vlengine.scene.state.lwjgl.LWJGLLineState;
import com.vlengine.scene.state.lwjgl.LWJGLMaterialState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderObjectsState;
import com.vlengine.scene.state.lwjgl.LWJGLShaderParameters;
import com.vlengine.scene.state.lwjgl.LWJGLStencilState;
import com.vlengine.scene.state.lwjgl.LWJGLTextureState;
import com.vlengine.scene.state.lwjgl.LWJGLWireframeState;
import com.vlengine.scene.state.lwjgl.LWJGLZBufferState;
import com.vlengine.scene.state.shader.ShaderVariableLocation;
import com.vlengine.scene.state.shader.ShaderVertexAttribute;
import com.vlengine.system.VleException;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import com.vlengine.util.FastList;
import com.vlengine.util.IntList;
import com.vlengine.util.WeakIdentityCache;
import com.vlengine.util.geom.BufferUtils;
import com.vlengine.util.geom.IndexBuffer;
import com.vlengine.util.geom.IndexBufferInt;
import com.vlengine.util.geom.IndexBufferShort;
import com.vlengine.util.geom.VertexAttribute;
import com.vlengine.util.geom.VertexBuffer;
import com.vlengine.util.geom.VertexFormat;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.EXTCompiledVertexArray;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.glu.GLU;

/**
 *
 * @author vear (Arpad Vekas) reworked for VL engine
 */
public class LWJGLRenderer extends Renderer {

    private int prevTextureNumber = 0;
    private boolean generatingDisplayList = false;
    protected WeakIdentityCache<Buffer, Integer> vboMap = new WeakIdentityCache<Buffer, Integer>();
    private IntList vboCleanupCache = new IntList();
    private IntBuffer idBuff = BufferUtils.createIntBuffer(16);
    private static final Logger logger = Logger.getLogger(LWJGLRenderer.class.getName());
    // temp variable
    private Vector3f vRot = new Vector3f();
    private LWJGLFont font;
    private boolean supportsVBO = false;
    private boolean indicesVBO = false;
    private FloatBuffer prevVerts;
    private FloatBuffer prevNorms;
    private FloatBuffer prevColor;
    private FloatBuffer[] prevTex;
    private int prevNormMode = GL11.GL_ZERO;
    protected org.lwjgl.opengl.ContextCapabilities capabilities;
    protected boolean[] vertexattribs = new boolean[VertexAttribute.USAGE_MAX];
    protected BaseGeometry prevGeom = null;
    protected boolean prevIndexVbo = false;
    protected boolean prevPositionOnlyMode = false;
    protected IntList shaderattribs = new IntList();

    // creates a renderer that matches the capabilities of
    // the current platform
    public static LWJGLRenderer createMatchingRenderer(int width, int height) {
        LWJGLRenderer chosen = null;
        ContextCapabilities cap = GLContext.getCapabilities();
        LWJGLShaderObjectsState.init();
        if (!cap.GL_ARB_multitexture) {
            // does not support multitexture
            chosen = new LWJGLRendererNoMultitexture(width, height);
        } else if (!cap.GL_ARB_vertex_buffer_object) {
            // no VBO support
            chosen = new LWJGLRendererNoVBO(width, height);
        } else if (!(LWJGLShaderObjectsState.isSupported())) {
            // no shader support
            chosen = new LWJGLRendererNoShader(width, height);
        } else {
            // renderer with full shader support
            chosen = new LWJGLRendererShader(width, height);
        }
        return chosen;
    }

    public LWJGLRenderer(int width, int height) {
        if (width <= 0 || height <= 0) {
            logger.warning("Invalid width and/or height values.");
            throw new VleException("Invalid width and/or height values.");
        }
        this.width = width;
        this.height = height;

        logger.info("LWJGLRenderer created. W:  " + width + "H: " + height);

        capabilities = GLContext.getCapabilities();

        //queue = new RenderQueue(this);

        // get texturing capabilities
        LWJGLTextureState.init();

        // get shader capabilities
        LWJGLShaderObjectsState.init();

        prevTex = new FloatBuffer[TextureState.getNumberOfTotalUnits()];
        supportsVBO = capabilities.GL_ARB_vertex_buffer_object;
        camera = new LWJGLCamera(width, height, this);
    }

    @Override
    public boolean lockRenderer( RenderContext ctx ) {
        boolean locked = false;
        synchronized (this ) {
            if( this.ctx == null ) {
                this.ctx = ctx;
                locked = true;
                try {
                    Display.makeCurrent();
                } catch(Exception e) {
                    
                }
            } else {
                try {
                    this.wait();
                } catch(Exception e) {
                    
                }
            }
        }
        return locked;
    }
    
    @Override
    public void unlockRenderer( ) {
        synchronized ( this ) {
            ctx = null;
            try {
                Display.releaseContext();
            } catch(Exception e) {
                
            }
            //TODO: notify threads waiting for this renderer
            notify();
        }
    }
    
    /**
     * <code>setBackgroundColor</code> sets the OpenGL clear color to the
     * color specified.
     * 
     * @see com.jme.renderer.Renderer#setBackgroundColor(com.jme.renderer.ColorRGBA)
     * @param c
     *            the color to set the background color to.
     */
    @Override
    public void setBackgroundColor(ColorRGBA c) {
        // if color is null set background to white.
        super.setBackgroundColor(c);
        GL11.glClearColor(backgroundColor.r, backgroundColor.g,
                backgroundColor.b, backgroundColor.a);
    }

    @Override
    public void setCamera(ViewCamera cam) {
        if (camera == null) {
            camera = new LWJGLCamera();
        }
        cam.copy(camera);
        camera.update();
        ((LWJGLCamera) camera).apply();
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        if (camera != null) {
            camera.resize(width, height);
            ((LWJGLCamera) camera).apply();
        }
        capabilities = GLContext.getCapabilities();
    }
    
    /**
     * <code>takeScreenShot</code> saves the current buffer to a file. The
     * file name is provided, and .png will be appended. True is returned if the
     * capture was successful, false otherwise.
     * 
     * @param filename
     *            the name of the file to save.
     * @return true if successful, false otherwise.
     */
    @Override
    public boolean takeScreenShot(String filename) {
        if (null == filename) {
            throw new VleException("Screenshot filename cannot be null");
        }
        logger.info("Taking screenshot: " + filename + ".png");

        // Create a pointer to the image info and create a buffered image to
        // hold it.
        IntBuffer buff = ByteBuffer.allocateDirect(width * height * 4).order(
                ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        grabScreenContents(buff, 0, 0, width, height);
        BufferedImage img = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        // Grab each pixel information and set it to the BufferedImage info.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, buff.get((height - y - 1) * width + x));
            }
        }

        // write out the screenshot image to a file.
        try {
            File out = new File(filename + ".png");
            return ImageIO.write(img, "png", out);
        } catch (IOException e) {
            logger.warning("Could not create file: " + filename + ".png");
            return false;
        }
    }

    /**
     * <code>grabScreenContents</code> reads a block of pixels from the
     * current framebuffer.
     * 
     * @param buff
     *            a buffer to store contents in.
     * @param x -
     *            x starting point of block
     * @param y -
     *            y starting point of block
     * @param w -
     *            width of block
     * @param h -
     *            height of block
     */
    public void grabScreenContents(IntBuffer buff, int x, int y, int w, int h) {
        GL11.glReadPixels(x, y, w, h, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE,
                buff);
    }

    @Override
    public void cleanup() {
        cleanupVBOs();
        if (font != null) {
            font.deleteFont();
            font = null;
        }
    }

    /**
     * <code>displayBackBuffer</code> renders any queued items then flips the
     * rendered buffer (back) with the currently displayed buffer.
     * 
     * @see com.jme.renderer.Renderer#displayBackBuffer()
     */
    @Override
    public void displayBackBuffer() {

        ctx.defaultStateList[RenderState.RS_COLORMASK_STATE].apply(ctx);

        reset();

        GL11.glFlush();
        if (!isHeadless()) {
            Display.update();
        }

    //vboMap.expunge();
    }

    @Override
    public void reset() {
        prevColor = prevNorms = prevVerts = null;
        Arrays.fill(prevTex, null);
        prevGeom=null;
    }

    @Override
    public void clearBuffers() {
        // make sure no funny business is going on in the z before clearing.
        if (ctx.defaultStateList[RenderState.RS_ZBUFFER] != null) {
            ctx.defaultStateList[RenderState.RS_ZBUFFER].apply(ctx);
        }
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 
     * <code>setOrtho</code> sets the display system to be in orthographic
     * mode. If the system has already been set to orthographic mode a
     * <code>JmeException</code> is thrown. The origin (0,0) is the bottom
     * left of the screen.
     *  
     */
    @Override
    public void setOrtho() {
        if (inOrthoMode) {
            throw new VleException("Already in Orthographic mode.");
        }
        // set up ortho mode
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluOrtho2D(0, width, 0, height);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        inOrthoMode = true;
    }

    @Override
    public void setOrthoCenter() {
        if (inOrthoMode) {
            throw new VleException("Already in Orthographic mode.");
        }
        // set up ortho mode
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluOrtho2D(-width / 2, width / 2, -height / 2, height / 2);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        inOrthoMode = true;
    }

    /**
     * 
     * <code>setOrthoCenter</code> sets the display system to be in
     * orthographic mode. If the system has already been set to orthographic
     * mode a <code>JmeException</code> is thrown. The origin (0,0) is the
     * center of the screen.
     * 
     */
    @Override
    public void unsetOrtho() {
        if (!inOrthoMode) {
            throw new VleException("Not in Orthographic mode.");
        }
        // remove ortho mode, and go back to original
        // state
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        inOrthoMode = false;
    }

    @Override
    public RenderState createState(int type) {
        switch (type) {
            case RenderState.RS_ALPHABLEND:
                return new LWJGLAlphaBlendState();
            case RenderState.RS_ALPHATEST:
                return new LWJGLAlphaTestState();
            case RenderState.RS_ATTRIBUTE:
                break; //TODO
            case RenderState.RS_CLIP:
                return new LWJGLClipState();
            case RenderState.RS_COLORMASK_STATE:
                return new LWJGLColorMaskState();
            case RenderState.RS_CULL:
                return new LWJGLCullState();
            case RenderState.RS_DITHER:
                break; //TODO
            case RenderState.RS_FOG:
                return new LWJGLFogState();
            case RenderState.RS_FRAGMENT_PROGRAM:
                break; //TODO
            case RenderState.RS_GLSL_SHADER_OBJECTS:
                return new LWJGLShaderObjectsState();
            case RenderState.RS_GLSL_SHADER_PARAM:
                return new LWJGLShaderParameters();
            case RenderState.RS_LIGHT:
                return new LWJGLLightState();
            case RenderState.RS_MATERIAL:
                return new LWJGLMaterialState();
            case RenderState.RS_SHADE:
                break; //TODO
            case RenderState.RS_STENCIL:
                return new LWJGLStencilState();
            case RenderState.RS_TEXTURE:
                return new LWJGLTextureState();
            case RenderState.RS_VERTEX_PROGRAM:
                break; //TODO
            case RenderState.RS_WIREFRAME:
                return new LWJGLWireframeState();
            case RenderState.RS_ZBUFFER:
                return new LWJGLZBufferState();
            case RenderState.RS_LINE:
                return new LWJGLLineState();
            default:
                break;
        }
        return null;
    }

    @Override
    public void setCurrentColor(ColorRGBA setTo) {
        GL11.glColor4f(setTo.r, setTo.g, setTo.b, setTo.a);
    }

    @Override
    public void setCurrentColor(float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
    }

    public void applyDefaultStates() {
        RenderState[] states = ctx.defaultStateList;

        /*
        // first apply the shader parameters
        GLSLShaderParameters shaderParams = (GLSLShaderParameters)(ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_PARAM] != null ? ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_PARAM] : states[RenderState.RS_GLSL_SHADER_PARAM]);
        GLSLShaderObjectsState shaderState = (GLSLShaderObjectsState)(ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_OBJECTS] != null ? ctx.enforcedStateList[RenderState.RS_GLSL_SHADER_OBJECTS] : states[RenderState.RS_GLSL_SHADER_OBJECTS]);
        if (shaderState != null && shaderState != ctx.defaultStateList[RenderState.RS_GLSL_SHADER_OBJECTS]
        && shaderParams != null && shaderParams != ctx.defaultStateList[RenderState.RS_GLSL_SHADER_PARAM] ) {
        // there is both a shader and shader parameters, apply the parameters
        shaderState.apply(ctx);
        shaderParams.apply(ctx);
        }
         */

        RenderState tempState = null;
        for (int i = 0; i < states.length; i++) {
            tempState = ctx.enforcedStateList[i] != null ? ctx.enforcedStateList[i]
                    : states[i];

            if (tempState != null) {
                if (tempState != ctx.currentStates[i]) {
                    tempState.apply(ctx);
                }
            }
        }
    }

    @Override
    public void draw(TriBatch batch) {
        BaseGeometry g = batch.getModel();

        if (!generatingDisplayList) {
            if (!g.preDraw(ctx, batch)) {
                return;
            }
            if(ctx.positionmode) {
                if (g.getDisplayListMode() > 0 && g.getPosOnlyDisplayListID() == -1) {
                    // need to generate display list
                    g.setPosOnlyDisplayListID(createDisplayList(batch));
                }
            } else {
                if (g.getDisplayListMode() > 0 && g.getDisplayListID() == -1) {
                    // need to generate display list
                    g.setDisplayListID(createDisplayList(batch));
                }
            }
            if (statisticsOn) {
                stats.numberOfTris += g.getTriangleCount();
                stats.numberOfVerts += g.getNumVertex();
                stats.numberOfMesh++;
            }
            if(ctx.positionmode) {
                if (g.getDisplayListMode() > 0 && g.getPosOnlyDisplayListID() != -1) {
                    renderDisplayList(batch, g);
                    g.postDraw(ctx, batch);
                    return;
                }
            } else {
                if (g.getDisplayListMode() > 0 && g.getDisplayListID() != -1) {
                    renderDisplayList(batch, g);
                    g.postDraw(ctx, batch);
                    return;
                }
            }
        }

        //if (!generatingDisplayList) applyStates(batch.states, batch);
        doTransforms(batch, g);
        int mode = g.getMode();
        int glMode;

        switch (mode) {
            case BaseGeometry.TRIANGLES:
                glMode = GL11.GL_TRIANGLES;
                break;
            case BaseGeometry.TRIANGLE_STRIP:
                glMode = GL11.GL_TRIANGLE_STRIP;
                break;
            case BaseGeometry.TRIANGLE_FAN:
                glMode = GL11.GL_TRIANGLE_FAN;
                break;
            case BaseGeometry.LINE_SEGMENTS:
                glMode = GL11.GL_LINES;
                break;
            case BaseGeometry.LINE_CONNECTED:
                glMode = GL11.GL_LINE_STRIP;
                break;
            case BaseGeometry.LINE_LOOP:
                glMode = GL11.GL_LINE_LOOP;
                break;
            default:
                throw new VleException("Unknown triangle mode " + mode);
        }

        boolean indexvbo = false;
        IndexBuffer idb;
        idb = ((Geometry) g).getIndexBuffer();
        if (prevGeom == g && ctx.positionmode == prevPositionOnlyMode) {
            indexvbo = prevIndexVbo;
        } else {
            indexvbo = predrawGeometry(batch, (Geometry) g);
        }

        prevGeom = g;
        prevIndexVbo = indexvbo;
        prevPositionOnlyMode = ctx.positionmode;

        if (!indexvbo) {
            // make sure only the necessary indices are sent through on old cards.
            if (idb == null || idb.getBuffer() == null) {
                logger.severe("missing indices on geometry object: " + batch.toString());
            } else {

                idb.limit(g.getStartIndex() + g.getNumIndex());
                idb.position(g.getStartIndex());

                if (capabilities.GL_EXT_compiled_vertex_array) {
                    EXTCompiledVertexArray.glLockArraysEXT(0, g.getNumVertex());
                }

                Buffer b = idb.getBuffer();
                if (b instanceof IntBuffer) {
                    GL11.glDrawElements(glMode, (IntBuffer) b);
                } else {
                    GL11.glDrawElements(glMode, (ShortBuffer) b);
                }
                if (capabilities.GL_EXT_compiled_vertex_array) {
                    EXTCompiledVertexArray.glUnlockArraysEXT();
                }

                idb.clear();
            }
        } else {
            int indexVboPos = idb.getVBOInfo().vboPointer;
            if (capabilities.GL_EXT_compiled_vertex_array) {
                EXTCompiledVertexArray.glLockArraysEXT(0, g.getNumVertex());
                // TODO: was 0
            }
            if (idb instanceof IndexBufferInt) {
                GL11.glDrawElements(glMode, g.getNumIndex(), GL11.GL_UNSIGNED_INT, indexVboPos + g.getStartIndex()*4);
            } else {
                GL11.glDrawElements(glMode, g.getNumIndex(), GL11.GL_UNSIGNED_SHORT, indexVboPos + g.getStartIndex()*2);
            }
            if (capabilities.GL_EXT_compiled_vertex_array) {
                EXTCompiledVertexArray.glUnlockArraysEXT();
            }
        }

        //postdrawGeometry(batch);

        undoTransforms(batch, g);
        if (!generatingDisplayList) {
            g.postDraw(ctx, batch);
        }
    }

    private void renderDisplayList(Renderable r, BaseGeometry g) {
        //applyStates(batch.states, batch);
        if (g.getDisplayListMode() != BaseGeometry.LIST_LOCKED) {
            // list not locked in one place
            doTransforms(r, g);
            GL11.glCallList(g.getDisplayListID());
            undoTransforms(r, g);
        } else {
            GL11.glCallList(g.getDisplayListID());
        }
        // invalidate line record as we do not know the line state anymore
        //((LineRecord) DisplaySystem.getDisplaySystem().getCurrentContext().getLineRecord()).invalidate();
        // invalidate "current arrays"
        reset();
    }
    
    // with composite geometry, we only have a number of vertex arays and a single index buffer
    public void prepVBO(Geometry g) {
        if (!supportsVBO() || g.getVBOMode() == BaseGeometry.VBO_NO) {
            return;
        }
        
        FastList<VertexBuffer> arrays = g.getBuffers();
        for(int j=0, mj=arrays.size(); j<mj; j++) {
            VertexBuffer vtx = arrays.get(j);
            prepVBO(vtx);
        }

        IndexBuffer idx = g.getIndexBuffer();
        prepVBO(idx);
    }

    public void prepVBO(VertexBuffer vtx) {
        if (vtx != null) {
            VBOAttributeInfo vboAtt = vtx.getVBOInfo();
            if (vboAtt != null && vboAtt.vboID == -1) {
                Object vboid;
                FloatBuffer vertbuff = vtx.getDataBuffer();
                if ((vboid = vboMap.get(vertbuff)) != null) {
                    vboAtt.vboID = ((Integer) vboid).intValue();
                } else {
                    vertbuff.rewind();
                    vboAtt.vboID = makeVBOId();
                    vboMap.put(vertbuff, vboAtt.vboID);
                    // ensure no VBO is bound
                    setBoundVBO(vboAtt.vboID);
                    ARBBufferObject.glBufferDataARB(
                            ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertbuff,
                            ARBBufferObject.GL_STATIC_DRAW_ARB);
                }
            }
        }
    }
    
    public void prepVBO(IndexBuffer ib) {
        if (ib != null) {
            VBOAttributeInfo vboAtt = ib.getVBOInfo();
            // TODO: add index VBO support
            if (vboAtt != null && vboAtt.vboID == -1) {
                //TriangleBatch tb = (TriangleBatch) g;
                if (ib != null) {
                    Object vboid;
                    Buffer indices = ib.getBuffer();
                    if ((vboid = vboMap.get(indices)) != null) {
                        vboAtt.vboID = ((Integer) vboid).intValue();
                    } else {
                        indices.rewind();
                        vboAtt.vboID = makeVBOId();
                        vboMap.put(indices, vboAtt.vboID);
                        setBoundElementVBO(vboAtt.vboID);
                        if (indices instanceof IntBuffer) {
                            ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, (IntBuffer) indices, ARBBufferObject.GL_STATIC_DRAW_ARB);
                        } else {
                            ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, (ShortBuffer) indices, ARBBufferObject.GL_STATIC_DRAW_ARB);
                        }
                    }
                }
            }
        }
    }
    
    public void setBoundVBO(int id) {
        ARBBufferObject.glBindBufferARB(
                ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, id);
    }

    public int makeVBOId() {
        idBuff.rewind();
        ARBBufferObject.glGenBuffersARB(idBuff);
        int vboID = idBuff.get(0);
        vboCleanupCache.add(vboID);
        return vboID;
    }

    public void deleteVBOId(int id) {
        idBuff.rewind();
        idBuff.put(id).flip();
        ARBBufferObject.glDeleteBuffersARB(idBuff);
        vboCleanupCache.removeElement(id);
    }

    public void cleanupVBOs() {
        for (int x = vboCleanupCache.size(); --x >= 0;) {
            int id = vboCleanupCache.get(x);
            idBuff.rewind();
            idBuff.put(id).flip();
            ARBBufferObject.glDeleteBuffersARB(idBuff);
        }
        vboCleanupCache.clear();
    }

    public void setBoundElementVBO(int id) {
        ARBBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, id);
    }

    @Override
    public void draw(TextBatch tx) {
        if (font == null) {
            font = new LWJGLFont();
        }
        font.setColor(tx.getTextColor(ctx.frameId));

        Vector3f trl = tx.getWorldTransForm(ctx.frameId).getTranslation();
        font.print(ctx, (int) trl.x, (int) trl.y, tx.getWorldTransForm(ctx.frameId).getScale(), tx.getText(ctx.frameId), 0);
    }

    /**
     * Return true if the system running this supports VBO
     * 
     * @return boolean true if VBO supported
     */
    public boolean supportsVBO() {
        return supportsVBO;
    }



    protected boolean predrawGeometry(TriBatch tb, Geometry t) {
        if (supportsVBO() && t.getVBOMode() != BaseGeometry.VBO_NO) {
            prepVBO(t);
        }

        indicesVBO = false;

        int vboid = -1;

        // VBO id for indices
        vboid = !supportsVBO 
                || t.getVBOMode() == BaseGeometry.VBO_NO
                || t.getIndexBuffer() == null 
                || t.getIndexBuffer().getVBOInfo() == null 
              ? -1 : t.getIndexBuffer().getVBOInfo().vboID;

        if (vboid != -1) { // use VBO for indices
            indicesVBO = true;
            setBoundElementVBO(vboid);
        } else if (supportsVBO) {
            setBoundElementVBO(0);
        }

        long vboPointer = 0;
        int arrayPointer = 0;
        int arrayLength = 0;
        int maxtexused = -1;
        // clear the enabled attribs list
        Arrays.fill(vertexattribs, false);
        shaderattribs.clear();
            
        // the vertex buffers array
        FastList<VertexBuffer> arrays = t.getBuffers();
        // the list of vertice starts
        IntList startVertex = t.getBufferStartVertex();
        for(int j=0, mj=arrays.size(); j<mj; j++) {
            VertexBuffer vtx = arrays.get(j);
            // get the floatbuffer
            FloatBuffer vbf = vtx.getDataBuffer();
            // get the vertexformat
            VertexFormat vf = vtx.getFormat();
            // get first 
            vboid = !supportsVBO 
                || t.getVBOMode() == BaseGeometry.VBO_NO
                || vtx.getVBOInfo() == null 
              ? -1 : vtx.getVBOInfo().vboID;
            
            if (vboid == -1) {
                // the start of vertices in the buffer (in floats)
                arrayPointer = vf.getSize() * startVertex.get(j); //t.getStartVertex();
                arrayLength = vf.getSize() * t.getNumVertex();

                // prepare vertex buffer
                vbf.limit(arrayPointer + arrayLength);
                // dont set position, it may break
                //vbf.position(arrayPointer);
                
            } else {
                VBOAttributeInfo vboinfo = vtx.getVBOInfo();
                // the VBO pointer (counted in bytes)
                vboPointer = vboinfo.vboPointer + ((long)vf.getBytes() * startVertex.get(j)); //t.getStartVertex();
            }

            if (vboid != -1) {
                setBoundVBO(vboid);
            } else {
                if (supportsVBO) {
                    setBoundVBO(0);
                }
            }

            // get the vertex attributes
            FastList<VertexAttribute> vtxas = vf.getAttributes();

            // store for vertex geometry, to be applyed at the end
            VertexAttribute vtxg = null;
            for (int i = 0,  mx = vtxas.size(); i < mx; i++) {
                VertexAttribute vtxa = vtxas.get(i);
                // if only position is to be sent, skip any other attribute type
                // we still need weights for bone animations
                if(ctx.positionmode && vtxa.type!=VertexAttribute.USAGE_POSITION
                        && vtxa.type!=VertexAttribute.USAGE_WEIGHTINDICES
                        && vtxa.type!=VertexAttribute.USAGE_WEIGHTS
                        && vtxa.type!=VertexAttribute.USAGE_NUMWEIGHTS)
                    continue;
                switch (vtxa.type) {
                    case Position:
    // geometry to be applyed at the end
                        vtxg = vtxa;
                        break;
                    case Normal:
                         {
    // vertex normals
                            int normMode = t.getNormalsMode();
                            prevNorms = null;
                            if (normMode != BaseGeometry.NM_OFF) {
                                applyNormalMode(normMode, tb, t);
                                // enable normals
                                ctx.setEnableVertexAttribute(vtxa.type, true);
                                vertexattribs[vtxa.type.id] = true;
                                if (vboid > 0) { // use VBO
                                    // stride is the length of the vertex structure
                                    // pointer is the start
                                    GL11.glNormalPointer(GL11.GL_FLOAT, vf.getBytes(), vboPointer + vtxa.startbyte);
                                } else {
                                    // 
                                    vbf.position(arrayPointer + (vtxa.startbyte / 4));
                                    // TODO, check this
                                    GL11.glNormalPointer(vf.getBytes(), vbf);
                                }
                            } else {
                                if (prevNormMode == GL12.GL_RESCALE_NORMAL) {
                                    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                                    prevNormMode = GL11.GL_ZERO;
                                } else if (prevNormMode == GL11.GL_NORMALIZE) {
                                    GL11.glDisable(GL11.GL_NORMALIZE);
                                    prevNormMode = GL11.GL_ZERO;
                                }
                            }
                        }
                        break;
                    case Color:
                         {
                            prevColor = null;
                            // enable color attributes
                            ctx.setEnableVertexAttribute(vtxa.type, true);
                            vertexattribs[vtxa.type.id] = true;
                            if (vboid > 0) { // use VBO
                                GL11.glColorPointer(vtxa.floats, GL11.GL_FLOAT, vf.getBytes(), vboPointer + vtxa.startbyte);
                            } else {
                                vbf.position(arrayPointer + vtxa.startfloat);
                                GL11.glColorPointer(vtxa.floats, vf.getBytes(), vbf);
                            }
                        }
                        break;

                    default:
                        if (vtxa.type.texcoordNo >= 0) {
                            TextureState ts = (TextureState) ctx.currentStates[RenderState.RS_TEXTURE];
                            if (ts != null && ts.isEnabled()) {
                                int offset = ts.getTextureCoordinateOffset();
                                int texcoord = vtxa.type.texcoordNo - offset;
                                VertexAttribute.Usage texunit = VertexAttribute.Usage.getById(vtxa.type.id - offset);
                                maxtexused = Math.max(texcoord, maxtexused);
                                if (texcoord >= 0) {
                                    prevTex[texcoord] = null;
                                    if (capabilities.GL_ARB_multitexture) {
                                        ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB + texcoord);
                                    }
                                    ctx.setEnableVertexAttribute(texunit, true);
                                    vertexattribs[texunit.id] = true;
                                    if (vboid > 0) { // use VBO
                                        GL11.glTexCoordPointer(vtxa.floats, GL11.GL_FLOAT, vf.getBytes(), vboPointer + vtxa.startbyte);
                                    } else {
                                        vbf.position(arrayPointer + (vtxa.startbyte / 4));
                                        GL11.glTexCoordPointer(vtxa.floats, vf.getBytes(), vbf);
                                    }
                                }
                            }
                        } else if (vtxa.type.isShaderAttrib && ctx.currentProgramid != 0) {
                            // shader vertex attribute
                            ShaderParameters sp = (ShaderParameters) ctx.currentStates[RenderState.RS_GLSL_SHADER_PARAM];
                            ShaderObjectsState so = (ShaderObjectsState) ctx.currentStates[RenderState.RS_GLSL_SHADER_OBJECTS];
                            if (sp != null && sp.isEnabled() && so != null && so.isEnabled()) {
                                // get the shader attribute by usage type
                                ShaderVertexAttribute sva = sp.getVertexAttribute(vtxa.type);
                                if(sva==null)
                                    continue;
                                ShaderVariableLocation svl = so.getVariableLocation(sva, ctx);
                                if(svl.variableID == -1)
                                    svl.update(so.getProgramId());
                                if (svl != null && svl.variableID >= 0) {
                                    // enable shader vertex attribute
                                    // TODO: based on class type, implement not only float attributes
                                    // easy to do with VBO enabled, but how to do it without VBO?
                                    switch(vtxa.type) {
                                        case WeightIndex : {
                                            if (vboid > 0) { // use VBO
                                                ARBVertexProgram.glVertexAttribPointerARB(svl.variableID, 4,
                                                        GL11.GL_UNSIGNED_BYTE, false,
                                                        vf.getBytes(), vboPointer + vtxa.startbyte);
                                            } else {
                                                throw new VleException("Only VBO mode supported with USAGE_WEIGHTINDICES attributes");
                                                //vbf.position(arrayPointer + vtxa.startfloat);
                                                //ARBVertexProgram.glVertexAttribPointerARB(sva.variableID, vtxa.floats,
                                                //        sva.normalized,
                                                //        vf.getBytes(), vbf);
                                            }
                                        } break;
                                        case WeightNumber : {
                                            if (vboid > 0) { // use VBO
                                                ARBVertexProgram.glVertexAttribPointerARB(svl.variableID, 1,
                                                        GL11.GL_UNSIGNED_BYTE, false,
                                                        vf.getBytes(), vboPointer + vtxa.startbyte);
                                            } else {
                                                throw new VleException("Only VBO mode supported with USAGE_NUMWEIGHTS attributes");
                                                //vbf.position(arrayPointer + vtxa.startfloat);
                                                //ARBVertexProgram.glVertexAttribPointerARB(sva.variableID, vtxa.floats,
                                                //        sva.normalized,
                                                //        vf.getBytes(), vbf);
                                            }
                                        } break;
                                        default : {
                                            if (vboid > 0) { // use VBO
                                                ARBVertexProgram.glVertexAttribPointerARB(svl.variableID, vtxa.floats,
                                                        GL11.GL_FLOAT, sva.normalized,
                                                        vf.getBytes(), vboPointer + vtxa.startbyte);
                                            } else {
                                                vbf.position(arrayPointer + vtxa.startfloat);
                                                ARBVertexProgram.glVertexAttribPointerARB(svl.variableID, vtxa.floats,
                                                        sva.normalized,
                                                        vf.getBytes(), vbf);
                                            }
                                        } break;
                                    }
                                    ctx.setEnableShaderAttribute(svl.variableID, true);
                                    shaderattribs.set(svl.variableID, 1);
                                }
                            }
                        }
                        break;
                }
            }
            if (vtxg != null) {
                // we have vertex position
                // define this the last
                // nVidia performs better, and ATI doesnt care
                ctx.setEnableVertexAttribute(vtxg.type, true);
                vertexattribs[vtxg.type.id] = true;
                prevVerts = null;
                if (vboid != -1) { // use VBO
                    GL11.glVertexPointer(vtxg.floats, GL11.GL_FLOAT, vf.getBytes(), vboPointer + vtxg.startbyte);
                } else {
                    // buffer positioning is in floats (4 bytes)
                    vbf.position(arrayPointer + vtxg.startfloat);
                    // stride is in bytes
                    GL11.glVertexPointer(vtxg.floats, vf.getBytes(), vbf);
                }
            }
        }
        
        // disable unused vertex attribs
        for (int i = 0,  mx = VertexAttribute.USAGE_TEXTURE0.id; i < mx; i++) {
            if (!vertexattribs[i]) {
                ctx.setEnableVertexAttribute(VertexAttribute.Usage.getById(i), false);
            }
        }

        // disable unused texcoords
        if (capabilities.GL_ARB_multitexture) {
            int mx = Math.min(prevTextureNumber,
                    TextureState.getNumberOfFragmentTexCoordUnits() - 1) + VertexAttribute.USAGE_TEXTURE0.id;
            for (int i = VertexAttribute.USAGE_TEXTURE0.id; i <= mx; i++) {
                if (vertexattribs[i] != ctx.vertexAttribState[i]) {
                    ARBMultitexture.glClientActiveTextureARB(
                            ARBMultitexture.GL_TEXTURE0_ARB + (i - VertexAttribute.USAGE_TEXTURE0.id));
                    ctx.setEnableVertexAttribute(VertexAttribute.Usage.getById(i), vertexattribs[i]);
                }
            }
        } else {
            if (!vertexattribs[VertexAttribute.USAGE_TEXTURE0.id]) {
                ctx.setEnableVertexAttribute(VertexAttribute.USAGE_TEXTURE0, false);
            }
        }
        prevTextureNumber = maxtexused;

        // set default color, if no collor array is set
        if (!vertexattribs[VertexAttribute.USAGE_COLOR.id]) {
            // enforce a current color here.
            ColorRGBA defCol = ctx.currentMaterial.getDefaultColor();
            if (defCol != null) {
                setCurrentColor(defCol);
            } else {
                // no default color, so set to white.
                setCurrentColor(1, 1, 1, 1);
            }
        }

        // disable unused shader attributes
        ctx.disableUnusedAttribs(shaderattribs);
        

        return indicesVBO;
    }

    protected void applyNormalMode(int normMode, TriBatch tb, BaseGeometry t) {
        switch (normMode) {
            case BaseGeometry.NM_GL_NORMALIZE_IF_SCALED:
                Vector3f scale = tb.getWorldTransForm(ctx.frameId).getScale();
                if (!scale.equals(Vector3f.UNIT_XYZ)) {
                    if (scale.x == scale.y && scale.y == scale.z && capabilities.OpenGL12 && prevNormMode != GL12.GL_RESCALE_NORMAL) {
                        if (prevNormMode == GL11.GL_NORMALIZE) {
                            GL11.glDisable(GL11.GL_NORMALIZE);
                        }
                        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                        prevNormMode = GL12.GL_RESCALE_NORMAL;
                    } else if (prevNormMode != GL11.GL_NORMALIZE) {
                        if (prevNormMode == GL12.GL_RESCALE_NORMAL) {
                            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                        }
                        GL11.glEnable(GL11.GL_NORMALIZE);
                        prevNormMode = GL11.GL_NORMALIZE;
                    }
                } else {
                    if (prevNormMode == GL12.GL_RESCALE_NORMAL) {
                        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                        prevNormMode = GL11.GL_ZERO;
                    } else if (prevNormMode == GL11.GL_NORMALIZE) {
                        GL11.glDisable(GL11.GL_NORMALIZE);
                        prevNormMode = GL11.GL_ZERO;
                    }
                }
                break;
            case BaseGeometry.NM_GL_NORMALIZE_PROVIDED:
                if (prevNormMode != GL11.GL_NORMALIZE) {
                    if (prevNormMode == GL12.GL_RESCALE_NORMAL) {
                        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                    }
                    GL11.glEnable(GL11.GL_NORMALIZE);
                    prevNormMode = GL11.GL_NORMALIZE;
                }
                break;
            case BaseGeometry.NM_USE_PROVIDED:
            default:
                if (prevNormMode == GL12.GL_RESCALE_NORMAL) {
                    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                    prevNormMode = GL11.GL_ZERO;
                } else if (prevNormMode == GL11.GL_NORMALIZE) {
                    GL11.glDisable(GL11.GL_NORMALIZE);
                    prevNormMode = GL11.GL_ZERO;
                }
                break;
        }
    }

    protected void doTransforms(Renderable r, BaseGeometry g) {
        // set world matrix
        if (!generatingDisplayList || g.getDisplayListMode() == BaseGeometry.LIST_LOCKED) {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();

            Transform t = r.getWorldTransForm(ctx.frameId);
            Vector3f translation = t.getTranslation();
            if (!translation.equals(Vector3f.ZERO)) {
                GL11.glTranslatef(translation.x, translation.y, translation.z);
            }

            Quaternion rotation = t.getRotation();
            if (!rotation.isIdentity()) {
                float rot = rotation.toAngleAxis(vRot) * FastMath.RAD_TO_DEG;
                GL11.glRotatef(rot, vRot.x, vRot.y, vRot.z);
            }

            Vector3f scale = t.getScale();
            if (!scale.equals(Vector3f.UNIT_XYZ)) {
                GL11.glScalef(scale.x, scale.y, scale.z);
            }
        }
    }

    protected void undoTransforms(Renderable r, BaseGeometry g) {
        if (!generatingDisplayList || g.getDisplayListMode() == BaseGeometry.LIST_LOCKED) {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        }
    }

    // inherited documentation
    public int createDisplayList(TriBatch t) {
        int listID = GL11.glGenLists(1);

        generatingDisplayList = true;
        //RenderState oldTS = ctx.currentStates[RenderState.RS_TEXTURE];
        //ctx.currentStates[RenderState.RS_TEXTURE] = g.states[RenderState.RS_TEXTURE];
        GL11.glNewList(listID, GL11.GL_COMPILE);
        draw(t);
        /*
        if (g instanceof TriangleBatch)
        draw((TriangleBatch)g);
        else if (g instanceof QuadBatch)
        draw((QuadBatch)g);
        else if (g instanceof LineBatch)
        draw((LineBatch)g);
        else if (g instanceof PointBatch)
        draw((PointBatch)g);
         */
        GL11.glEndList();
        //context.currentStates[RenderState.RS_TEXTURE] = oldTS;
        generatingDisplayList = false;

        return listID;
    }

    @Override
    public VertexBuffer allocVertexBuffer(VertexFormat format, int vertices, VertexBuffer orig) {
        VertexBuffer vb = null;
        if(orig!=null) {
            if(orig.getFormat() != format
                 || orig.getVertexCount() != vertices
                 || orig.getVBOInfo() == null
                 || orig.getVBOInfo().mappedBuffer == null) {
                // this is not the same, 
                // or is not an mapped buffer already
                //relase the buffer
                releaseBuffer(orig);
            } else {
                return orig;
            }
        }
        // we dont yet have a buffer created, create it
        vb = new VertexBuffer();
        vb.setFormat(format);
        VBOAttributeInfo vbo = new VBOAttributeInfo();
        vb.setVBOInfo(vbo);
        // allocate a new vbo
        vbo.vboID = this.makeVBOId();
        setBoundVBO(vbo.vboID);
        // allocate the buffer with proper size
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertices*format.getBytes(), ARBBufferObject.GL_STATIC_DRAW_ARB);
        vb.setVertexCount(vertices);
        return vb;
    }
    
    @Override
    public IndexBuffer allocIndexBuffer(int indices, int vertices, IndexBuffer orig) {
        IndexBuffer idx = null;
        
        // do we have a short or int buffer
        boolean isShort = IndexBuffer.isShortBufferPossible(vertices);
        // the size of the buffer in bytes
        int byteSize = isShort? 2*indices: 4*indices;
        
        if(orig!=null) {
            if( ( !isShort && orig instanceof IndexBufferShort) 
                    || (isShort && orig instanceof IndexBufferInt)
                    || orig.limit() != indices
                    || orig.getVBOInfo() == null
                    || orig.getVBOInfo().mappedBuffer == null) {
                // this is not the same, 
                // or is not an mapped buffer already
                //relase the buffer
                releaseBuffer(orig);
            } else {
                return orig;
            }
        }
        // we dont yet have a buffer created, create it
        idx = IndexBuffer.createEmptyBuffer(indices, vertices, orig);

        VBOAttributeInfo vbo = idx.getVBOInfo();
        if(vbo==null) {
            vbo = new VBOAttributeInfo();
            idx.setVBOInfo(vbo);
        }
        if(vbo.vboID==-1) {
            // allocate a new vbo
            vbo.vboID = this.makeVBOId();
        }
        setBoundElementVBO(vbo.vboID);
        // allocate the buffer with proper size
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, byteSize*indices, ARBBufferObject.GL_STATIC_DRAW_ARB);

        idx.setVBOInfo(vbo);
        return idx;
    }
    
    @Override
    public VertexBuffer mapVertexBuffer(VertexBuffer vb) {
        VBOAttributeInfo vbo = vb.getVBOInfo();
        if(vbo==null || vbo.vboID==-1)
            throw new VleException("Tryed to map a non-mapped vertex buffer");
        setBoundVBO(vbo.vboID);
        ByteBuffer bb = ARBBufferObject.glMapBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, ARBBufferObject.GL_WRITE_ONLY_ARB , vbo.mappedBuffer);
        if(bb==null) {
            // TODO: use non mapped buffer from now on
        }
        if(bb != vbo.mappedBuffer) {
            // the mapped buffer got relocated, we need to wrap it into
            // a new FloatBuffer
            // TODO: this actualy makes garbage here, dont know if its any good
            // but no other option is possible
            vbo.mappedBuffer = bb;
            vbo.mappedBuffer.order(ByteOrder.nativeOrder());
            vbo.wrappedBuffer = vbo.mappedBuffer.asFloatBuffer();
        }
        vb.setDataBuffer((FloatBuffer) vbo.wrappedBuffer);
        return vb;
    }
    
    @Override
    public IndexBuffer mapIndexBuffer(IndexBuffer idx) {
        VBOAttributeInfo vbo = idx.getVBOInfo();
        if(vbo == null || idx.getVBOInfo().vboID==-1)
            throw new VleException("Tryed to map a non-mapped buffer");
        
        ByteBuffer bb = ARBBufferObject.glMapBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, ARBBufferObject.GL_READ_WRITE_ARB , vbo.mappedBuffer);
        //GL_WRITE_ONLY_ARB
        if(bb != vbo.mappedBuffer) {
            // the mapped buffer got relocated, we need to wrap it into
            // a new FloatBuffer
            // TODO: this actualy makes garbage here, dont know if its any good
            // but no other option is possible
            vbo.mappedBuffer = bb;
            vbo.mappedBuffer.order(ByteOrder.nativeOrder());
            // TODO: change to wrappedbuffer
            idx.setData(vbo.mappedBuffer);
        }
        return idx;
    }

    @Override
    public void unMapVertexBuffer(VertexBuffer buf) {
        VBOAttributeInfo vbo = buf.getVBOInfo();
        if(vbo!=null
              && vbo.mappedBuffer!=null
              && vbo.vboID != -1) {
            // bind the vbo
            setBoundVBO(vbo.vboID);
            // unmap the buffer
            ARBBufferObject.glUnmapBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB);
            buf.removeDataBuffer();
        }
    }
    
    @Override
    public void unMapIndexBuffer(VertexBuffer buf) {
        VBOAttributeInfo vbo = buf.getVBOInfo();
        if(vbo!=null
              && vbo.mappedBuffer!=null
              && vbo.vboID != -1) {
            // bind the vbo
            setBoundVBO(vbo.vboID);
            // unmap the buffer
            ARBBufferObject.glUnmapBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB);
            // clear out the data buffer
            //buf.removeDataBuffer();
        }
    }
    
    @Override
    public void releaseBuffer(VertexBuffer buf) {
        // get vbo id
        if(buf.getVBOInfo()!=null) {
            int vboid = buf.getVBOInfo().vboID;
            if(vboid!=-1) {
                buf.getVBOInfo().mappedBuffer = null;
                
                // release vbo id
                deleteVBOId(vboid);
                // remove from vbo map
                if(buf.getDataBuffer()!=null) {
                    vboMap.remove(buf.getDataBuffer());
                }
                // to be safe
                // if its mapped, then null out the buffer, since its invalid
                //if(buf.getVBOInfo().mapped) {
                buf.setDataBuffer(null);
                //}
                buf.getVBOInfo().vboID = -1;
            }
        }
    }
    
    @Override
    public void releaseBuffer(IndexBuffer buf) {
        if(buf.getVBOInfo()!=null) {
            int vboid = buf.getVBOInfo().vboID;
            if(vboid!=-1) {
                if(buf.getVBOInfo().mappedBuffer!=null) {
                    // bind the vbo
                    setBoundElementVBO(vboid);
                    // unmap the buffer
                    ARBBufferObject.glUnmapBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB);
                    setBoundElementVBO(0);
                    buf.getVBOInfo().mappedBuffer = null;
                }
                // release vbo id
                deleteVBOId(vboid);
                // remove from vbo map
                if(buf.getBuffer()!=null) {
                    vboMap.remove(buf.getBuffer());
                }
                buf.removeBuffer();
                buf.getVBOInfo().vboID = -1;
            }
        }
    }
    
    @Override
    public void drawDirect(SceneElement node) {
        Context tmp = LocalContext.getContext();
        FastList<Object> list = tmp.directDrawList;
        CullContext stx = tmp.scene;

        int prevmaterial = ctx.currentPassShaderFlags;
        // render with unlit
        ctx.currentPassShaderFlags = 0;
        
        list.clear();
        list.add(node);
        int lss = list.size();
        while (lss > 0) {
            Object s = list.get(lss - 1);
            list.remove(lss - 1);

            if (s instanceof Node) {
                Node n = (Node) s;
                // extract effect from node
                /*
                FastList<SceneEffect> eff = n.getEffects();
                if (eff != null) {
                    for (int j = 0,  mj = eff.size(); j < mj; j++) {
                        SceneEffect e = eff.get(j);
                        // put it into the list so we can undo the effect later
                        list.add(e);
                        // do the effect
                        e.doEffect(stx);
                    }
                }
                 */
                // extract node children
                for (int i = 0,  mx = n.getQuantity(); i < mx; i++) {
                    list.add(n.getChild(i));
                }
            } else if (s instanceof Text) {
                // get batch from text
                list.add(((Text) s).getBatch());
            } else if (s instanceof LodMesh) {
                // get batches from mesh
                LodMesh m = (LodMesh) s;
                for (int i = 0,  mx = m.getBatchCount(0); i < mx; i++) {
                    list.add(m.getBatch(0, i));
                }
            } else if (s instanceof Renderable) {
                // apply material of renderable
                Renderable r = (Renderable) s;
                if (r.isNeedUpdate(ctx.frameId)) {
                    r.update(stx);
                    r.setNeedUpdate(ctx.frameId, false);
                }

                Material mat = r.getMaterial();
                mat = mat == null ? ctx.defaultMaterial : mat;
                if (mat != null && ctx.currentMaterial != mat) {
                    if (mat.isNeedUpdate()) {
                        mat.update(stx);
                        mat.prepare(ctx);
                    }
                    mat.apply(ctx);
                }

                r.draw(ctx);
            } /*else if (s instanceof SceneEffect) {
                // effects from list need to be undone
                SceneEffect e = (SceneEffect) s;
                e.undoEffect(stx);
            }*/

            // recalculate list size
            lss = list.size();
        }
        ctx.currentPassShaderFlags = prevmaterial;
    }
    
    
}
