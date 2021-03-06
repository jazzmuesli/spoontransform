package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassNameAnalysis {

	private static final Logger LOG = LoggerFactory.getLogger(ClassNameAnalysis.class);

	static class NGram {
		String part;
		private int start;
		private int end;
		private int size;

		NGram(String part, int start, int end, int size) {
			this.part = part;
			this.start = start;
			this.end = end;
			this.size = size;
		}

		public NGram(List<String> parts, int i, int end, int size) {
			this(parts.stream().collect(Collectors.joining()), i, end, size);
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + end;
			result = prime * result + ((part == null) ? 0 : part.hashCode());
			result = prime * result + size;
			result = prime * result + start;
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
			NGram other = (NGram) obj;
			if (end != other.end)
				return false;
			if (part == null) {
				if (other.part != null)
					return false;
			} else if (!part.equals(other.part))
				return false;
			if (size != other.size)
				return false;
			if (start != other.start)
				return false;
			return true;
		}

		public Object[] toCSV() {
			return new Object[] { part, start, end, size };
		}

	}


	public static List<NGram> calculateNGrams(int n, List<String> str) {
		List<NGram> ngrams = new ArrayList();
		for (int i = 0; i < str.size() - n + 1; i++)
			ngrams.add(new NGram(str.subList(i, i + n), i, i + n, str.size()));
		return ngrams;
	}

	public static void main(String[] args) throws IOException {
		run(args[0]);
	}

	static List<NGram> extractNGrams(String className) {
		List<String> parts = Arrays.asList(extractParts(extractPlainName(className)));
		if (parts.size() == 1) {
			return Arrays.asList(new NGram(parts.get(0), 0, 1, 1));
		}
		List<NGram> ret = new ArrayList();
		for (int i = parts.size() - 1; i > 0; i--) {
			ret.addAll(calculateNGrams(i, parts));
		}

		return ret;
	}

	private static void run(String dir) throws IOException {
		CSVParser parser = CSVParser.parse(new File(dir + "class-metrics.csv"), Charset.defaultCharset(),
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		Set<ClassName> classNames = new HashSet();
		parser.forEach(x -> classNames.add(new ClassName(x.get("class"), x.get("file"))));

		LOG.info("Loaded " + classNames.size() + " classNames");
		Map<String, Long> projectCountByClassName = classNames.stream().map(x -> x.className)
				.collect(ClassNameAnalysis.calculateFrequencyTable());

		List<ClassNamePart> classNameParts = classNames.stream()
				.map(className -> ClassNameAnalysis.createNGramParts(className.className,
						projectCountByClassName.get(className.className)))
				.flatMap(List::stream).collect(Collectors.toList());
		CSVReporter csvReporterParts = new CSVReporter("class-name-parts.csv", "classNamePart", "start", "end", "size",
				"plainClassName", "className", "projects");
		classNameParts.forEach(x -> csvReporterParts.write(x.toCSV()));
		csvReporterParts.close();
		LOG.info("Generated " + classNameParts.size() + " parts");
		
		
		CSVReporter csvReporterNGrams = new CSVReporter("class-name-ngrams.csv", "classNamePart", "start", "end", "size",
				"projects");

		Map<NGram, Long> projectCountByNGram = classNameParts.stream()
				.collect(Collectors.groupingBy(x -> x.ngram, Collectors.summingLong(x -> x.projects)));
		projectCountByNGram.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEach(x -> csvReporterNGrams.write(x.getKey().part, x.getKey().start, x.getKey().end, x.getKey().size, x.getValue()));
		csvReporterNGrams.close();

		LOG.info("Generated " + projectCountByNGram.size() + " ngrams");
	}

	static List<ClassNamePart> createNGramParts(String className, long projects) {
		List<NGram> parts = ClassNameAnalysis.extractNGrams(className);
		return parts.stream().map(part -> new ClassNamePart(part, className, projects)).collect(Collectors.toList());
	}

	private static String[] extractParts(String plainClassName) {
		return plainClassName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	}

	static class ClassName {
		String className;
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

	static String extractPlainName(String className) {
		return className.replaceAll(".*?\\.([^\\.]+)$", "$1");
	}

	static class ClassNamePart {
		NGram ngram;
		String plainClassName;
		String className;
		long projects;

		ClassNamePart(NGram part, String className, long projects) {
			this(part, extractPlainName(className), className, projects);
		}

		ClassNamePart(NGram part, String plainClassName, String className, long projects) {
			this.ngram = part;
			this.plainClassName = plainClassName;
			this.className = className;
			this.projects = projects;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

		Object[] toCSV() {
			return new Object[] { ngram.part, ngram.start, ngram.end, ngram.size, plainClassName, className, projects };
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
			result = prime * result + ((ngram == null) ? 0 : ngram.hashCode());
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
			if (ngram == null) {
				if (other.ngram != null)
					return false;
			} else if (!ngram.equals(other.ngram))
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

	static <T> Collector<T, ?, Map<T, Long>> calculateFrequencyTable() {
		return Collectors.groupingBy(x -> x, Collectors.counting());
	}
}
