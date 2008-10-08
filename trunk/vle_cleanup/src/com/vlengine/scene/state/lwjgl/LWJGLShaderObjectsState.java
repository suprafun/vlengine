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

package com.vlengine.scene.state.lwjgl;

import com.vlengine.renderer.RenderContext;
import com.vlengine.scene.state.ShaderObjectsState;
import com.vlengine.scene.state.shader.ShaderVariable;
import com.vlengine.scene.state.shader.ShaderVariableLocation;
import com.vlengine.system.VleException;
import com.vlengine.util.FastList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;


/**
 * Implementation of the GL_ARB_shader_objects extension.
 *
 * @author Thomas Hourdel
 * @author Joshua Slack (attributes and StateRecord)
 * @author Rikard Herlitz (MrCoder)
 */
public class LWJGLShaderObjectsState extends ShaderObjectsState {
    private static final Logger logger = Logger.getLogger(LWJGLShaderObjectsState.class.getName());

    /** OpenGL id for this program. * */
    private int programID = -1;

    /** OpenGL id for the attached vertex shader. */
    private int vertexShaderID = -1;

    /** OpenGL id for the attached fragment shader. */
    private int fragmentShaderID = -1;
    
    // the map of variable (uniform and attribute) locations
    // for this shader
    // not that locations are stored by index provided in the AppContext
    protected FastList<ShaderVariableLocation> shaderVariableLocations =
            new FastList<ShaderVariableLocation>();
    
    public LWJGLShaderObjectsState() {
        super();
    }
    
    public static void init() {
        if( ! inited ) {
            supported = GLContext.getCapabilities().GL_ARB_shader_objects &&
                    GLContext.getCapabilities().GL_ARB_fragment_shader &&
                    GLContext.getCapabilities().GL_ARB_vertex_shader &&
                    GLContext.getCapabilities().GL_ARB_shading_language_100;

            // get the number of supported shader attributes
            if( supported ) {
                IntBuffer buf = BufferUtils.createIntBuffer(16);
                GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS, buf);
                maxVertexAttribs = buf.get(0);

                if (logger.isLoggable(Level.FINE)) {
                    StringBuffer shaderInfo = new StringBuffer();
                    shaderInfo.append("GL_MAX_VERTEX_ATTRIBS: " + maxVertexAttribs + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS, buf);
                    shaderInfo.append("GL_MAX_VERTEX_UNIFORM_COMPONENTS: " + buf.get(0) + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, buf);
                    shaderInfo.append("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS: " + buf.get(0) + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_TEXTURE_COORDS, buf);
                    shaderInfo.append("GL_MAX_TEXTURE_COORDS: " + buf.get(0) + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, buf);
                    shaderInfo.append("GL_MAX_TEXTURE_IMAGE_UNITS: " + buf.get(0) + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, buf);
                    shaderInfo.append("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS: " + buf.get(0) + "\n");
                    GL11.glGetInteger(GL20.GL_MAX_VARYING_FLOATS, buf);
                    shaderInfo.append("GL_MAX_VARYING_FLOATS: " + buf.get(0) + "\n");
                    shaderInfo.append(GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));

                    logger.fine(shaderInfo.toString());
                }
            }
            inited = true;
        }
    }
    

    /**
     * Loads the shader object. Use null for an empty vertex or empty fragment
     * shader.
     *
     * @param vertexByteBuffer vertex shader
     * @param fragmentByteBuffer fragment shader
     * @see com.jme.scene.state.GLSLShaderObjectsState#load(java.net.URL,
     *java.net.URL)
     */
    private void load(ByteBuffer vertexByteBuffer,
            ByteBuffer fragmentByteBuffer) {

        if (vertexByteBuffer == null && fragmentByteBuffer == null) {
            logger.warning("Could not find shader resources!"
                    + "(both inputbuffers are null)");
            return;
        }

        if (programID == -1)
            programID = ARBShaderObjects.glCreateProgramObjectARB();

        if (vertexByteBuffer != null) {
            if (vertexShaderID != -1)
                removeVertShader();

            vertexShaderID = ARBShaderObjects.glCreateShaderObjectARB(
                    ARBVertexShader.GL_VERTEX_SHADER_ARB);

            // Create the sources
            ARBShaderObjects
                    .glShaderSourceARB(vertexShaderID, vertexByteBuffer);

            // Compile the vertex shader
            IntBuffer compiled = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glCompileShaderARB(vertexShaderID);
            ARBShaderObjects.glGetObjectParameterARB(vertexShaderID,
                    ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, vertexShaderID, vertSource);

            // Attach the program
            ARBShaderObjects.glAttachObjectARB(programID, vertexShaderID);
        } else if (vertexShaderID != -1) {
            removeVertShader();
            vertexShaderID = -1;
        }

        if (fragmentByteBuffer != null) {
            if (fragmentShaderID != -1)
                removeFragShader();

            fragmentShaderID = ARBShaderObjects.glCreateShaderObjectARB(
                    ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

            // Create the sources
            ARBShaderObjects
                    .glShaderSourceARB(fragmentShaderID, fragmentByteBuffer);

            // Compile the fragment shader
            IntBuffer compiled = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glCompileShaderARB(fragmentShaderID);
            ARBShaderObjects.glGetObjectParameterARB(fragmentShaderID,
                    ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, compiled);
            checkProgramError(compiled, fragmentShaderID, fragSource);

            // Attatch the program
            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderID);
        } else if (fragmentShaderID != -1) {
            removeFragShader();
            fragmentShaderID = -1;
        }

        ARBShaderObjects.glLinkProgramARB(programID);
        //setNeedsRefresh(true);
    }

    /** Removes the fragment shader */
    private void removeFragShader() {
        if (fragmentShaderID != -1) {
            ARBShaderObjects.glDetachObjectARB(programID, fragmentShaderID);
            ARBShaderObjects.glDeleteObjectARB(fragmentShaderID);
            fragmentShaderID = -1;
        }
    }

    /** Removes the vertex shader */
    private void removeVertShader() {
        if (vertexShaderID != -1) {
            ARBShaderObjects.glDetachObjectARB(programID, vertexShaderID);
            ARBShaderObjects.glDeleteObjectARB(vertexShaderID);
            vertexShaderID = -1;
        }
    }

    private void removeProgam() {
        removeVertShader();
        removeFragShader();
        if(programID != -1 ) {
            ARBShaderObjects.glDeleteObjectARB(programID);
            programID = -1;
        }
    }
    
    /**
     * Check for program errors. If an error is detected, program exits.
     *
     * @param compiled the compiler state for a given shader
     * @param id shader's id
     */
    private void checkProgramError(IntBuffer compiled, int id, String source) {
        if (compiled.get(0) == 0) {
            IntBuffer iVal = BufferUtils.createIntBuffer(1);
            ARBShaderObjects.glGetObjectParameterARB(id,
                    ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);
            int length = iVal.get();
            String out = null;

            if (length > 0) {
                ByteBuffer infoLog = BufferUtils.createByteBuffer(length);

                iVal.flip();
                ARBShaderObjects.glGetInfoLogARB(id, iVal, infoLog);

                byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out + source);

            throw new VleException("Error compiling GLSL shader: " + out);
        }
    }

    /**
     * Applies those shader objects to the current scene. Checks if the
     * GL_ARB_shader_objects extension is supported before attempting to enable
     * those objects.
     *
     * @see com.jme.scene.state.RenderState#apply()
     */
    @Override
    public void apply( RenderContext context ) {
        
        if(indep==null)
                context.currentStates[getType()] = this;
        if ( isSupported() ) {
            if ( isEnabled() ) {
                if( needsRefresh ) {
                    removeProgam();
                    if( this.vertexByteBuffer != null && this.fragmentByteBuffer != null ) {
                        load(vertexByteBuffer, fragmentByteBuffer);
                        vertexByteBuffer = null;
                        fragmentByteBuffer = null;
                    }
                    needsRefresh = false;
                }
                if (programID != -1) {
                    ARBShaderObjects.glUseProgramObjectARB(programID);
                    context.currentProgramid = programID;
                }
            } else {
                ARBShaderObjects.glUseProgramObjectARB(0);
                context.currentProgramid = 0;
            }
        }
        needsRefresh = false;
    }
    
    // do nothing for implementation
    @Override
    public void update(RenderContext ctx) {   
    }
    
    @Override
    public ShaderVariableLocation getVariableLocation(ShaderVariable var, RenderContext ctx) {
        ShaderVariableLocation loc=null;
        if(var.variableIndex<0) {
            var.variableIndex = ctx.app.ensureVariableIndex(var.name);
        }
        if(shaderVariableLocations.size()>var.variableIndex)
            loc=shaderVariableLocations.get(var.variableIndex);
        if(loc==null) {
            // create a new variable location
            loc = new ShaderVariableLocation();
            loc.name = var.name;
            loc.type = var.type;
            // add it to the map
            shaderVariableLocations.set(var.variableIndex, loc);
        }
        return loc;
    }
    
    @Override
    public int getProgramId() {
        return this.programID;
    }
}