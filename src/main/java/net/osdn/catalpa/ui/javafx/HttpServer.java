package net.osdn.catalpa.ui.javafx;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class HttpServer extends NanoHTTPD {
	
	public static void main(String[] args) throws Exception {
		HttpServer server = new HttpServer(4000);
		server.setDocumentRoot(Paths.get("C:\\Users\\hiruk\\AppData\\Local\\Temp\\catalpa\\htdocs"));
		server.start();
		
		Thread.sleep(120 * 1000);
	}
	
	static {
		Logger.getLogger(NanoHTTPD.class.getName()).setLevel(Level.OFF);
	}

	private static final String[] INDEX_FILE_NAMES = new String[] {
			"index.html",
			"index.htm"
	};
	
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
			if(Files.exists(path)) {
				if(Files.isDirectory(path)) {
					
					for(String i : INDEX_FILE_NAMES) {
						Path index = path.resolve(i);
						if(Files.exists(index) && !Files.isDirectory(index)) {
							path = index;
							break;
						}
					}
				}
				if(!Files.isDirectory(path)) {
					IStatus status = Response.Status.OK;
					String mimeType = getMimeTypeForFile(path.getFileName().toString());
					InputStream data = Files.newInputStream(path);
					long totalBytes = Files.size(path);
					Response response = newFixedLengthResponse(status, mimeType, data, totalBytes);
					for(Entry<String, String> header : HEADERS.entrySet()) {
						response.addHeader(header.getKey(), header.getValue());
					}
					response.closeConnection(true);
					return response;
				}
			}
			Response response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
			for(Entry<String, String> header : HEADERS.entrySet()) {
				response.addHeader(header.getKey(), header.getValue());
			}
			response.closeConnection(true);
			return response;
			
		} catch(Exception e) {
			e.printStackTrace();

			Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
			for(Entry<String, String> header : HEADERS.entrySet()) {
				response.addHeader(header.getKey(), header.getValue());
			}
			response.closeConnection(true);
			return response;
		}
	}
	
}
