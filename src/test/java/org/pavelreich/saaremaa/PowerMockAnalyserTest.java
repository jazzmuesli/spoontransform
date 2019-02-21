package org.pavelreich.saaremaa;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class PowerMockAnalyserTest {
	private static final Logger LOG = LoggerFactory.getLogger(PowerMockAnalyserTest.class);
	
    @Test
    public void test() throws FileNotFoundException {

    	TestFileProcessor s = TestFileProcessor.run("src/test");
    	s.getElements().forEach(x->LOG.info("x: "+ x.toJSON()));
    }
    
    @Test
    public void tesSet() {
    	assertTrue(TestFileProcessor.SETUP_CLASSES.containsAll(Arrays.asList("Before","After")));
    }

}
