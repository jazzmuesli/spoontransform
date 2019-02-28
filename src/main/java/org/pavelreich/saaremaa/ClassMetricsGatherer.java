package org.pavelreich.saaremaa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;

public class ClassMetricsGatherer {

	private static final Logger LOG = LoggerFactory.getLogger(ClassMetricsGatherer.class);

	static final List<JavaClassMetricKey> metrics = Arrays.asList(JavaClassMetricKey.values());

	static class MetricsCSVReporter extends CSVReporter {

		public MetricsCSVReporter() throws IOException {
			super(new CSVPrinter(Files.newBufferedWriter(Paths.get("class-metrics.csv")),
					CSVFormat.DEFAULT.withHeader(getHeaders()).withDelimiter(';')));
		}

	}

	static String[] getHeaders() {
		List<String> headers = new ArrayList(Arrays.asList("fileName", "className"));
		headers.addAll(metrics.stream().map(x -> x.name()).collect(Collectors.toList()));
		return headers.toArray(new String[0]);
	}

	public static void main(String[] args) throws IOException {
		List<Path> files = java.nio.file.Files.walk(java.nio.file.Paths.get(".")).filter(p -> p.toFile().getName().endsWith(".java"))
				.collect(Collectors.toList());

		String fileName = "src/main/java/org/pavelreich/saaremaa/DirectoryProject.java";
		CSVReporter reporter = new MetricsCSVReporter();
		files.parallelStream().forEach(f -> reportMetrics(f.toFile().getAbsolutePath(), reporter));
		reporter.close();

	}

	public static void reportMetrics(String fileName, CSVReporter reporter) {
		PMDConfiguration configuration = new PMDConfiguration();
		configuration.setReportFormat("xml");
//		configuration.setInputPaths("src/main/java");
		LanguageVersion version = new JavaLanguageModule().getVersion("1.8");
		configuration.setDefaultLanguageVersion(version);
		File sourceCodeFile = new File(fileName);
		String filename = sourceCodeFile.getAbsolutePath();
		try (InputStream sourceCode = new FileInputStream(sourceCodeFile)) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceCode))) {
				Parser parser = PMD.parserFor(version, configuration);
				ASTCompilationUnit compilationUnit = (ASTCompilationUnit) parser.parse(filename, reader);

				List<ASTAnyTypeDeclaration> astClassOrInterfaceDeclarations = compilationUnit
						.findDescendantsOfType(ASTAnyTypeDeclaration.class);
				astClassOrInterfaceDeclarations.forEach(declaration -> reportMetrics(fileName, reporter, declaration));

			}
		} catch (Exception e) {
			LOG.error("Can't report metrics for " + fileName + " due to " + e.getMessage(), e);
		}
		reporter.flush();
	}

	private static void reportMetrics(String fileName, CSVReporter reporter, ASTAnyTypeDeclaration declaration) {
		List<String> values = new ArrayList(Arrays.asList(fileName, declaration.getImage()));
		List<String> metricValues = metrics.stream().map(m -> {
			try {
				return String.valueOf(JavaMetrics.get(m, declaration));
			} catch (Exception e) {
				LOG.error("Can't get metric " + m + " in declaration " + declaration.getImage() + " file " + fileName + " due to "
						+ e.getMessage());
				return String.valueOf(Double.NaN);
			}
		}).collect(Collectors.toList());
		values.addAll(metricValues);
		reporter.write(values.toArray(new String[0]));
	}
}
