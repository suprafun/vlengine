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

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import com.vlengine.updater.ClientUpdater;
import com.vlengine.updater.FtpServerUpdater;
import com.vlengine.updater.RecordList;
import com.vlengine.updater.RecordManager;
import com.vlengine.updater.UpdateStatus;

/**
 * This updater updates files individually
 * @author lex (Aleksey Nikiforov)
 */
public class Test039AutoUpdater {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//generateDescriptor();
		
		testFtpUpdater();
	}
	
	public static void generateDescriptor() throws Exception {
		File localDir = new File("/home/lex/workspace/X-Shift_v01/dist");
		File localDescriptor = new File("/home/lex/workspace/X-Shift_v01/dist/xshift.idx");
		
		RecordManager recMan = new RecordManager();
		
		RecordList records = recMan.generateLocalList(localDir, null);
		records.save(new GZIPOutputStream(
				new FileOutputStream(localDescriptor)));
	}
	
	public static void testClientUpdater() throws Exception {
		File localDir = new File("/home/lex/Temp");
		File localDescriptor = new File("/home/lex/Temp/xshift.idx");
		File serverDir = new File("/home/lex/workspace/X-Shift_v01/dist");
		File serverDescriptor = new File("/home/lex/workspace/X-Shift_v01/dist/xshift.idx");
		
		final ClientUpdater cu = new ClientUpdater(localDir, localDescriptor,
				serverDir.toURI().toURL(), serverDescriptor.toURI().toURL());
		UpdateStatus status = cu.getUpdateStatus();
		
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					cu.update();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		t.start();
		
		System.out.println("Starting update.");
		do {
			System.out.println(status);
			Thread.sleep(500);
		} while (status.getJobProgressPercent() < 0.9999);
		System.out.println("Update complete.");
	}
	
	public static void testFtpUpdater() throws Exception {
		File localDir = new File("/home/lex/Temp/testdata");
		File localDescriptor = new File("/home/lex/Temp/testdata/xshift.idx");
		URL serverDescriptor = new URL("ftp://127.0.0.1:21/testdata/xshift.idx");
		
		String user = "";
		String password = "";
		
		FtpServerUpdater updater = new FtpServerUpdater(localDir,
				localDescriptor, null, serverDescriptor);
		updater.setAuthentication(user, password);
		
		updater.update();
	}

}
