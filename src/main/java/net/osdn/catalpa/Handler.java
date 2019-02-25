package net.osdn.catalpa;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public interface Handler {
	
	int getPriority();
	boolean isApplicable(Path path);
	void handle(Context context, Reader reader, Writer writer) throws Exception;
	
}
