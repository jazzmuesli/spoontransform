package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
			ZipFile zf = new ZipFile(f);
			var files = zf.stream().collect(Collectors.toList());
			boolean pom = false;
			boolean tests = false;
			for (var x : files) {
				if (x.getName().contains("pom.xml")) {
					InputStream is = zf.getInputStream(x);
					pom = isJacocoInside(is);
				}
				if (x.getName().contains("Test.java")) {
					tests = true;
				}
			}
			zf.close();
			return pom && tests;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean isJacocoInside(InputStream is) throws IOException {
		String pluginName = "jacoco-maven-plugin";
		//final String pluginName = "maven-shade-plugin";
		long count = new BufferedReader(new InputStreamReader(is)).lines().filter(s->s.contains(pluginName)).count();
		is.close();
		return count > 0;
	}

	public static void main(String[] args) throws IOException {
		var zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> p.toFile().toString().endsWith(".zip")).collect(Collectors.toList());

		FileWriter fw = new FileWriter("suitable-projects.txt1");
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
