package net.osdn.catalpa.upload.smb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import jcifs.Config;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class SmbUploader {
	
	private static final String UPLOAD_INDEX_FILE = ".upload.idx";
	
	private SmbConfig config;
	
	public SmbUploader(SmbConfig config) {
		this.config = config;
	}
	
	public int upload(File localDirectory) throws IOException {
		return upload(localDirectory, config.getPath());
	}
	
	public int upload(File localDirectory, String remoteDirectory) throws IOException {
		int uploadCount = 0;

		Properties prop = new Properties();
		String domain = config.getDomain();
		if(domain != null && domain.trim().length() > 0) {
			prop.setProperty("jcifs.smb.client.domain", domain.trim());
		}
		String username = config.getUsername();
		if(username != null && username.trim().length() > 0) {
			prop.setProperty("jcifs.smb.client.username", username.trim());
		}
		String password = config.getPassword();
		if(password != null && password.trim().length() > 0) {
			prop.setProperty("jcifs.smb.client.password", password.trim());
		}
		Config.setProperties(prop);
		
		int localDirectoryLength = (localDirectory.getAbsolutePath() + "\\").length();
		
		Map<String, Long> index = getUploadIndex(remoteDirectory);
		
		List<File> list = listLocalFiles(localDirectory);
		for(File file : list) {
			String localFile = file.getAbsolutePath();
			String remoteFile = remoteDirectory + localFile.substring(localDirectoryLength).replace('\\', '/');
			
			long localHash = (0x4000000000000000L | (file.length() << 32) | file.lastModified()) & 0x7FFFFFFFFFFFFFFFL;
			Long remoteHash = index.get(remoteFile);
			
			if(remoteHash == null || remoteHash.longValue() != localHash) {
				put(localFile, remoteFile);
				uploadCount++;
				index.put(remoteFile, localHash);
			}
		}

		try {
			putUploadIndex(remoteDirectory, index);
		} catch(SmbException e) {
			e.printStackTrace();
		}
		
		return uploadCount;
	}
	
	protected void put(String localFile, String remoteFile) throws IOException {
		byte[] buf = new byte[65536];
		int size;
		
		InputStream in = null;
		OutputStream out = null;
		SmbFile smbRemoteFile = null;
		try {
			smbRemoteFile = new SmbFile("smb:" + remoteFile);
			try {
				smbRemoteFile.createNewFile();
			} catch(SmbException e) {
				if(e.getNtStatus() == 0xC000003A) {
					// NtStatus = 0xC000003A はファイルパスが存在しないときのエラーコードです。
					// このエラーの場合は mkdirs で上位パスの作成を試みてから再実行します。(ここではエラー出力しません)
					int i = remoteFile.lastIndexOf('/');
					if(i > 0) {
						String remoteDir = remoteFile.substring(0, i);
						SmbFile smbRemoteDir = new SmbFile("smb:" + remoteDir);
						smbRemoteDir.mkdirs();
					}
					smbRemoteFile.createNewFile();
				}
			}
			
			out = smbRemoteFile.getOutputStream();
			in = new FileInputStream(localFile);
			while((size = in.read(buf)) >= 0) {
				if(size > 0) {
					out.write(buf, 0, size);
				}
			}
		} finally {
			if(in != null) {
				try { in.close(); } catch(Exception e) {}
				in = null;
			}
			if(out != null) {
				try { out.close(); } catch(Exception e) {}
				out = null;
			}
		}
	}
	
	protected Map<String, Long> getUploadIndex(String remoteDirectory) {
		Map<String, Long> index = new HashMap<String, Long>();

		SmbFileInputStream in = null;
		BufferedReader r = null;
		
		try {
			in = new SmbFileInputStream("smb:" + remoteDirectory + UPLOAD_INDEX_FILE);
			r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while((line = r.readLine()) != null) {
				if(line.length() >= 20) {
					String key = line.substring(20);
					Long value = Long.parseLong(line.substring(0, 19));
					index.put(key, value);
				}
			}
		} catch(SmbException e) {
			//NtStatus == 0xC0000034 STATUS_OBJECT_NAME_NOT_FOUND は無視します。(エラー出力しません)
			if(e.getNtStatus() != 0xC0000034) {
				System.err.println("#NtStatus=" + String.format("0x%08X", e.getNtStatus()));
				e.printStackTrace();
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(r != null) {
				try { r.close(); } catch(Exception e) {}
				r = null;
			}
			if(in != null) {
				try { in.close(); } catch(Exception e) {}
				in = null;
			}
		}
		
		return index;
	}
	
	protected void putUploadIndex(String remoteDirectory, Map<String, Long> index) throws IOException {
		SmbFileOutputStream out = null;
		Writer w = null;
		
		try {
			out = new SmbFileOutputStream("smb:" + remoteDirectory + UPLOAD_INDEX_FILE);
			w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			for(Entry<String, Long> entry : index.entrySet()) {
				w.write(Long.toString(entry.getValue()));
				w.write(' ');
				w.write(entry.getKey());
				w.write('\r');
				w.write('\n');
			}
		} finally {
			if(w != null) {
				try { w.close(); } catch(Exception e) {}
				w = null;
			}
			if(out != null) {
				try { out.close(); } catch(Exception e) {}
				out = null;
			}
		}
	}
	
	protected List<File> listLocalFiles(File dir) {
		List<File> list = new ArrayList<File>();
		
		for(File child : dir.listFiles()) {
			if(child.isDirectory()) {
				list.addAll(listLocalFiles(child));
			} else {
				list.add(child);
			}
		}
		
		return list;
	}
}
