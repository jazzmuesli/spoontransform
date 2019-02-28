package org.pavelreich.saaremaa;

import java.util.zip.ZipEntry;

public class ZipProjectEntry extends ProjectEntry {

	ZipEntry zipEntry;

	public ZipProjectEntry(ZipEntry x) {
		this.zipEntry = x;
	}

	@Override
	public String getName() {
		return zipEntry.getName();
	}

	public boolean isDirectory() {
		return zipEntry.isDirectory();
	}

	@Override
	protected long getSize() {
		return zipEntry.getSize();
	}

	@Override
	protected long getTime() {
		return zipEntry.getTime();
	}

}
