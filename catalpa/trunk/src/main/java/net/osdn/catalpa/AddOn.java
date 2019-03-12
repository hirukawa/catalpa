package net.osdn.catalpa;

import java.nio.file.Path;
import java.util.Map;

public interface AddOn {

	public boolean isApplicable(String type) throws Exception;
	public void setCatalpa(Catalpa catalpa);
	public void prepare(Path inputPath, Path outputPath, Map<String, Object> config, Map<String, Object> options, Context context) throws Exception;
	public void execute(Context context) throws Exception;
	public void postExecute(Path inputPath, Path outputPath, Map<String, Object> options, Context context) throws Exception;

}
