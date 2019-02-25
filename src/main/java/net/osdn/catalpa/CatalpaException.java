package net.osdn.catalpa;

public class CatalpaException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private String title;
	
	public CatalpaException(String title, String message, Throwable cause) {
		super("[" + title + "] " + message, cause);
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
}
