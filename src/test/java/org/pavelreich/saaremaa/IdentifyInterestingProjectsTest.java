package org.pavelreich.saaremaa;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.*;
import org.mockito.Mockito;
import org.pavelreich.saaremaa.IdentifyInterestingProjects.CSVReporter;

public class IdentifyInterestingProjectsTest {

	@Test
	public void testProcessPath() throws IOException {
		CSVReporter filesReporter = Mockito.mock(CSVReporter.class);
		CSVReporter suitableReporter = Mockito.mock(CSVReporter.class);
		IdentifyInterestingProjects proj = new IdentifyInterestingProjects(suitableReporter, filesReporter);
		proj.processPath(Paths.get("."));
		Mockito.verify(filesReporter, Mockito.atLeast(10)).write(Mockito.any());
		Mockito.verify(suitableReporter).write(Mockito.any());
	}
	
	@Test
	public void testScanPomProjects() throws IOException {
		new IdentifyInterestingProjects().run();
	}
}
