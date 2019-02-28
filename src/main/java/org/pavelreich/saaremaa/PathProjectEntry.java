package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Path-based ProjectEntry, can represent a .java file or . directory.
 *
 */
public class PathProjectEntry extends ProjectEntry {

	private Path path;
	private File file;

	public PathProjectEntry(Path x) {
		this.path = x;
		this.file = path.toFile();
	}

	@Override
	public String getName() {
		try {
			return this.file.getCanonicalPath();
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	@Override
	protected boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	protected long getSize() {
		return file.length();
	}

	@Override
	protected long getTime() {
		return file.lastModified();
	}

}
