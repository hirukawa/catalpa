package net.osdn.catalpa.upload.sftp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import net.osdn.catalpa.ProgressObserver;

public class SftpUploader {
	
	private static final String UPLOAD_INDEX_FILE = ".upload.idx";
	
	private SftpConfig config;
	private JSch jsch;
	private Session session;
	private ChannelSftp channel;
	private ProgressObserver observer;
	private int progress;
	private int maxProgress;

	public SftpUploader(SftpConfig config) throws JSchException {
		this.config = config;
		
		jsch = new JSch();
		
		String prvkey = config.getPrivateKeyFilePath();
		String passphrase = config.getPassphrase();
		if(prvkey != null && passphrase != null) {
			jsch.addIdentity(prvkey, passphrase);
		} else if(prvkey != null) {
			jsch.addIdentity(prvkey);
		}
		
		session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
		session.setUserInfo(config.getUserInfo());
	}
	
	public void connect() throws JSchException {
		if(!session.isConnected()) {
			session.connect();
		}
		if(channel == null) {
			channel = (ChannelSftp)session.openChannel("sftp");
			channel.connect();
		}
	}
	
	public void disconnect() {
		if(channel != null) {
			channel.disconnect();
			channel = null;
		}
		if(session != null) {
			session.disconnect();
		}
	}
	
	public int upload(File localDirectory, ProgressObserver observer) throws SftpException, IOException {
		return upload(localDirectory, config.getPath(), observer);
	}
	
	public int upload(File localDirectory, String remoteDirectory, ProgressObserver observer) throws SftpException, IOException {
		this.observer = (observer != null) ? observer : ProgressObserver.EMPTY;
		this.observer.setProgress(0.0);
		this.observer.setText("アップロードの準備をしています…");
		
		int uploadCount = 0;
		System.out.println("localDirectory=" + localDirectory.getAbsolutePath());
		System.out.println("remoteDirectory=" + remoteDirectory);
		
		int localDirectoryLength = (localDirectory.getAbsolutePath() + "\\").length();
		
		Map<String, Long> index = getUploadIndex(remoteDirectory);
		
		class FileEntry {
			String localFile;
			String remoteFile;
			long   localHash;
		}
		List<FileEntry> entries = new ArrayList<FileEntry>();

		List<File> list = listLocalFiles(localDirectory);
		for(File file : list) {
			FileEntry entry = new FileEntry();
			entry.localFile = file.getAbsolutePath();
			entry.remoteFile = remoteDirectory + entry.localFile.substring(localDirectoryLength).replace('\\', '/');
			
			entry.localHash = (0x4000000000000000L | (file.length() << 32) | file.lastModified()) & 0x7FFFFFFFFFFFFFFFL;
			Long remoteHash = index.get(entry.remoteFile);

			if(remoteHash == null || remoteHash.longValue() != entry.localHash) {
				entries.add(entry);
			}
		}
		
		progress = 0;
		maxProgress = entries.size() + 1;
		
		for(FileEntry entry : entries) {
			observer.setProgress(++progress / (double)maxProgress);
			observer.setText(entry.remoteFile.substring(remoteDirectory.length()));
			
			System.out.println(entry.localFile + " -> " + entry.remoteFile);
			put(entry.localFile, entry.remoteFile);
			uploadCount++;
			index.put(entry.remoteFile, entry.localHash);
		}
		
		observer.setProgress(++progress / (double)maxProgress);
		observer.setText("アップロード管理用インデックスを更新しています…");
		putUploadIndex(remoteDirectory, index);
		
		return uploadCount;
	}
	
	protected void put(String localFile, String remoteFile) throws SftpException {
		try {
			channel.put(localFile, remoteFile);
		} catch (SftpException e) {
			if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
				int i = remoteFile.lastIndexOf('/');
				if(i > 0) {
					String remoteDir = remoteFile.substring(0, i);
					mkdirs(remoteDir);
				}
				channel.put(localFile, remoteFile);
			} else {
				throw e;
			}
		}
	}
	
	protected void mkdirs(String remoteDir) throws SftpException {
		for(;;) {
			try {
				channel.mkdir(remoteDir);
				return;
			} catch (SftpException e) {
				if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
					int i = remoteDir.lastIndexOf('/');
					if(i > 0) {
						mkdirs(remoteDir.substring(0, i));
						channel.mkdir(remoteDir);
						return;
					}
				}
				throw e;
			}
		}
	}
	
	protected Map<String, Long> getUploadIndex(String remoteDirectory) {
		Map<String, Long> index = new HashMap<String, Long>();
		
		try {
			
			InputStream in = channel.get(remoteDirectory + UPLOAD_INDEX_FILE);
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while((line = r.readLine()) != null) {
				if(line.length() >= 20) {
					String key = line.substring(20);
					Long value = Long.parseLong(line.substring(0, 19));
					index.put(key, value);
				}
			}
		} catch(SftpException e) {
			if(e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
				e.printStackTrace();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return index;
	}
	
	protected void putUploadIndex(String remoteDirectory, Map<String, Long> index) throws IOException, SftpException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		Writer w = new OutputStreamWriter(buf, StandardCharsets.UTF_8);
		for(Entry<String, Long> entry : index.entrySet()) {
			w.write(Long.toString(entry.getValue()));
			w.write(' ');
			w.write(entry.getKey());
			w.write('\r');
			w.write('\n');
		}
		w.close();
		
		InputStream src = new ByteArrayInputStream(buf.toByteArray());
		String dst = remoteDirectory + UPLOAD_INDEX_FILE;
		channel.put(src, dst);
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
