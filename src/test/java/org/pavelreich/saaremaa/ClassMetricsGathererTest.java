package org.pavelreich.saaremaa;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClassMetricsGathererTest {

	@Test
	public void testRegex() {
		String className = "org.pavelreich.saaremaa.TestFileProcessor$MyClass";
		assertEquals("TestFileProcessor$MyClass", ClassMetricsGatherer.getSimpleClassName(className));
	}
}
