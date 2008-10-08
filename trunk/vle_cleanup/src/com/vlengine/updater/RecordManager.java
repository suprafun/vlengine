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

package com.vlengine.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is not thread safe. However FileProgress retrieved from this class
 * can be safely read in other threads.
 * 
 * @author lex (Aleksey Nikiforov)
 */
public class RecordManager {

	private static final Logger log = Logger.getLogger(
			RecordManager.class.getName());
	
	
	private MessageDigest md5Digest;
	private byte[] buffer;
	private byte[] md5Bytes = new byte[17];
	
	protected FileProgress progress;
	
	public RecordManager() {
		this(10*1024);
	}
	
	/**
	 * @param bufferSize
	 * @param yieldInterval 0 = no yielding
	 */
	public RecordManager(int bufferSize) {
		try {
			buffer = new byte[bufferSize];
			progress = new FileProgress();
			
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "MD5 algorithm is not found.");
			throw new RuntimeException("MD5 algorithm is not found.");
		}
	}
	
	public FileProgress getFileProgress() {
		return progress;
	}
	
	public RecordList generateLocalList(File baseDir,
			HashMap<String, String> exclude)
	throws IOException
	{
		if (!baseDir.exists()) {
			throw new IOException("The target directory '" +
					baseDir.getAbsolutePath() + "' does not exists.");
		}
		if (!baseDir.isDirectory()) {
			throw new IOException("The target directory '" +
					baseDir.getAbsolutePath() + "' is a file.");
		}
		
		int pathSkip = baseDir.toURI().getPath().length();
		RecordList records = new RecordList();
		populateLocalList(baseDir, pathSkip, records, exclude);
		
		return records;
	}
	
	private void populateLocalList(File dir, int pathSkip, RecordList records,
			HashMap<String, String> exclude)
		throws IOException
	{
		File[] files = dir.listFiles();
		
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isDirectory()) {
				
				try {
					
					String relativePath = getRelativePath(file, pathSkip);
					if (exclude == null || !exclude.containsKey(relativePath)) {
						records.addRecord(new FileRecord(
							copyWithMd5(new FileInputStream(file), null),
							file.lastModified(),
							file.length(),
							relativePath));
					}
					
				} catch (FileNotFoundException e) {
					
				} catch (DigestException e) {
					throw new IOException(
							"Unable to genere local file list - could not " +
							"compute md5 for '" + file.getAbsolutePath() + "'.",
							e);
					
				} catch (IOException e) {
					throw new IOException(
							"Unable to generate local file list - error while "+
							"reading '" + file.getAbsolutePath() + "'.",
							e);
				}
				
			} else {
				populateLocalList(files[i], pathSkip, records, exclude);
			}
		}
	}
	
	private String getRelativePath(File file, int pathSkip) {
		return file.toURI().getPath().substring(pathSkip);
	}
	
	/**
	 * Given the destination list and the source list, this method will generate
	 * three lists: files to be added, files to be replaced and files
	 * to be removed.
	 * 
	 * @param dest
	 * @param source
	 * @return the array with add, replace and remove lists in that order
	 */
	public static RecordList[] findDifference(
			RecordList dest, RecordList source)
	{
		RecordList add = new RecordList();
		RecordList replace = new RecordList();
		RecordList remove;
		
		@SuppressWarnings("unchecked")
		HashMap<String, FileRecord> oldRecords = 
			(HashMap<String, FileRecord>)dest.getRecords().clone();
		
		for (FileRecord newRecord : source.getRecords().values()) {
			FileRecord oldRecord = oldRecords.remove(newRecord.getFile());
			
			if (oldRecord == null) {
				add.addRecord(newRecord);
			} else if (!oldRecord.getMd5().equals(newRecord.getMd5())) {
				replace.addRecord(newRecord);
			}
		}
		
		remove = new RecordList(oldRecords);
		
		return new RecordList[] {add, replace, remove};
	}
	
	/**
	 * The out stream can be null, in this case no copy is performed, only
	 * md5 sum is computed.
	 * 
	 * @param in the stream to copy from
	 * @param out the stream to copy into
	 * @return md5 sum encoded as a hex string, null if cancelled
	 * @throws IOException
	 * @throws DigestException
	 */
	public String copyWithMd5(InputStream in, OutputStream out)
		throws IOException, DigestException
	{
		try {
			
			md5Digest.reset();
			progress.reset();
			
			int read = 0;
			while ((read = in.read(buffer)) != -1) {
				if (out != null) out.write(buffer, 0, read);
				md5Digest.update(buffer, 0, read);
				progress.incrementProgress(read);
				
				if (progress.isCancelled()) return null;
				
				Thread.yield();
			}
			
			md5Digest.digest(md5Bytes, 1, 16);
			
			String md5String = new BigInteger(md5Bytes).toString(16);
			if (md5String.length() < 32) {
				
				StringBuilder sb = new StringBuilder(32);
				
				for (int i = 0; i < 32 - md5String.length(); i++) {
					sb.append("0");
				}
				
				sb.append(md5String);
				md5String = sb.toString();
			}
			
			return md5String;
			
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
			
			try {
				if (out != null) out.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
		}
	}
	
	public boolean copy(InputStream in, OutputStream out)
	throws IOException
	{
		try {
			
			progress.reset();
			
			int read = 0;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				progress.incrementProgress(read);
				
				if (progress.isCancelled()) return false;
				
				Thread.yield();
			}
			
			return true;
			
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
			
			try {
				if (out != null) out.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
		}
	}
	
}
