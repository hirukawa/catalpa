package net.osdn.catalpa.upload;

import java.io.File;

public interface UploadConfig {

	public UploadType getType();
	public int upload(File dir) throws Exception;
	
}
