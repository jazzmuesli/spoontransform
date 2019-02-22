package org.pavelreich.saaremaa;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * convert result.json to CSVs
 * 
 * @author preich
 *
 */
public class ResultToCSV {

	private static final Logger LOG = LoggerFactory.getLogger(ResultToCSV.class);

	public static void main(String[] args) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get("results.csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer,
				CSVFormat.DEFAULT
						.withHeader("fileName", "testClassName", "methodType", "methodName", "LOC", "statements", "annotations")
						.withDelimiter(';'));

		List<Path> files = java.nio.file.Files.walk(java.nio.file.Paths.get("."))
				.filter(p -> p.toFile().toString().endsWith("result.json")).collect(Collectors.toList());
		files.parallelStream().forEach(file -> processFile(csvPrinter, file));

		csvPrinter.close();
	}

	private static void processFile(CSVPrinter csvPrinter, Path file)  {
		String jsonStr;
		try {
			LOG.info("Processing file " + file);
			jsonStr = new String(Files.readAllBytes(file));
			Type type = new TypeToken<List>() {
			}.getType();
			List<Map> json = new Gson().fromJson(jsonStr, type);
			json.stream().forEach(x -> processTest(x, file, csvPrinter));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private synchronized static void processTest(Map testMap, Path file, CSVPrinter csvPrinter) {
		Object testClassName = testMap.get("simpleName");
		try {
			Path fileName = file.getFileName();
			List<Map> setupMethods = (List) testMap.getOrDefault("setupMethods", Collections.emptyList());
			List<Map> testMethods = (List) testMap.getOrDefault("testMethods", Collections.emptyList());
			setupMethods.forEach(setupMethod -> {
				try {
					
					csvPrinter.printRecord(fileName, testClassName, "setup", setupMethod.get("simpleName"),
							setupMethod.get("LOC"), setupMethod.get("statementCount"), setupMethod.get("annotations"));
				} catch (IOException e) {
					LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
				}
			});
			testMethods.forEach(setupMethod -> {
				try {
					csvPrinter.printRecord(fileName, testClassName, "test", setupMethod.get("simpleName"), setupMethod.get("LOC"),
							setupMethod.get("statementCount"), setupMethod.get("annotations"));
				} catch (IOException e) {
					LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
				}
			});

			csvPrinter.printRecord(fileName, testClassName, "class", "", 0, 0, testMap.get("annotationsMap"));
			csvPrinter.flush();
		} catch (Exception e) {
			LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
		}
	}
}
