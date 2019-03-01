package net.osdn.catalpa.addon.blog;

import java.time.LocalDate;

public class Link {

	private LocalDate date;
	private String title;
	private String url;
	
	public Link(String title, String url) {
		this.title = title;
		this.url = url;
	}
	
	public Link(LocalDate date, String title, String url) {
		this.date = date;
		this.title = title;
		this.url = url;
	}
	
	public LocalDate getDate() {
		return date;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getUrl() {
		return url;
	}
}
