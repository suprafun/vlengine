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

package com.vlengine.test;

import com.vlengine.app.AppContext;
import com.vlengine.app.MainGame;
import com.vlengine.app.frame.Frame;
import com.vlengine.math.FastMath;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.ViewCamera;
import com.vlengine.renderer.lwjgl.LWJGLCamera;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Test035CameraMatrix extends MainGame {

    public static void main(String[] args) {
        Test035CameraMatrix test = new Test035CameraMatrix();
        test.start();
    }

    @Override
    protected void simpleInitGame(AppContext app) {
        // calculated matrixes
        ViewCamera vcam = new ViewCamera();
        Matrix4f cproj = new Matrix4f();
        Matrix4f cview = new Matrix4f();

        // matrixes from opengl
        Matrix4f oglproj = new Matrix4f();
        Matrix4f oglview = new Matrix4f();
        LWJGLCamera cam = new LWJGLCamera();
        
        Vector3f loc = new Vector3f( 0.0f, 0.0f, 25.0f );
        Vector3f up = new Vector3f( Vector3f.UNIT_Y );
        Vector3f lookpos = new Vector3f( 200.0f, -100f, 1000.0f );

        // calculate OGL camera
        cam.setFrustumPerspective( 45.0f, (float) app.display.getWidth()
                / (float) app.display.getHeight(), 1f, app.conf.view_frustrum_far );
        cam.setParallelProjection( false );
        cam.setLocation(loc);
        cam.lookAt(lookpos, up);
        cam.setUp(up);
        cam.update();
        cam.apply();
        oglproj.set(cam.getProjectionMatrix()).transposeLocal();
        oglview.set(cam.getModelViewMatrix()).transposeLocal();
        
        
        // calculate view camera
        vcam.setFrustumPerspective( 45.0f, (float) app.display.getWidth()
                / (float) app.display.getHeight(), 1f, app.conf.view_frustrum_far );
        vcam.setParallelProjection( false );
        vcam.setLocation(loc);
        vcam.lookAt(lookpos, up);
        vcam.setUp(up);
        vcam.update();
        // retrieve calculate camera matrices
        cview = vcam.getModelViewMatrix();
        //cview.lookAt(vcam.getLocation(), vcam.getLocation().add(vcam.getDirection()), vcam.getUp());
        cproj = vcam.getProjectionMatrix();
        
        // compare the matrixes
        float viewcheck = 0;
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m00 - oglview.m00));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m01 - oglview.m01));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m02 - oglview.m02));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m03 - oglview.m03));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m10 - oglview.m10));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m11 - oglview.m11));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m12 - oglview.m12));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m13 - oglview.m13));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m20 - oglview.m20));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m21 - oglview.m21));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m22 - oglview.m22));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m23 - oglview.m23));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m30 - oglview.m30));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m31 - oglview.m31));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m32 - oglview.m32));
        viewcheck = Math.max(viewcheck, FastMath.abs(cview.m33 - oglview.m33));
        
        float projcheck = 0;
        projcheck += cproj.m00 - oglproj.m00;
        projcheck += cproj.m01 - oglproj.m01;
        projcheck += cproj.m02 - oglproj.m02;
        projcheck += cproj.m03 - oglproj.m03;
        projcheck += cproj.m10 - oglproj.m10;
        projcheck += cproj.m11 - oglproj.m11;
        projcheck += cproj.m12 - oglproj.m12;
        projcheck += cproj.m13 - oglproj.m13;
        projcheck += cproj.m20 - oglproj.m20;
        projcheck += cproj.m21 - oglproj.m21;
        projcheck += cproj.m22 - oglproj.m22;
        projcheck += cproj.m23 - oglproj.m23;
        projcheck += cproj.m30 - oglproj.m30;
        projcheck += cproj.m31 - oglproj.m31;
        projcheck += cproj.m32 - oglproj.m32;
        projcheck += cproj.m33 - oglproj.m33;

        System.out.println("ViewMatrix checksum"+viewcheck);
        System.out.println("Projection Matrix checksum"+projcheck);
        app.finished = true;
    }

    @Override
    protected void simpleUpdate(Frame f) {
        
    }
}
