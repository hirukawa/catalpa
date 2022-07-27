package net.osdn.catalpa.addon.blog;

import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Category {
	
	private String id;
	private String name;
	private List<Post> posts = new ArrayList<Post>();
	private LocalDate date;
	
	protected Category(String name) {
		this.name = name;
	}
	
	public String getUrl() {
		return "category/" + id + "/index.html";
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	/** カテゴリー名から決定した色相を返します。
	 *
	 * @return 色相（0～359）
	 */
	public int getHue() {
		return (name.hashCode() & 0x7FFF) % 360;
	}
	
	public void add(Post post) {
		date = null;
		posts.add(post);
	}
	
	public List<Post> getPosts() {
		return posts;
	}
	
	public LocalDate getDate() {
		if(date == null) {
			LocalDate last = null;
			for(Post post : posts) {
				if(last == null || post.getDate().isAfter(last)) {
					last = post.getDate();
				}
			}
			date = last;
		}
		return date;
	}
}
