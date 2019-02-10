package com.mycompany.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

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
		List<String> combinedList1 = new ArrayList<String>(localVarMockList);
		List<String> combinedList2 = new ArrayList<String>(fieldMockList);
		assertEquals(1, combinedList1.size());
	}
	
	@Test
	public void testPair() {
		ArrayList list1 = new ArrayList(fieldMockList);
		Wrapper1 wrapper1 = new Wrapper1(list1);
		Socket socket = new Socket();
		MyTCPClient tcpClient = new MyTCPClient(socket);
		MyAPIClient myAPIClient = new MyAPIClient(tcpClient);
		List<MyAPIClient> apiClientList = Arrays.asList(myAPIClient);
		Wrapper1 wrapper2 = new Wrapper1(apiClientList);
		Pair pair = Pair.of(wrapper1, wrapper2);
	}
	
	@Test
	public void socket() throws IOException {
		Socket socket = mock(Socket.class);
		OutputStream outputStream = mock(OutputStream.class);
		Mockito.when(socket.getOutputStream()).thenReturn(outputStream);
		InputStream inputStream = mock(InputStream.class);
		String value = "1.14";
		byte[] bytes = value.getBytes();
		Answer answer = new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		Mockito.doAnswer(answer).when(inputStream.read(Matchers.any(byte[].class)));
		//Mockito.when(inputStream.read(new ArgumentMatchers()).thenReturn(bytes);
		Mockito.when(socket.getInputStream()).thenReturn(inputStream);
		MyTCPClient tcpClient = new MyTCPClient(socket);
		MyAPIClient apiClient = new MyAPIClient(tcpClient);
		assertEquals(1.14, apiClient.provideFxRate("GBPEUR"), 1e-6);
	}
	
	@Test
	public void testCreation() {
		Socket socket = new Socket();
		MyTCPClient tcpClient = new MyTCPClient(socket);
		MyAPIClient apiClient = new MyAPIClient(tcpClient);
		List<MyAPIClient> list = new ArrayList();
		list.add(apiClient);
		HashSet<MyAPIClient> set = new HashSet<MyAPIClient>(list);
		
	}
	static  class  Wrapper1<T> {
		private List<T> slist;

		Wrapper1(List<T> slist) {
			this.slist = slist;
		}
	}

	static class MyTCPClient {
		private Socket socket;

		public MyTCPClient(Socket socket) {
			this.socket = socket;
		}

		public String getFxRate(String pair) {
			try {
				socket.getOutputStream().write(pair.getBytes());
				byte[] b = new byte[1024];
				socket.getInputStream().read(b);
				return new String(b);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	static class MyAPIClient {
		private MyTCPClient tcpClient;

		public MyAPIClient(MyTCPClient tcpClient) {
			this.tcpClient = tcpClient;
		}

		public Double provideFxRate(String pair) {
			return Double.valueOf(tcpClient.getFxRate(pair));
		}
	}
}
