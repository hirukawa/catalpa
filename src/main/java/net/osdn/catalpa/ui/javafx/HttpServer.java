package net.osdn.catalpa.ui.javafx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class HttpServer extends NanoHTTPD {
	
	static {
		Logger.getLogger(NanoHTTPD.class.getName()).setLevel(Level.OFF);
	}

	private static final long READ_TIMEOUT = 3000;
	private static final String INDEX_FILE_NAME = "index.html";
	
	private static final String ERROR_HTML = 
		"<!DOCTYPE html><html lang=\"ja\"><body>\r\n" + 
		"${content}\r\n" +
		"<script type=\"text/javascript\">\r\n" +
		"	function waitForUpdate() {\r\n" +
		"		var xhr = new XMLHttpRequest();\r\n" +
		"		xhr.onload = function (e) {\r\n" +
		"			if (xhr.readyState === 4) {\r\n" +
		"				if (xhr.status === 200) {\r\n" +
		"					location.reload();\r\n" +
		"				}\r\n" +
		"				waitForUpdate();\r\n" +
		"			}\r\n" +
		"		};\r\n" +
		"		xhr.onerror = function (e) {\r\n" +
		"			waitForUpdate();\r\n" +
		"		};\r\n" +
		"		xhr.open(\"GET\", \"/wait-for-update\", true);\r\n" +
		"		xhr.send(null);\r\n" +
		"	}\r\n" +
		"	waitForUpdate();\r\n" +
		"</script>\r\n" +
		"</body></html>";
	
	private static final Map<String, String> HEADERS;
	static {
		HEADERS = new HashMap<String, String>();
		HEADERS.put("Cache-Control", "no-cache, no-store, must-revalidate");
		HEADERS.put("Pragma", "no-cache");
		HEADERS.put("Expires", "0");
	}
	
	private Object update = new Object();
	private Path documentRoot;
	
	public HttpServer(int port) {
		super("localhost", port);
	}
	
	public void setDocumentRoot(Path dir) {
		documentRoot = dir;
	}
	
	public Path getDocumentRoot() {
		return documentRoot;
	}
	
	public void update() {
		synchronized (update) {
			update.notifyAll();
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		try {
			
			String uri = session.getUri();
			if("/wait-for-update".equals(uri)) {
				synchronized (update) {
					update.wait();
				}
				Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", "updated");
				for(Entry<String, String> header : HEADERS.entrySet()) {
					response.addHeader(header.getKey(), header.getValue());
				}
				response.closeConnection(true);
				return response;
			}
			
			if(uri != null && uri.length() > 0 && uri.charAt(0) == '/') {
				uri = uri.substring(1);
			}
			Path path = getDocumentRoot().resolve(uri);
			if(Files.isDirectory(path)) {
				path = path.resolve(INDEX_FILE_NAME);
			}
			try {
				IStatus status = Response.Status.OK;
				String mimeType = getMimeTypeForFile(path.getFileName().toString());
				byte[] data = readAllBytes(path, READ_TIMEOUT);
				Response response = newFixedLengthResponse(status, mimeType, new ByteArrayInputStream(data), data.length);
				for(Entry<String, String> header : HEADERS.entrySet()) {
					response.addHeader(header.getKey(), header.getValue());
				}
				response.closeConnection(true);
				return response;
			} catch(NoSuchFileException e) {
				if(uri != null && uri.endsWith(".html")) {
					Response response = newFixedLengthResponse(Response.Status.TEMPORARY_REDIRECT, "text/plain", "Temporary Redirect");
					response.addHeader("Location", "/");
					response.closeConnection(true);
					return response;
				} else {
					Response response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
					for(Entry<String, String> header : HEADERS.entrySet()) {
						response.addHeader(header.getKey(), header.getValue());
					}
					response.closeConnection(true);
					return response;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			
			String uri = session.getUri();
			if(uri != null && (uri.equals("/") || uri.endsWith(".html"))) {
				Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
						"text/html", ERROR_HTML.replace("${content}", "Internal Server Error"));
				for(Entry<String, String> header : HEADERS.entrySet()) {
					response.addHeader(header.getKey(), header.getValue());
				}
				response.closeConnection(true);
				return response;
			} else {
				Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
				for(Entry<String, String> header : HEADERS.entrySet()) {
					response.addHeader(header.getKey(), header.getValue());
				}
				response.closeConnection(true);
				return response;
			}
		}
	}
	
	public static byte[] readAllBytes(Path path, long timeout) throws NoSuchFileException, IOException, InterruptedException {
		try(FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
			long s = System.currentTimeMillis();
			while(timeout == 0 || System.currentTimeMillis() - s < timeout) {
				try(FileLock lock = fc.tryLock(0L, Long.MAX_VALUE, true)) {
					if(lock != null) {
						FileTime lastModifiedTime = Files.getLastModifiedTime(path);
						byte[] bytes = Files.readAllBytes(path);
						if(lastModifiedTime.equals(Files.getLastModifiedTime(path))) {
							return bytes;
						}
					}
				} catch(OverlappingFileLockException e) {
					// ignore
				}
				Thread.sleep(16);
			}
		}
		throw new IOException("timeout: " + path);
	}
}
