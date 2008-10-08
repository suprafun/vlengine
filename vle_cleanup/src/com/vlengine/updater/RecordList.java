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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author lex (Aleksey Nikiforov)
 */
public class RecordList {
	
	private static final Logger log = Logger.getLogger(
			RecordList.class.getName());
	
	
	protected long totalSize;
	protected HashMap<String, FileRecord> records;

	public RecordList() {
		records = new HashMap<String, FileRecord>();
	}
	
	public RecordList(HashMap<String, FileRecord> records) {
		this.records = records;
		for (FileRecord record : records.values()) {
			totalSize += record.getSize();
		}
	}
	
	public HashMap<String, FileRecord> getRecords() {
		return records;
	}
	
	public long getTotalSize() {
		return totalSize;
	}
	
	public void addRecord(FileRecord record) {
		totalSize += record.getSize();
		
		FileRecord prev = records.put(record.getFile(), record);
		if (prev != null) {
			totalSize -= prev.getSize();
		}
	}
	
	public void removeRecord(FileRecord record) {
		FileRecord old = records.remove(record.getFile());
		if (old != null) totalSize -= old.getSize();
	}
	
	public void clearAll(RecordList recordList) {
		for (FileRecord record : recordList.getRecords().values()) {
			removeRecord(record);
		}
	}
	
	public void applyPatch(RecordList recordList) {
		for (FileRecord record : recordList.getRecords().values()) {
			addRecord(record);
		}
	}
	
	public static RecordList fromFile(InputStream in, FileRecord[] last)
		throws IOException
	{
		if (last != null && last.length == 0) last = null;
		
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(in));
		
		try {
			
			RecordList recordList = new RecordList();
			
			int lineNumber = 0;
			String line = "";
			
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				FileRecord record;
				
				try {
					record = FileRecord.fromString(line);
				} catch (MalformedRecordException e) {
					log.log(Level.SEVERE,
							"MalformedRecord on line '" + lineNumber + "'.",
							e);
					continue;
				}
				
				recordList.addRecord(record);
				if (last != null) last[0] = record;
			}
			
			return recordList;
			
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
		}
	}
	
	public void save(OutputStream out) throws IOException {
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(out));
		
		try {
			
			for (FileRecord record : records.values()) {
				writer.write(record.toString());
				writer.write("\n");
			}
			
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "File socket lost.");
			}
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		sb.append("\n");
		sb.append("size = ");
		sb.append(totalSize);
		sb.append(" bytes");
		sb.append("\n");
		
		for (FileRecord record : records.values()) {
			sb.append(record.toString());
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
