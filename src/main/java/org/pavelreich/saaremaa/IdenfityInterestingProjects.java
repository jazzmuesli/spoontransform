package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * identify suitable projects in 50K-C_projects.tgz
 * @author preich
 *
 */
public class IdenfityInterestingProjects {

	private static boolean isSomethingInside(List<String> lines, String mask) throws IOException {
		long count = lines.stream().filter(s->s.toLowerCase().contains(mask)).count();
		return count > 0;
	}
	
	static String extractMetaData(String fileName) {
		boolean pom = false;
		int tests = 0;
		boolean jacoco = false;
		boolean gradle = false;
		boolean powermock = false;
		boolean mockito = false;
		boolean easymock = false;
		boolean junit = false;
		int classes = 0;
		try {
			ZipFile zf = new ZipFile(fileName);
			var files = zf.stream().collect(Collectors.toList());
			for (var x : files) {
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
				+ mockito + "," + easymock + "," + junit;
	}

	private static List<String> readLines(ZipFile zf, ZipEntry x) throws IOException {
		List<String> lines;
		InputStream is = zf.getInputStream(x);
		lines = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.toList());
		is.close();
		return lines;
	}


	public static void main(String[] args) throws IOException {
		var zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> p.toFile().toString().endsWith(".zip")).collect(Collectors.toList());

		FileWriter fw = new FileWriter("suitable-projects.csv");
		fw.write("filename,pom,gradle,tests,classes,jacoco,powermock,mockito,easymock,junit\n");
		var goodFiles = zipFiles.parallelStream().map(x -> {
			try {
				String md = extractMetaData(x.toString());
				fw.write(md+"\n");
				fw.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return x;
		}).collect(Collectors.toList());
		fw.close();

	}
}
