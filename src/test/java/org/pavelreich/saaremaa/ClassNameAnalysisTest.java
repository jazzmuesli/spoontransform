package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.pavelreich.saaremaa.ClassNameAnalysis.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassNameAnalysisTest {
	private static final Logger LOG = LoggerFactory.getLogger(ClassNameAnalysis.class);

	@Test
	public void calculateNGrams() {
		List<NGram> ret = ClassNameAnalysis.calculateNGrams(2, Arrays.asList("Linked","Hash","Map"));
		LOG.info("ret: " + ret);
	}
	@Test
	public void test() {
		Set<ClassName> classNames = new HashSet();
		classNames.add(new ClassName("java.util.Map", "project1"));
		classNames.add(new ClassName("java.util.LinkedHashMap", "project2"));
		classNames.add(new ClassName("java.util.HashMap", "project2"));
		classNames.add(new ClassName("java.util.concurrent.ConcurrentHashMap", "project1"));
		classNames.add(new ClassName("java.util.concurrent.ConcurrentHashMap", "project2"));
		classNames.add(new ClassName("java.util.concurrent.ConcurrentHashMapTest", "project2"));
		Map<String, Long> projectCountByClassName = classNames.stream().map(x -> x.className)
				.collect(ClassNameAnalysis.calculateFrequencyTable());
		List<ClassNamePart> ret = classNames.stream()
				.map(className -> ClassNameAnalysis.createNGramParts(className.className, projectCountByClassName.get(className.className))).flatMap(List::stream)
				.collect(Collectors.toList());
		ret.forEach(x -> LOG.info("x: " + x));
		Map<String, Long> resultsByPart = ret.stream().map(x->x.ngram.part).collect(ClassNameAnalysis.calculateFrequencyTable());
		resultsByPart.entrySet().forEach(x -> LOG.info("part: " + x));
		assertEquals(11, resultsByPart.size());
		assertEquals(6, resultsByPart.get("Map").longValue());
	}


}
