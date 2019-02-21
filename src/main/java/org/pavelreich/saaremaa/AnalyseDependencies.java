package org.pavelreich.saaremaa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.CtModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AnalyseDependencies {
	static final Logger LOG = LoggerFactory.getLogger(AnalyseDependencies.class);



	public Map<String, ObjectCreationOccurence> run(String path) throws FileNotFoundException {
		Launcher launcher = new Launcher();
		SpoonResource resource = SpoonResourceHelper.createResource(new File(path));
		launcher.addInputResource(resource);
		launcher.buildModel();

		CtModel model = launcher.getModel();
		final Map<String, ObjectCreationOccurence> objectsCreated = new HashMap();
		model.processWith(new MockProcessor(objectsCreated));
		model.processWith(new AnnotatedMockProcessor(objectsCreated));
		model.processWith(new ObjectInstantiationProcessor(objectsCreated));
		return objectsCreated;
	}


	public static void main(String[] args) {
		runMockAnalysis(args);
	}


	private static void runMockAnalysis(String[] args) {
		try (FileWriter fw = new FileWriter("output.csv")) {
			if (args.length != 1) {
				throw new IllegalArgumentException("Usage: directory");
			}
			fWrite(fw,"objectName;objectType;objectClass;file;position;rootPath\n");
			Path rootPath = Paths.get(args[0]);
			Files.walk(rootPath)
					.filter(f -> isRelevantFile(f))
					.parallel()
					.forEach(path -> processPath(fw, path, rootPath));

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static boolean isRelevantFile(Path f) {
		if (Boolean.valueOf(System.getProperty("scan.zip","false"))) {
			return f.toString().endsWith(".zip");
		}
		return f.toFile().isDirectory() & f.toFile().getAbsolutePath().endsWith("src/test");
	}

	private static void processPath(FileWriter fw, Path path, Path rootPath) {
		LOG.info("path: " + path);

		try {
			Map<String, ObjectCreationOccurence> mocks = new AnalyseDependencies().run(path.toFile().getAbsolutePath());
			mocks.entrySet().forEach(e -> {
				try {
					ObjectCreationOccurence mockOc = e.getValue();
					fWrite(fw,e.getKey() + ";" + mockOc.toCSV() + ";" + path.toFile().getAbsolutePath() + "\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			GroumRunner groumRunner = new GroumRunner();
			if (!mocks.isEmpty() && Boolean.valueOf(System.getProperty("enable.groom","false"))) {
				mocks.values().stream().map(x -> x.getAbsolutePath()).filter(p -> p != null).forEach(x -> groumRunner.runGroom(x));
			}
			fwFlush(fw);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private synchronized static void fwFlush(FileWriter fw) throws IOException {
		fw.flush();
	}

	private synchronized static void fWrite(FileWriter fw, String string) throws IOException {
		fw.write(string);
	}

}
