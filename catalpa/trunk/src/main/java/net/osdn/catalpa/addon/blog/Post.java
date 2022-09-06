package net.osdn.catalpa.addon.blog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
	private String         image;
	private boolean        isMore;
	private boolean        isDraft;

	private List<Post>     related;
	
	protected Post(Path path, String url, LocalDate date, String title, Set<Category> categories, String leading, boolean isMore) {
		this.path = path;
		this.url = url;
		this.date = date;
		this.title = title;
		this.categories = categories;
		this.leading = leading;
		this.isMore = isMore;
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

	/** 記事のカテゴリーから決定した色相を返します。
	 *
	 * @return 色相（0～359）
	 */
	public int getHue() {
		int hue = 0;
		int influence = 1;
		for(Category category : categories) {
			hue += (category.getHue() / influence);
			influence++;
		}
		return hue % 360;
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
	
	public void setImage(String image) {
		this.image = image;
	}
	
	public String getImage() {
		return image;
	}

	public boolean isMore() {
		return isMore;
	}

	public void setDraft(boolean isDraft) {
		this.isDraft = isDraft;
	}
	
	public boolean isDraft() {
		return isDraft;
	}

	public List<Post> getRelated() {
		return related != null ? related : Collections.emptyList();
	}

	/* package private */ void setRelated(List<Post> related) {
		this.related = related;
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
