package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipProject extends AbstractProject<ZipProjectEntry> {

	private List<? extends ZipEntry> files;
	private ZipFile zf;

	public ZipProject(String fileName) {
		super(fileName);
		try {
			this.zf = new ZipFile(fileName);
			this.files = zf.stream().collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	public List<String> readLines(ZipProjectEntry x) {
		try {
			return readLines(zf, x.zipEntry);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private static List<String> readLines(ZipFile zf, ZipEntry x) throws IOException {
		InputStream is = zf.getInputStream(x);
		List<String> lines = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.toList());
		is.close();
		return lines;
	}

	@Override
	public void close() {
		try {
			this.zf.close();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	public List<ZipProjectEntry> getFiles() {
		return files.stream().map(x -> new ZipProjectEntry(x)).collect(Collectors.toList());
	}
}
