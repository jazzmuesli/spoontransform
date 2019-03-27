package org.pavelreich.saaremaa;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.*;

import com.google.common.base.Functions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassNameAnalysis {

	static ConcurrentMap<String, Integer> countStrings(Collection<String> input) {
		ConcurrentMap<String, Integer> map = new ConcurrentHashMap();
		input.stream().forEach(x -> map.compute(x, (key, oldValue) -> ((oldValue == null) ? 1 : oldValue + 1)));
		return map;
	}

	private static final Logger LOG = LoggerFactory.getLogger(ClassNameAnalysis.class);

	public static List<List<String>> ngrams(int n, List<String> str) {
	    List<List<String>> ngrams = new ArrayList<List<String>>();
	    for (int i = 0; i < str.size() - n + 1; i++)
	        ngrams.add(str.subList(i, i + n));
	    return ngrams;
	}
	
	public static void main(String[] args) throws IOException {
		/*
		Set<ClassName> classNames = new HashSet();
		classNames.add(new ClassName("java.util.Map","project1"));
		classNames.add(new ClassName("java.util.LinkedHashMap","project2"));
		classNames.add(new ClassName("java.util.HashMap","project2"));
		classNames.add(new ClassName("java.util.concurrent.ConcurrentHashMap","project2"));
		classNames.add(new ClassName("java.util.concurrent.ConcurrentHashMapTest","project2"));
		List<String> xs = classNames.stream().map(x->x.className).collect(Collectors.toList());
		for (ClassName x : classNames) {
			LOG.info("ret: " + extractNGrams(x) + " for " + x.className);	
		}
		*/
		
		run(args[0]);
	}

	private static List<String> extractNGrams(ClassName last) {
		List<ClassNamePart> parts = extractParts(last, 1).collect(Collectors.toList());
		if (parts.size() == 1) {
			return Arrays.asList(parts.get(0).part);
		}
		List<List<String>> ret = new ArrayList();
		for (int i=parts.size()-1; i>0; i--) {
			ret.addAll(ngrams(i, parts.stream().map(x->x.part).collect(Collectors.toList())));
		}
		
		return ret.stream().map(x -> x.stream().collect(Collectors.joining(""))).collect(Collectors.toList());
	}
	private static void run(String dir) throws IOException {
		CSVParser parser = CSVParser.parse(new File(dir+"class-metrics.csv"),
				Charset.defaultCharset(), CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		Set<ClassName> classNames = new HashSet();
		parser.forEach(x -> classNames.add(new ClassName(x.get("class"), x.get("file"))));
		/*
		classNames.add(new ClassName("java.util.Map","project1"));
		classNames.add(new ClassName("java.util.Map","project2"));
		classNames.add(new ClassName("java.util.HashMap","project2"));
		classNames.add(new ClassName("java.util.concurrent.Map","project2"));
		*/
		Map<String, Long> classNameByProjectCount = classNames.parallelStream().map(x -> x.className).collect(table());
		LOG.info("classNames: " + classNames.size());
		LOG.info("classNameByProjectCount: " + classNameByProjectCount.size());
		Map<String, Long> plainClassNames = classNames.parallelStream().map(x -> extractPlainName(x.className))
				.collect(table());
		LOG.info("plainClassNames: " + plainClassNames.size());
		List<ClassNamePart> classNamePartsStream = classNames.parallelStream()
				.flatMap(s -> extractParts(s, classNameByProjectCount.get(s.className))).collect(Collectors.toList());
		Map<ClassNamePart, Long> classNameParts4 = classNamePartsStream.stream().collect(table());
		Map<String, Long> classNameParts1 = classNamePartsStream.parallelStream().map(x->x.part).collect(table());
		LOG.info("classNameParts4: " + classNameParts4.size());
		CSVReporter csvReporter4 = new CSVReporter(
				new CSVPrinter(Files.newBufferedWriter(Paths.get("class-name-parts4.csv")),
						CSVFormat.DEFAULT.withHeader(new String[] { "part", "plainClassName","className","projects","count" }).withDelimiter(';')));
		classNameParts4.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEach(x -> csvReporter4.write(x.getKey().part, x.getKey().plainClassName, x.getKey().className, x.getKey().projects, x.getValue()));
		csvReporter4.close();
		CSVReporter csvReporter1 = new CSVReporter(
				new CSVPrinter(Files.newBufferedWriter(Paths.get("class-name-parts1.csv")),
						CSVFormat.DEFAULT.withHeader(new String[] { "part", "count" }).withDelimiter(';')));
		classNameParts1.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEach(x -> csvReporter1.write(x.getKey(), x.getValue()));
		csvReporter1.close();
		Map<String, Long> classNgrams = classNames.parallelStream().map(x -> extractNGrams(x)).flatMap(List::stream).collect(table());
		CSVReporter csvReporter2 = new CSVReporter(
				new CSVPrinter(Files.newBufferedWriter(Paths.get("class-name-parts2.csv")),
						CSVFormat.DEFAULT.withHeader(new String[] { "ngram", "words", "count" }).withDelimiter(';')));
		classNgrams.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEach(x -> csvReporter2.write(x.getKey(), extractParts(x.getKey()).length, x.getValue()));
		csvReporter2.close();
		
		LOG.info("Finished");
	}
	private static Stream<ClassNamePart> extractParts(ClassName className, long projects) {
		String plainClassName = className.getPlainName();
		return Stream.of(extractParts(plainClassName)).map(x -> new ClassNamePart(x, plainClassName, className.className, projects));
	}

	private static String[] extractParts(String plainClassName) {
		return plainClassName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	}

	static class ClassName {
		private String className;
		private String project;

		ClassName(String className, String file) {
			this.className = className;
			this.project = file.replaceAll(".*?\\./([^/]+/[^/]+).*", "$1");
		}

		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
			result = prime * result + ((project == null) ? 0 : project.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClassName other = (ClassName) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			if (project == null) {
				if (other.project != null)
					return false;
			} else if (!project.equals(other.project))
				return false;
			return true;
		}


		public String getPlainName() {
			return extractPlainName(className);
		}


	}
	private static String extractPlainName(String className) {
		return className.replaceAll(".*?\\.([^\\.]+)$", "$1");
	}
	static class ClassNamePart {
		private String part;
		private String plainClassName;
		private String className;
		private long projects;

		ClassNamePart(String part, String plainClassName, String className, long projects) {
			this.part = part;
			this.plainClassName = plainClassName;
			this.className = className;
			this.projects = projects;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
			result = prime * result + ((part == null) ? 0 : part.hashCode());
			result = prime * result + ((plainClassName == null) ? 0 : plainClassName.hashCode());
			result = prime * result + (int) (projects ^ (projects >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClassNamePart other = (ClassNamePart) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			if (part == null) {
				if (other.part != null)
					return false;
			} else if (!part.equals(other.part))
				return false;
			if (plainClassName == null) {
				if (other.plainClassName != null)
					return false;
			} else if (!plainClassName.equals(other.plainClassName))
				return false;
			if (projects != other.projects)
				return false;
			return true;
		}
		
	}

	private static <T> Collector<T, ?, Map<T, Long>> table() {
		return Collectors.groupingBy(x -> x, Collectors.counting());
	}
}
