package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class DirectoryProject extends AbstractProject<PathProjectEntry> {

	private Path rootPath;

	public DirectoryProject(String fileName) {
		super(fileName);
		this.rootPath = Paths.get(fileName);
	}

	@Override
	public List<String> readLines(PathProjectEntry x) {
		List<String> lines;
		try {
			lines = Files.lines(Paths.get(x.getName())).collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		return lines;
	}

	@Override
	public List<PathProjectEntry> getFiles() {
		try {
			List<PathProjectEntry> files = java.nio.file.Files.walk(rootPath)
					.map(x->new PathProjectEntry(x)).collect(Collectors.toList());
			return files;
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}
