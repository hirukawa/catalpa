package net.osdn.catalpa.addon.blog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

public class Post {
	
	private Path           path;
	private String         url;
	private LocalDate      date;
	private String         title;
	private Set<Category>  categories;
	private String         leading;
	private String         relativeUrlPrefix;
	private String         thumbnail;
	
	protected Post(Path path, String url, LocalDate date, String title, Set<Category> categories, String leading) {
		this.path = path;
		this.url = url;
		this.date = date;
		this.title = title;
		this.categories = categories;
		this.leading = leading;
	}
	
	public Path getPath() {
		return path;
	}
	
	public String getUrl() {
		return url;
	}
	
	public LocalDate getDate() {
		return date;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Set<Category> getCategories() {
		return categories;
	}
	
	public String getLeading() {
		return leading;
	}
	
	public void setRelativeUrlPrefix(String relativeUrlPrefix) {
		this.relativeUrlPrefix = relativeUrlPrefix;
	}
	
	public String getRelativeUrlPrefix() {
		return relativeUrlPrefix != null ? relativeUrlPrefix : "";
	}
	
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public String getThumbnail() {
		return thumbnail;
	}

	protected static final String[] APPLICABLE_EXTENSIONS = new String[] {
		".markdown",
		".markdown.txt",
		".md",
		".md.txt"
	};
	
	public static boolean isApplicable(Path path) {
		if(!Files.isDirectory(path)) {
			String s = path.getFileName().toString().toLowerCase();
			for(String ext : APPLICABLE_EXTENSIONS) {
				if(s.endsWith(ext)) {
					return true;
				}
			}
		}
		return false;
	}
}
