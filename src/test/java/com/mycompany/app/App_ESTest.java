/*
 * This file was automatically generated by EvoSuite
 * Tue Oct 16 00:56:57 GMT 2018
 */

package com.mycompany.app;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.mycompany.app.App;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.evosuite.runtime.testdata.EvoSuiteFile;
import org.evosuite.runtime.testdata.FileSystemHandling;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class App_ESTest extends App_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      App.MyGoodClass app_MyGoodClass0 = new App.MyGoodClass();
      String string0 = app_MyGoodClass0.getUsefulInfo();
      assertEquals("Yes", string0);
  }

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
      App.Singleton app_Singleton0 = App.Singleton.getInstance();
      String string0 = app_Singleton0.getUsefulInfo();
      assertEquals("Yes, TEST_EXECUTION_THREAD_1", string0);
  }

  @Test(timeout = 4000)
  public void test2()  throws Throwable  {
      EvoSuiteFile evoSuiteFile0 = new EvoSuiteFile("spooned");
      FileSystemHandling.createFolder(evoSuiteFile0);
      App.main((String[]) null);
  }

  @Test(timeout = 4000)
  public void test3()  throws Throwable  {
      App app0 = new App();
  }

  @Test(timeout = 4000)
  public void test4()  throws Throwable  {
      App.MyGoodClass app_MyGoodClass0 = new App.MyGoodClass();
      String string0 = app_MyGoodClass0.getUsefulInfo();
      assertEquals("Yes", string0);
  }

  @Test(timeout = 4000)
  public void test5()  throws Throwable  {
      App.Singleton app_Singleton0 = App.Singleton.getInstance();
      app_Singleton0.doSomething();
  }

  @Test(timeout = 4000)
  public void test6()  throws Throwable  {
      String[] stringArray0 = new String[1];
      // Undeclared exception!
      try { 
        App.main(stringArray0);
        fail("Expecting exception: RuntimeException");
      
      } catch(RuntimeException e) {
         //
         // Output must be a directory
         //
         verifyException("spoon.support.StandardEnvironment", e);
      }
  }
}
