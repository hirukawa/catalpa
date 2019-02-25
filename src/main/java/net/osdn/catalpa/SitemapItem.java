package net.osdn.catalpa;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class SitemapItem {
	
	public enum ChangeFreq {
		Always,
		Hourly,
		Daily,
		Weekly,
		Monthly,
		Yearly,
		Never
	}
	
	private String loc;
	private LocalDateTime lastmod;
	private ChangeFreq changefreq;
	private double priority;

	public SitemapItem(String loc, FileTime lastmod, ChangeFreq changefreq, double priority) {
		this(loc, lastmod.toMillis(), changefreq, priority);
	}
	
	public SitemapItem(String loc, long lastmod, ChangeFreq changefreq, double priority) {
		this(loc, LocalDateTime.ofInstant(Instant.ofEpochMilli(lastmod - (lastmod % 1000)), TimeZone.getDefault().toZoneId()), changefreq, priority);
	}
	
	public SitemapItem(String loc, LocalDateTime lastmod, ChangeFreq changefreq, double priority) {
		this.loc = loc;
		this.lastmod = lastmod;
		this.changefreq = changefreq;
		this.priority = priority;
	}
	
	public String getLoc() {
		return this.loc;
	}
	
	public LocalDateTime getLastmod() {
		return this.lastmod;
	}
	
	public String getLastmod_iso8601() {
		return this.lastmod.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);  // DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
	}
	
	public String getChangefreq() {
		return this.changefreq.name().toLowerCase();
	}
	
	public String getPriority() {
		return String.format("%.1f", this.priority);
	}
}
