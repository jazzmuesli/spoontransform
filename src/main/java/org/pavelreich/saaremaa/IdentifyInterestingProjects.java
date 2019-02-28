package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * identify suitable projects in 50K-C_projects.tgz
 * 
 * java -classpath /home/preich/projects/spoontransform/target/experiments-1.0-SNAPSHOT-jar-with-dependencies.jar org.pavelreich.saaremaa.IdentifyInterestingProjects
 * @author preich
 *
 */
public class IdentifyInterestingProjects {

	private CSVReporter suitableCSVPrinter;
	private CSVReporter filesCSVPrinter;



	static class ProjectsCSVReporter extends CSVReporter {
		public ProjectsCSVReporter() throws IOException {
			super(new CSVPrinter(Files.newBufferedWriter(Paths.get("suitable-projects.csv")),
					CSVFormat.DEFAULT.withHeader("filename", "pom", "gradle", "tests", "classes", "jacoco", "powermock",
							"mockito", "easymock", "junit", "test_prod_classes").withDelimiter(';')));
		}
	}

	static class FilesCSVReporter extends CSVReporter {

		public FilesCSVReporter() throws IOException {
			super(new CSVPrinter(Files.newBufferedWriter(Paths.get("files.csv")), CSVFormat.DEFAULT
					.withHeader("zipFileName", "isDirectory", "fileName", "size", "time").withDelimiter(';')));
		}

	}

	public IdentifyInterestingProjects() throws IOException {
		this(new ProjectsCSVReporter(), new FilesCSVReporter());

	}

	public IdentifyInterestingProjects(CSVReporter suitableCSVPrinter, CSVReporter filesCSVPrinter) {
		this.suitableCSVPrinter = suitableCSVPrinter;
		this.filesCSVPrinter = filesCSVPrinter;
	}

	private static boolean isSomethingInside(List<String> lines, String mask) throws IOException {
		long count = lines.stream().filter(s -> s.toLowerCase().contains(mask)).count();
		return count > 0;
	}

	static List<ZipEntry> listFiles(String fileName) throws IOException {
		ZipFile zf = new ZipFile(fileName);
		List<ZipEntry> files = zf.stream().collect(Collectors.toList());
		zf.close();
		return files;
	}

	Object[] extractMetaData(String fileName) {
		boolean pom = false;
		int tests = 0;
		boolean jacoco = false;
		boolean gradle = false;
		boolean powermock = false;
		boolean mockito = false;
		boolean easymock = false;
		boolean junit = false;
		int classes = 0;
		int test_prod_classes = 0;
		try {
			AbstractProject project;
			if (fileName.endsWith(".zip")) {
				project = new ZipProject(fileName);
			} else {
				project = new DirectoryProject(fileName);
			}

			project.reportFilesInsideZip(filesCSVPrinter);
			test_prod_classes = project.countProdToTestClasses();
			List<ProjectEntry> files = project.getFiles();
			for (ProjectEntry x : files) {
				List<String> lines = null;
				if (x.getName().contains("build.gradle")) {
					gradle = true;
					lines = project.readLines(x);
				}
				if (x.getName().contains("pom.xml")) {
					pom = true;
					lines = project.readLines(x);
				}
				if (lines != null) {
					jacoco = isSomethingInside(lines, "jacoco");
					powermock = isSomethingInside(lines, "powermock");
					mockito = isSomethingInside(lines, "mockito");
					easymock = isSomethingInside(lines, "easymock");
					junit = isSomethingInside(lines, "junit");
				}
				if (x.getName().contains("Test.java")) {
					tests++;
				}
				if (x.getName().endsWith(".java")) {
					classes++;
				}
			}
			project.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Object[] { fileName, pom, gradle, tests, classes, jacoco, powermock, mockito, easymock, junit,
				test_prod_classes };
	}

	public static void main(String[] args) throws IOException {
		new IdentifyInterestingProjects().run();
	}

	public void run() throws IOException {
		List<Path> zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> isSuitableProject(p))
				.collect(Collectors.toList());

		zipFiles.parallelStream().forEach(x -> processPath(x));
		this.suitableCSVPrinter.close();

	}

	static final List<String> PROJECT_BUILD_FILES = Arrays.asList("pom.xml", "build.gradle", "build.xml", ".classpath",
			".project");

	protected boolean isSuitableProject(Path p) {
		if (p.toFile().toString().endsWith(".zip")) {
			return true;
		}
		if (PROJECT_BUILD_FILES.stream().anyMatch(f -> p.resolve(f).toFile().exists())) {
			return true;
		}
		return false;
	}

	void processPath(Path x) {
		Object[] md = extractMetaData(x.toString());
		this.suitableCSVPrinter.write(md);
		this.suitableCSVPrinter.flush();
	}
}
