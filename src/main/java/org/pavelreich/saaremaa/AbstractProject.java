package org.pavelreich.saaremaa;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractProject<E extends ProjectEntry> {

	protected String fileName;

	public AbstractProject(String fileName) {
		this.fileName = fileName;
	}

	public void close() {

	}

	public abstract List<String> readLines(E x);

	public void reportFilesInsideZip(CSVReporter filesCSVPrinter) {
		getFiles().stream().forEach(x -> {
			filesCSVPrinter.write(this.fileName, x.isDirectory(), x.getName(), x.getSize(), x.getTime());
		});
	}

	public int countProdToTestClasses() {
		List<? extends ProjectEntry> files = getFiles();
		List<String> fileNames = files.stream().filter(p -> p.getName().endsWith(".java"))
				.map(p -> Paths.get(p.getName()).toFile().getName()).collect(Collectors.toList());
		Set<String> testClasses = fileNames.stream().filter(p -> p.endsWith("Test.java"))
				.map(x -> x.replaceAll("Test.java", "")).collect(Collectors.toSet());
		List<String> prodClasses = fileNames.stream().filter(p -> !p.endsWith("Test.java"))
				.map(x -> x.replaceAll(".java", "")).collect(Collectors.toList());
		List<String> matchedProdClasses = prodClasses.stream().filter(p -> testClasses.contains(p))
				.collect(Collectors.toList());
		return matchedProdClasses.size();
	}

	public abstract List<? extends ProjectEntry> getFiles();

}
