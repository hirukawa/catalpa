package net.osdn.catalpa.addon.blog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import net.osdn.catalpa.Util;
import net.osdn.catalpa.handler.TemplateHandler;

public class Pager {
	
	private Link previous;
	private Link next;

	private Path rootInputPath;
	private Path rootOutputPath;
	private Map<Path, Integer> index = new HashMap<Path, Integer>();
	private List<Post> posts = new ArrayList<Post>();
	
	protected Pager(Link previous, Link next) {
		this.previous = previous;
		this.next = next;
	}
	
	protected Pager(Path rootInputPath, Path rootOutputPath, List<Post> posts) {
		this.rootInputPath = rootInputPath;
		this.rootOutputPath = rootOutputPath;
		
		int i = 0;
		for(Post post : posts) {
			this.posts.add(post);
			this.index.put(post.getPath(), i++);
		}
	}

	protected Path getCurrentInputPath() throws TemplateModelException {
		Environment env = Environment.getCurrentEnvironment();
		if(env != null) {
			TemplateHashModel dataModel = env.getDataModel();
			if(dataModel != null) {
				TemplateModel obj = dataModel.get("_INPUT_PATH");
				if(obj instanceof TemplateScalarModel) {
					return Paths.get(((TemplateScalarModel)obj).getAsString());
				}
			}
		}
		return null;
	}
	
	public Link getPrevious() throws TemplateModelException {
		if(previous != null) {
			return (previous.getUrl() != null) ? previous : null;
		}
		
		Path inputPath = null;
		Path outputPath = null;
		
		Environment env = Environment.getCurrentEnvironment();
		if(env != null) {
			TemplateHashModel dataModel = env.getDataModel();
			if(dataModel != null) {
				TemplateModel i = dataModel.get("_INPUT_PATH");
				if(i instanceof TemplateScalarModel) {
					inputPath = Paths.get(((TemplateScalarModel)i).getAsString());
				}
				TemplateModel o = dataModel.get("_OUTPUT_PATH");
				if(o instanceof TemplateScalarModel) {
					outputPath = Paths.get(((TemplateScalarModel)o).getAsString());
				}
			}
		}
		
		if(inputPath != null && outputPath != null) {
			Integer i = index.get(inputPath);
			if(i != null) {
				int index = i + 1;
				if(0 <= index && index < posts.size()) {
					Post previous = posts.get(index);
					Path fromPath = rootOutputPath.relativize(outputPath);
					Path toPath = rootInputPath.relativize(Util.replaceFileExtension(previous.getPath(), TemplateHandler.APPLICABLE_EXTENSIONS, ".html"));
					String url = fromPath.getParent().relativize(toPath.getParent()).resolve(toPath.getFileName()).toString().replace('\\', '/');
					Link link = new Link(previous.getDate(), previous.getTitle(), url);
					return link;
				}
			}
		}
		return null;
	}
	
	public Link getNext() throws TemplateModelException {
		if(next != null) {
			return (next.getUrl() != null) ? next : null;
		}
		
		Path inputPath = null;
		Path outputPath = null;
		
		Environment env = Environment.getCurrentEnvironment();
		if(env != null) {
			TemplateHashModel dataModel = env.getDataModel();
			if(dataModel != null) {
				TemplateModel i = dataModel.get("_INPUT_PATH");
				if(i instanceof TemplateScalarModel) {
					inputPath = Paths.get(((TemplateScalarModel)i).getAsString());
				}
				TemplateModel o = dataModel.get("_OUTPUT_PATH");
				if(o instanceof TemplateScalarModel) {
					outputPath = Paths.get(((TemplateScalarModel)o).getAsString());
				}
			}
		}
		
		if(inputPath != null && outputPath != null) {
			Integer i = index.get(inputPath);
			if(i != null) {
				int index = i - 1;
				if(0 <= index && index < posts.size()) {
					Post next = posts.get(index);
					Path fromPath = rootOutputPath.relativize(outputPath);
					Path toPath = rootInputPath.relativize(Util.replaceFileExtension(next.getPath(), TemplateHandler.APPLICABLE_EXTENSIONS, ".html"));
					String url = fromPath.getParent().relativize(toPath.getParent()).resolve(toPath.getFileName()).toString().replace('\\', '/');
					Link link = new Link(next.getDate(), next.getTitle(), url);
					return link;
				}
			}
		}
		return null;
	}
	
	/*
	protected String computeRelativeUrl(Path fromPath, Path toPath) {
		
		fromPath.re
		
		while(fromPath.getNameCount() >= 2 && toPath.getNameCount() >= 2
				&& fromPath.getName(0).equals(toPath.getName(0))) {
			relativeInputPath = relativeInputPath.subpath(1, relativeInputPath.getNameCount());
			relativeOutputPath = relativeOutputPath.subpath(1, relativeOutputPath.getNameCount());
		}
		System.out.println("relativeInputPath=" + relativeInputPath);

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < relativeInputPath.getNameCount() - 1; i++) {
			sb.append("../");
		}
		sb.append(relativeOutputPath.toString().replace('\\', '/'));
		return sb.toString();
	}
	*/
}
