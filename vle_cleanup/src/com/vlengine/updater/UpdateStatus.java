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

/**
 * This class can be written to in one thread and read from multiple threads.
 * 
 * @author lex (Aleksey Nikiforov)
 */
public class UpdateStatus {

	public enum Status {
		INACTIVE,
		
		ADDING,
		UPDATING,
		CLEANING_UP,
		
		COMPLETE,
		CANCELLED,
		ERROR
	}
	
	
	private Status status;
	private long totalSize;
	
	private long processedSize;
	private FileRecord currentFile;
	private FileProgress fileProgress;
	
	public UpdateStatus(FileProgress fileProgress) {
		this.fileProgress = fileProgress;
		status = Status.INACTIVE;
	}

	public synchronized Status getStatus() {
		return status;
	}

	public synchronized long getTotalSize() {
		return totalSize;
	}

	public synchronized long getJobProgressBytes() {
		return processedSize;
	}

	public synchronized FileRecord getCurrentFile() {
		return currentFile;
	}
	
	public synchronized long getFileProgressBytes() {
		return fileProgress.getProgress();
	}

	protected synchronized void setStatus(Status status) {
		this.status = status;
		
		if (status == Status.COMPLETE ||
				status == Status.CANCELLED ||
				status == Status.ERROR)
		{
			finish();
		}
	}

	protected synchronized void setJobSize(long totalSize) {
		this.totalSize = totalSize;
	}
	
	protected synchronized void nextFile(FileRecord file) {
		if (isCancelled()) return;
		
		if (currentFile != null) processedSize += currentFile.getSize();
		fileProgress.reset();
		
		this.currentFile = file;
	}
	
	public synchronized float getJobProgressPercent() {
		if (isFinished()) return 1;
		if (totalSize < 1) return 0;
		return (float) (processedSize + fileProgress.getProgress()) / totalSize;
	}
	
	public synchronized float getFileProgressPercent(){
		if (currentFile == null || currentFile.getSize() < 1) return 0;
		return (float) fileProgress.getProgress() / currentFile.getSize();
	}

	protected synchronized void reset() {
		status = Status.INACTIVE;
		totalSize = 0;
		
		processedSize = 0;
		currentFile = null;
		fileProgress.reset();
	}
	
	private void finish() {
		totalSize = 0;
		
		processedSize = 0;
		currentFile = null;
		fileProgress.reset();
	}
	
	public synchronized boolean isFinished() {
		return (status == Status.COMPLETE ||
				status == Status.CANCELLED ||
				status == Status.ERROR);
	}
	
	public synchronized boolean isCancelled() {
		return fileProgress.isCancelled();
	}

	public synchronized void cancel() {
		fileProgress.cancel();
	}
	
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(status);
		sb.append(" File: ");
		sb.append(getFileProgressPercent() * 100);
		sb.append("% Total: ");
		sb.append(getJobProgressPercent() * 100);
		sb.append("%");
		
		return sb.toString();
	}
	
}
