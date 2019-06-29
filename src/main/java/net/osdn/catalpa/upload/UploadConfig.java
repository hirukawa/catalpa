package net.osdn.catalpa.upload;

import java.io.File;

import net.osdn.catalpa.ProgressObserver;

public interface UploadConfig {

	public UploadType getType();
	public int upload(File dir, ProgressObserver observer) throws Exception;
	
}
