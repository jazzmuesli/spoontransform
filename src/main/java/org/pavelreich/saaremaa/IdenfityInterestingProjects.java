package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * identify suitable projects in 50K-C_projects.tgz
 * 
 * @author preich
 *
 */
public class IdenfityInterestingProjects {

	private FileWriter fw;
	private FileWriter listFw;

	public IdenfityInterestingProjects(FileWriter fw, FileWriter listFw) {
		this.fw = fw;
		this.listFw = listFw;
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

	int countProdToTestClasses(List<ZipEntry> files) throws IOException {
		List<String> fileNames = files.stream().filter(p -> p.toString().endsWith(".java"))
				.map(p -> Paths.get(p.getName()).toFile().getName()).collect(Collectors.toList());
		Set<String> testClasses = fileNames.stream().filter(p -> p.endsWith("Test.java"))
				.map(x -> x.replaceAll("Test.java", "")).collect(Collectors.toSet());
		List<String> matchedProdClasses = fileNames.stream().filter(p -> !p.endsWith("Test.java"))
				.map(x -> x.replaceAll(".java", "")).filter(p -> testClasses.contains(p)).collect(Collectors.toList());
		return matchedProdClasses.size();
	}

	String extractMetaData(String fileName) {
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
			ZipFile zf = new ZipFile(fileName);
			List<ZipEntry> files = zf.stream().collect(Collectors.toList());
			reportFilesInsideZip(fileName, files);
			test_prod_classes = countProdToTestClasses(files);
			for (ZipEntry x : files) {
				List<String> lines = null;
				if (x.getName().contains("build.gradle")) {
					gradle = true;
					lines = readLines(zf, x);
				}
				if (x.getName().contains("pom.xml")) {
					pom = true;
					lines = readLines(zf, x);
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
			zf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileName + "," + pom + "," + gradle + "," + tests + "," + classes + "," + jacoco + "," + powermock + ","
				+ mockito + "," + easymock + "," + junit + "," + test_prod_classes;
	}

	private void reportFilesInsideZip(String fileName, List<ZipEntry> files) {
		files.stream().map(x->fileName+";"+x.isDirectory()+";"+x.getName()+";"+x.getSize()+";"+x.getTime()).forEach(s->{
			try {
				listFw.write(s+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	private static List<String> readLines(ZipFile zf, ZipEntry x) throws IOException {
		InputStream is = zf.getInputStream(x);
		List<String> lines = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.toList());
		is.close();
		return lines;
	}

	public static void main(String[] args) throws IOException {

		FileWriter fw = new FileWriter("suitable-projects.csv");
		FileWriter listFw = new FileWriter("files.csv");
		new IdenfityInterestingProjects(fw, listFw).run();

		fw.close();
		listFw.close();
	}

	private void run() throws IOException {
		fw.write("filename,pom,gradle,tests,classes,jacoco,powermock,mockito,easymock,junit,test_prod_classes\n");
		listFw.write("zipFileName;isDirectory;fileName;size;time\n");
		List<Path> zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get("."))
				.filter(p -> p.toFile().toString().endsWith(".zip")).collect(Collectors.toList());

		List<Path> goodFiles = zipFiles.parallelStream().map(x -> {
			try {
				String md = extractMetaData(x.toString());
				fw.write(md + "\n");
				fw.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return x;
		}).collect(Collectors.toList());

	}
}
