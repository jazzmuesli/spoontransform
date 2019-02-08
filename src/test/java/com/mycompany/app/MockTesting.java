package com.mycompany.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class MockTesting {

	@Mock
	private List<String> fieldMockList;

	@Before
	public void setUp() {
	}

	@Test
	public void test() {
		List<String> localVarMockList = mock(List.class);
		localVarMockList.add("xs");
		fieldMockList.add("two");
		Mockito.when(localVarMockList.toArray()).thenReturn(new String[] { "a" });
		Mockito.when(fieldMockList.toArray()).thenReturn(new String[] { "b" });
		ArrayList combinedList1 = new ArrayList(localVarMockList);
		ArrayList combinedList2 = new ArrayList(fieldMockList);
		assertEquals(1, combinedList1.size());
	}
}
