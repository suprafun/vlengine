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

package com.vlengine.thread;

import com.vlengine.app.AppContext;
import com.vlengine.bounding.BoundingBox;
import com.vlengine.bounding.ReuseManager;
import com.vlengine.bounding.TreeComparator;
import com.vlengine.intersection.IntersectionRecord;
import com.vlengine.math.Matrix3f;
import com.vlengine.math.Matrix4f;
import com.vlengine.math.Quaternion;
import com.vlengine.math.Vector2f;
import com.vlengine.math.Vector3f;
import com.vlengine.renderer.CullContext;
import com.vlengine.util.BitSet;
import com.vlengine.util.FastList;
import com.vlengine.util.geom.BufferUtils;
import java.nio.FloatBuffer;

/**
 *
 * @author vear (Arpad Vekas)
 */
public class Context {

         // not temp, the SceneContext used with updating, culling, and rendering
         public CullContext scene = null;
         // not temp, the pointer to root application context
         public AppContext app = null;
         // not temp, is this the OGL thread?
         //public boolean inOglThread = false;
         
         // tmp variables for Quaternion
         public final Vector3f tmpYaxis = new Vector3f();
         public final Vector3f tmpZaxis = new Vector3f();
         public final Vector3f tmpXaxis = new Vector3f();
         
         // tmp variables for Matrix4f
         public final Vector3f tmpMatrix4f_f = new Vector3f();
         public final Vector3f tmpMatrix4f_s = new Vector3f();
         public final Vector3f tmpMatrix4f_u = new Vector3f();
         public final Matrix4f tmpMatrix4f_m = new Matrix4f();
         public final Quaternion tmpMatrix4f_q = new Quaternion();
         public final Quaternion tmpMatrix4f_q2 = new Quaternion();
         public final float[] tmpMatrix4fArr16 = new float[16];
         
         // tmp variables for Ray
         public final Vector3f rytmpVa=new Vector3f();
         public final Vector3f rytmpVb=new Vector3f();
         public final Vector3f rytmpVc=new Vector3f();
         public final Vector3f rytmpVd=new Vector3f();
         
         // tmp variables for BoundingVolume
         public final Vector3f _compVect1 = new Vector3f();
         public final Vector3f _compVect2 = new Vector3f();
         
         // tmp variables for BoundingBox
         public final Matrix3f _compMat = new Matrix3f();
         public final BoundingBox tempBoundingBox = new BoundingBox();
         public final BoundingBox tempBoundingBox2 = new BoundingBox();
         public final float[] fWdU = new float[3];
         public final float[] fAWdU = new float[3];
         public final float[] fDdU = new float[3];
         public final float[] fADdU = new float[3];
         public final float[] fAWxDdU = new float[3];
	 public final Vector3f[] bbverts = new Vector3f[3];
	 public final Vector3f[] bbtarget = new Vector3f[3];
         
         // tmp variables for boundingsphere
        //final public FloatBuffer tmpBoudingBox_mergeBuf = BufferUtils.createVector3Buffer(8);
        public final Vector3f[] tmpBoudingSphereverts = new Vector3f[3];
        public final Vector3f tmpBoudingSpheretempA = new Vector3f();
        public final Vector3f tmpBoudingSpheretempB = new Vector3f();
        public final Vector3f tmpBoudingSpheretempC = new Vector3f();
        public final Vector3f tmpBoudingSpheretempD = new Vector3f();
        public FloatBuffer tmpBoudingSphere_Buf = null;


         // intersection record returned from intersectsWhere
         public final IntersectionRecord parIntersectionRecord = new IntersectionRecord();
         
         // tmp variables for ModelBatch
         public final Vector3f compVect = new Vector3f();
         
         // tmp variables for Spatial
         public final Vector3f compVecA = new Vector3f();
         public final Quaternion compQuat = new Quaternion();
               
	// tmp variables for CollisionTree
	 public final Vector3f tempVa = new Vector3f();
	 public final Vector3f tempVb = new Vector3f();
	 public final Vector3f tempVc = new Vector3f();
	 public final Vector3f tempVd = new Vector3f();
	 public final Vector3f tempVe = new Vector3f();
	 public final Vector3f tempVf = new Vector3f();
	 public final Vector3f[] ctverts = new Vector3f[3];
	 public final Vector3f[] cttarget = new Vector3f[3];
         public final ReuseManager ctbstack = new ReuseManager();

	//Comparator used to sort triangle indices
	 public final TreeComparator comparator = new TreeComparator();
        
         // tmp variables for Intersection
         public final Vector3f istempVa = new Vector3f();
	 public final Vector3f istempVb = new Vector3f();
	 public final Vector3f istempVc = new Vector3f();
	 public final Vector3f istempVd = new Vector3f();
	 public final Vector3f istempVe = new Vector3f();
	 public final float[] istempFa = new float[2];
	 public final float[] istempFb = new float[2];
	 public final Vector2f istempV2a = new Vector2f();
	 public final Vector2f istempV2b = new Vector2f();
         public final Vector3f tIntersectionV1 = new Vector3f();
         
         // tmp variables for LWJGLCamera
         public final FloatBuffer lwcmtmp_FloatBuffer = BufferUtils.createFloatBuffer(16);
         
         // tmp variables for Renderable
          public final BitSet qlist = new BitSet();

          // tmp for LWJGLRenderer
              // temp array for debug rendering
          public final FastList<Object> directDrawList = new FastList<Object>();
          
          // tmp for CollisonData
          public final Vector3f tempCollisonDataV1 = new Vector3f();
          public final Vector3f tempCollisonDataV2 = new Vector3f();
          public final Vector3f tempCollisonDataV3 = new Vector3f();
          public final Vector3f tempCollisonDataV4 = new Vector3f();
          public final Vector3f[] tempCollisonDataVerts = new Vector3f[3];
          
          
          // tmp data for CollisionVolume
          public final Vector3f[] tCollisionVolumeVerts = new Vector3f[3];
          
          // tp data for Spatial
          public final Quaternion tSpatialq1 = new Quaternion();
          
          // tmp buffer for LWJGLFogState
          public final FloatBuffer tLWJGLFogState_colorBuff = BufferUtils.createColorBuffer(1);
}
