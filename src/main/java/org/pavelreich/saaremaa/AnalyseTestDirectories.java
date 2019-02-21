package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyseTestDirectories {

	private static final Logger LOG = LoggerFactory.getLogger(AnalyseTestDirectories.class);

	public static void main(String[] args) throws IOException {
		String startDir = args.length == 1 ? args[0] : ".";
		List<Path> dirs = java.nio.file.Files.walk(java.nio.file.Paths.get(startDir))
				.filter(p -> p.toFile().toString().endsWith("src/test")).collect(Collectors.toList());
		dirs.parallelStream().forEach(x -> processDirectory(x));
	}

	private static void processDirectory(Path path) {
		try {
			LOG.info("Processing directory " + path);
			TestFileProcessor.run(path.toFile().getAbsolutePath(), path.toFile().getAbsolutePath() + "/result.json");
		} catch (Exception e) {
			LOG.error("Failed to process " + path + " due to error: " + e.getMessage(), e);
		}
	}
}
