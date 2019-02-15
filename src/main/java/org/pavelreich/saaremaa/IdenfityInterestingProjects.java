package org.pavelreich.saaremaa;

import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * identify suitable projects in 50K-C_projects.tgz
 * @author preich
 *
 */
public class IdenfityInterestingProjects {

	static boolean goodEnough(String f) {
		try {
			var files = new ZipFile(f).stream().collect(Collectors.toList());
			boolean pom = false;
			boolean tests = false;
			for (var x : files) {
				if (x.getName().contains("pom.xml")) {
					pom = true;
				}
				if (x.getName().contains("Test.java")) {
					tests = true;
				}
			}
			return pom && tests;
		} catch (Exception e) {
			return false;
		}
	}

	public static void main(String[] args) throws IOException {
		var zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> p.toFile().toString().endsWith(".zip")).collect(Collectors.toList());

		FileWriter fw = new FileWriter("suitable-projects.txt");
		var goodFiles = zipFiles.parallelStream().filter(p -> goodEnough(p.toString())).map(x -> {
			try {
				fw.write(x+"\n");
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
