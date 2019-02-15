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

	private static boolean isJacocoInside(InputStream is) throws IOException {
		String pluginName = "jacoco-maven-plugin";
		//final String pluginName = "maven-shade-plugin";
		long count = new BufferedReader(new InputStreamReader(is)).lines().filter(s->s.contains(pluginName)).count();
		is.close();
		return count > 0;
	}
	
	static String extractMetaData(String fileName) {
		boolean pom = false;
		int tests = 0;
		boolean jacoco = false;
		boolean gradle = false;
		int classes = 0;
		try {
			ZipFile zf = new ZipFile(fileName);
			var files = zf.stream().collect(Collectors.toList());
			for (var x : files) {
				if (x.getName().contains("build.gradle")) {
					gradle = true;
				}
				if (x.getName().contains("pom.xml")) {
					pom = true;
					InputStream is = zf.getInputStream(x);
					jacoco = isJacocoInside(is);
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
		return fileName + "," + pom + "," + gradle + "," + tests + "," + classes + "," + jacoco;
	}


	public static void main(String[] args) throws IOException {
		var zipFiles = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> p.toFile().toString().endsWith(".zip")).collect(Collectors.toList());

		FileWriter fw = new FileWriter("suitable-projects.csv");
		fw.write("filename,pom,gradle,tests,classes,jacoco\n");
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
