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

import com.vlengine.math.Matrix4f;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.thread.Context;
import com.vlengine.thread.LocalContext;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.glu.GLU;


/**
 * <code>LWJGLCamera</code> defines a concrete implementation of a
 * <code>AbstractCamera</code> using the LWJGL library for view port setting.
 * Most functionality is provided by the <code>AbstractCamera</code> class with
 * this class handling the OpenGL specific calls to set the frustum and
 * viewport.
 * @author Mark Powell
 * @version $Id: LWJGLCamera.java,v 1.22 2007/09/20 15:14:43 nca Exp $
 */
public class LWJGLCamera extends ViewCamera {
    
    public LWJGLCamera() {}

    /**
     * Constructor instantiates a new <code>LWJGLCamera</code> object. The
     * width and height are provided, which cooresponds to either the
     * width and height of the rendering window, or the resolution of the
     * fullscreen display.
     * @param width the width/resolution of the display.
     * @param height the height/resolution of the display.
     */
    public LWJGLCamera(int width, int height, Object parent) {
        super();
        this.width = width;
        this.height = height;
        //this.parent = parent;
        update();
        apply();
    }
    
    /**
     * Constructor instantiates a new <code>LWJGLCamera</code> object. The
     * width and height are provided, which cooresponds to either the
     * width and height of the rendering window, or the resolution of the
     * fullscreen display.
     * @param width the width/resolution of the display.
     * @param height the height/resolution of the display.
     */
    public LWJGLCamera(int width, int height, boolean dataOnly) {
        super(dataOnly);
        this.width = width;
        this.height = height;
        //this.parent = parent;
        setDataOnly(dataOnly);
        update();
        apply();
    }

    @Override
    public void update() {
        super.update();
    }

    public void apply() {
        doFrustumChange();
        doViewPortChange();
        doFrameChange();
    }

    /**
     * Sets the OpenGL frustum.
     * @see com.jme.renderer.Camera#onFrustumChange()
     */
    public void doFrustumChange() {

        if (!isDataOnly()) {
            // set projection matrix
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            if ( !isParallelProjection() ) {
                /*
                if(cropMatrix!=null && !cropMatrix.isIdentity()) {
                    // we have a zoom-in, so update the projection matrix
                    // and load it directly
                    if(updateProjectionMatrix)
                        checkViewProjection();
                    // load this matrix first
                    Context tmp = LocalContext.getContext();
                    FloatBuffer tmp_FloatBuffer = tmp.lwcmtmp_FloatBuffer;
                    tmp_FloatBuffer.clear();
                    projection.fillFloatBuffer(tmp_FloatBuffer, true);
                    tmp_FloatBuffer.rewind();
                    GL11.glLoadMatrix(tmp_FloatBuffer);
                } else {
                 */
                    GL11.glFrustum(
                        frustumLeft,
                        frustumRight,
                        frustumBottom,
                        frustumTop,
                        frustumNear,
                        frustumFar);
                //}
            }
            else {
                GL11.glOrtho(
                        frustumLeft,
                        frustumRight,
                        frustumTop,
                        frustumBottom,
                        frustumNear,
                        frustumFar);
            }
            if ( projection != null ) {
                Context tmp = LocalContext.getContext();
                FloatBuffer tmp_FloatBuffer = tmp.lwcmtmp_FloatBuffer;
                tmp_FloatBuffer.rewind();
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, tmp_FloatBuffer);
                tmp_FloatBuffer.rewind();
                projection.readFloatBuffer( tmp_FloatBuffer );
                updateProjectionMatrix = false;
            }
        }
    }

    /**
     * Sets OpenGL's viewport.
     * @see com.jme.renderer.Camera#onViewPortChange()
     */
    public void doViewPortChange() {

        if (!isDataOnly()) {
            // set view port
            int x = (int) (viewPortLeft * width);
            int y = (int) (viewPortBottom * height);
            int w = (int) ((viewPortRight - viewPortLeft) * width);
            int h = (int) ((viewPortTop - viewPortBottom) * height);
            GL11.glViewport(x, y, w, h);
        }
    }

    /**
     * Uses GLU's lookat function to set the OpenGL frame.
     * @see com.jme.renderer.Camera#onFrameChange()
     */
    public void doFrameChange() {
        super.onFrameChange();

        if (!isDataOnly()) {
            // set view matrix
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GLU.gluLookAt(
                location.x,
                location.y,
                location.z,
                location.x + direction.x,
                location.y + direction.y,
                location.z + direction.z,
                up.x,
                up.y,
                up.z);
    
            if ( modelView != null ){
                
                // get the buffer from thread local context
                Context tmp = LocalContext.getContext();
                FloatBuffer tmp_FloatBuffer = tmp.lwcmtmp_FloatBuffer;
                
                tmp_FloatBuffer.rewind();
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, tmp_FloatBuffer);
                tmp_FloatBuffer.rewind();
                modelView.readFloatBuffer( tmp_FloatBuffer );
                updateModelViewMatrix = false;
            }
        }
    }
}
