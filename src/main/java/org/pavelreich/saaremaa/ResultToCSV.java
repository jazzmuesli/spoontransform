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
				CSVFormat.DEFAULT.withHeader("fileName", "testClassName", "methodType", "methodName", "LOC",
						"statements", "annotations", "mockName", "mockType", "mockClass").withDelimiter(';'));

		List<Path> files = java.nio.file.Files.walk(java.nio.file.Paths.get("."))
				.filter(p -> p.toFile().toString().endsWith("result.json")).collect(Collectors.toList());
		files.parallelStream().forEach(file -> processFile(csvPrinter, file));

		csvPrinter.close();
	}

	private static void processFile(CSVPrinter csvPrinter, Path file) {
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
			Path fileName = file.toAbsolutePath();
			List<Map> setupMethods = (List) testMap.getOrDefault("setupMethods", Collections.emptyList());
			List<Map> testMethods = (List) testMap.getOrDefault("testMethods", Collections.emptyList());
			List<Map> mockFields = (List) testMap.getOrDefault("mockFields", Collections.emptyList());
			setupMethods.forEach(setupMethod -> {
				try {
					csvPrinter.printRecord(fileName, testClassName, "setup", setupMethod.get("simpleName"),
							setupMethod.get("LOC"), setupMethod.get("statementCount"), setupMethod.get("annotations"),"","","");
					List<Map> mocks = (List<Map>) setupMethod.getOrDefault("mocks", Collections.emptyList());
					processMocks(csvPrinter, fileName, testClassName, setupMethod, mocks, "setup");
				} catch (IOException e) {
					LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
				}
			});
			testMethods.forEach(testMethod -> {
				try {
					csvPrinter.printRecord(fileName, testClassName, "test", testMethod.get("simpleName"),
							testMethod.get("LOC"), testMethod.get("statementCount"), testMethod.get("annotations"),"","","");
					List<Map> mocks = (List<Map>) testMethod.getOrDefault("mocks", Collections.emptyList());
					processMocks(csvPrinter, fileName, testClassName, testMethod, mocks, "test");

				} catch (IOException e) {
					LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
				}
			});
			mockFields.forEach(mockField -> {
				try {
					csvPrinter.printRecord(fileName, testClassName, "mockField", "class", 0,0,
							mockField.get("annotations"), mockField.get("name"), mockField.get("type"), mockField.get("class"));
				} catch (IOException e) {
					LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
				}
			});

			csvPrinter.printRecord(fileName, testClassName, "class", "", 0, 0, testMap.get("annotationsMap"),"","","");
			csvPrinter.flush();
		} catch (Exception e) {
			LOG.error("Failed to handle " + testMap + " due to error: " + e.getMessage(), e);
		}
	}

	private static void processMocks(CSVPrinter csvPrinter, Path fileName, Object testClassName, Map setupMethod,
			List<Map> mocks, String prefix) {
		mocks.forEach(mock -> {
			try {
				csvPrinter.printRecord(fileName, testClassName, prefix + "Mock", setupMethod.get("simpleName"), 0, 0,
						"", mock.get("name"), mock.get("type"), mock.get("class"));
			} catch (Exception e) {
				LOG.error("Failed to handle " + setupMethod + " due to error: " + e.getMessage(), e);
			}
		});

	}

}
