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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * @author lex (Aleksey Nikiforov)
 *
 */
public class FileRecord {

	private String md5;
	private long date;
	private long size;
	private String file;
	
	private FileRecord() { }
	
	public FileRecord(String md5, long date, long size, String file) {
		this.md5 = md5;
		this.date = date;
		this.size = size;
		this.file = file;
	}

	public static FileRecord fromString(String s)
		throws MalformedRecordException
	{
		FileRecord record = new FileRecord();
		
		StringTokenizer tokens = new StringTokenizer(s);
		
		try {

			record.md5 = tokens.nextToken();
			
			try {
				record.date = Long.parseLong(tokens.nextToken());
			} catch (NumberFormatException e) {
				throw new MalformedRecordException("Unable to parse date.");
			}
			
			try {
				record.size = Long.parseLong(tokens.nextToken());
			} catch (NumberFormatException e) {
				throw new MalformedRecordException("Unable to parse size.");
			}
			
			record.file = tokens.nextToken("").substring(1);
			
		} catch (NoSuchElementException e) {
			throw new MalformedRecordException("The record is uncomplete.");
		}
		
		return record;
	}

	public long getDate() {
		return date;
	}
	
	public long getSize() {
		return size;
	}

	public String getMd5() {
		return md5;
	}

	public String getFile() {
		return file;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(md5);
		sb.append(" ");
		sb.append(date);
		sb.append(" ");
		sb.append(size);
		sb.append(" ");
		sb.append(file);
		
		return sb.toString();
	}
}
