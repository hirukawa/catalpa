package net.osdn.catalpa.handler;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessSource;
import com.github.sommeri.less4j.LessSource.FileSource;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.core.DefaultLessCompiler;

import net.osdn.catalpa.Context;
import net.osdn.catalpa.Handler;
import net.osdn.catalpa.Util;

public class LessHandler implements Handler {
	
	protected static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".less"
	};
	
	private static final String OUTPUT_EXTENSION = ".css";
	
	private LessCompiler compiler = new DefaultLessCompiler();
	
	@Override
	public int getPriority() {
		return 500;
	}

	@Override
	public boolean isApplicable(Path path) {
		String filename = path.getFileName().toString().toLowerCase();
		for(String ext : APPLICABLE_EXTENSIONS) {
			if(filename.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void handle(Context context, Reader reader, Writer writer) throws IOException, Less4jException {
		context.setOutputPath(Util.replaceFileExtension(context.getOutputPath(), APPLICABLE_EXTENSIONS, OUTPUT_EXTENSION));
		
		LessSource source = new ReaderSource(context, context.getInputPath().getParent(), reader);
		CompilationResult result = compiler.compile(source);
		writer.write(result.getCss());
	}
	
	private class ReaderSource extends LessSource {
		
		private Context context;
		private Path basePath;
		private String content;
		
		public ReaderSource(Context context, Path basePath, Reader reader) throws IOException {
			this.context = context;
			this.basePath = basePath;
			StringBuilder sb = new StringBuilder();
			for(String line : Util.readAllLines(reader)) {
				sb.append(line);
				sb.append("\r\n");
			}
			this.content = sb.toString();
		}

		@Override
		public LessSource relativeSource(String filename) throws FileNotFound, CannotReadFile, StringSourceException {
			return new TrackingFileSource(context, basePath.resolve(filename).toFile());
		}

		@Override
		public String getContent() throws FileNotFound, CannotReadFile {
			return content;
		}

		@Override
		public byte[] getBytes() throws FileNotFound, CannotReadFile {
			return content.getBytes();
		}
	}
	
	public static class TrackingFileSource extends FileSource {
		
		private Context context;

		public TrackingFileSource(Context context, File inputFile) {
			super(inputFile);
			this.context = context;
			try {
				FileTime lastModifiedTime = Files.getLastModifiedTime(inputFile.toPath());
				if(lastModifiedTime.compareTo(context.getLastModifiedTime()) > 0) {
					context.setLastModifiedTime(lastModifiedTime);
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public FileSource relativeSource(String filename) {
			File file = new File(getInputFile().getParentFile(), filename);
			return new TrackingFileSource(context, file);
		}
	}
}
