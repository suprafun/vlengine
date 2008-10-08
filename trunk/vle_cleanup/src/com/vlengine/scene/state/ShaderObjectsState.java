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

package com.vlengine.scene.state;

import com.vlengine.app.Config;
import com.vlengine.renderer.RenderContext;
import com.vlengine.renderer.material.ShaderKey;
import com.vlengine.scene.state.shader.ShaderVariable;
import com.vlengine.scene.state.shader.ShaderVariableLocation;
import com.vlengine.system.DisplaySystem;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the GL_ARB_shader_objects extension.
 *
 * @author Thomas Hourdel
 * @author Rikard Herlitz (MrCoder)
 */
public class ShaderObjectsState extends RenderState {
    private static final Logger logger = Logger
            .getLogger(ShaderObjectsState.class.getName());

    /** Storage for shader uniform values */
    
    //protected FastList<ShaderVariable> shaderUniforms =
    //        new FastList<ShaderVariable>();
    
    /** Storage for shader attribute values */
    //protected FastList<ShaderVariable> shaderAttributes =
    //        new FastList<ShaderVariable>();
    
        
    protected static boolean supported = false;
    
    /** Holds the maximum number of vertex attributes available. */
    protected static int maxVertexAttribs;

    protected static boolean inited = false;
    
    protected ByteBuffer vertexByteBuffer;
    protected ByteBuffer fragmentByteBuffer;
    
    protected String fragSource;
    protected String vertSource;
    
    // default is no special flags
    protected ShaderKey shaderKey;
    
    public void setShaderKey(ShaderKey key) {
        this.shaderKey = key;
    }
    
    public ShaderKey getShaderKey() {
        return shaderKey;
    }

    /**
     * <code>isSupported</code> determines if the ARB_shader_objects extension
     * is supported by current graphics configuration.
     *
     * @return if ARB shader objects are supported
     */
    public static boolean isSupported() {
        return supported;
    }

    /*
    public void defineUniform( String name ) {
        boolean found = false;
        for (int i = shaderUniforms.size(); --i >= 0;) {
            ShaderVariable temp = shaderUniforms.get(i);
            if (name.equals(temp.name)) {
                found = true;
            }
        }
        if( !found ) {
            ShaderVariable un = new ShaderVariable();
            un.name = name;
            shaderUniforms.add(un);
        }
    }
    
    public void defineAttribute( String name ) {
        boolean found = false;
        for (int i = shaderAttributes.size(); --i >= 0;) {
            ShaderVariable temp = shaderAttributes.get(i);
            if (name.equals(temp.name)) {
                found = true;
            }
        }
        if( !found ) {
            ShaderVariable un = new ShaderVariable();
            un.name = name;
            shaderAttributes.add(un);
        }
    }
     */
    
    /**
     * @return RS_SHADER_OBJECTS
     * @see com.jme.scene.state.RenderState#getType()
     */
    public int getType() {
        return RS_GLSL_SHADER_OBJECTS;
    }

    /* (non-Javadoc)
     * @see com.jme.scene.state.GLSLShaderObjectsState#checkAttributeSizeLimits()
     */
    /*
    public boolean checkAttributeSizeLimits() {
      if (shaderAttributes.size() > maxVertexAttribs) {
            logger.severe("Too many shader attributes(standard+defined): "
                            + shaderAttributes.size() + " maximum: "
                            + maxVertexAttribs);
            return false;
        } else {
          // check if we are on nVidia
          if(DisplaySystem.getDisplaySystem().getDisplaySignature().toUpperCase().contains("NVIDIA")) {
              // on nvidia reserve the first 10 attributes for fixed function attributes
              // this will allow for 2 texcoord sets
              if (shaderAttributes.size() + 10 > maxVertexAttribs)
                logger.warning("User defined attributes might overwrite default OpenGL attributes");
              return false;
          }
        }
      return true;
    }
     */
    
    /**
     * Load an URL and grab content into a ByteBuffer.
     *
     * @param url the url to load
     * @return the loaded url
     */
    private ByteBuffer load(java.net.URL url) {
        try {
            BufferedInputStream bufferedInputStream =
                    new BufferedInputStream(url.openStream());
            DataInputStream dataStream =
                    new DataInputStream(bufferedInputStream);
            byte shaderCode[] = new byte[bufferedInputStream.available()];
            dataStream.readFully(shaderCode);
            bufferedInputStream.close();
            dataStream.close();
            ByteBuffer shaderByteBuffer =
                    BufferUtils.createByteBuffer(shaderCode.length);
            shaderByteBuffer.put(shaderCode);
            shaderByteBuffer.rewind();

            return shaderByteBuffer;
        } catch (Exception e) {
            logger.severe("Could not load shader object: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
            return null;
        }
    }

    /**
     * Loads a string into a ByteBuffer
     *
     * @param data string to load into ByteBuffer
     * @return the converted string
     */
    private ByteBuffer load(String data) {
        try {
            byte[] bytes = data.getBytes();
            ByteBuffer program = BufferUtils.createByteBuffer(bytes.length);
            program.put(bytes);
            program.rewind();
            return program;
        } catch (Exception e) {
            logger.severe("Could not load fragment program: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
            return null;
        }
    }

    /**
     * Loads the shader object. Use null for an empty vertex or empty fragment
     * shader.
     *
     * @param vert vertex shader
     * @param frag fragment shader
     * @see com.jme.scene.state.GLSLShaderObjectsState#load(java.net.URL,
     *java.net.URL)
     */
    public void load(URL vert, URL frag) {
        vertexByteBuffer = vert != null ? load(vert) : null;
        fragmentByteBuffer = frag != null ? load(frag) : null;
        needsRefresh = true;
    }

    /**
     * Loads the shader object. Use null for an empty vertex or empty fragment
     * shader.
     *
     * @param vert vertex shader
     * @param frag fragment shader
     * @see com.jme.scene.state.GLSLShaderObjectsState#load(java.net.URL,
     *java.net.URL)
     */
    public void load(String vert, String frag) {
        if(Config.p_storeshadersource) {
            vertSource = vert;
            fragSource = frag;
        }
        vertexByteBuffer = vert != null ? load(vert) : null;
        fragmentByteBuffer = frag != null ? load(frag) : null;
        needsRefresh = true;
    }
    
    @Override
    public void update(RenderContext ctx) {
        if( indep != null )
            return;
        super.update(ctx);
        if( needsRefresh ) {
            ((ShaderObjectsState)impl).enabled = this.enabled;
            ((ShaderObjectsState)impl).needsRefresh = true;

            ((ShaderObjectsState)impl).vertexByteBuffer = this.vertexByteBuffer;
            ((ShaderObjectsState)impl).fragmentByteBuffer = this.fragmentByteBuffer;
            ((ShaderObjectsState)impl).shaderKey = this.shaderKey;
            ((ShaderObjectsState)impl).fragSource = this.fragSource;
            ((ShaderObjectsState)impl).vertSource = this.vertSource;

            needsRefresh = false;
        }
    }

    public ShaderVariableLocation getVariableLocation(ShaderVariable var, RenderContext ctx) {
        if(impl!=null)
            return ((ShaderObjectsState)impl).getVariableLocation(var, ctx);
        return null;
    }
    
    public int getProgramId() {
        if(impl!=null)
            return ((ShaderObjectsState)impl).getProgramId();
        return 0;
    }
}