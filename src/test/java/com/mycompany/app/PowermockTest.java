package com.mycompany.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pavelreich.saaremaa.TestFileProcessor;
import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//@RunWith(PowerMockRunner.class)
@PrepareForTest(TestFileProcessor.class)
//@SuppressStaticInitializationFor(value="org.pavelreich.saaremaa.AnnotatedMockProcessor")
public class PowermockTest {

	@Test
	public void test() {
		System.out.println("hello");
	}
}
