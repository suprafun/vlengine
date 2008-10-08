/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vlengine.model;

import com.vlengine.math.Vector3f;
import com.vlengine.scene.LodMesh;
import com.vlengine.scene.batch.TriBatch;

/**
 *
 * @author lex (Aleksey Nikiforov)
 */
public class PrimitiveFactory {

    public static LodMesh createBox(String name, Vector3f center,
            float xEntent, float yExtent, float zExtent)
    {
        LodMesh m = new LodMesh(name);
        Box box = new Box(center, xEntent, yExtent, zExtent);
        m.addBatch(0, new TriBatch(box));
        
        return m;
    }
}
