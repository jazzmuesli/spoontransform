package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;

public class App {
	interface MyInterface {
		String getUsefulInfo();
	}
	static class MyGoodClass implements MyInterface {

		public String getUsefulInfo() {
			return "Yes";
		}

	}
	static class Singleton {
		private static final Singleton instance = new Singleton();

		private Singleton() {
			LOG.info("How bad are private singleton constructors?");
		}

		public static Singleton getInstance() {
			return instance;
		}
		

		public void doSomething() throws Exception{
			LOG.info("doSomething");
		}
		private String getName() {
			return Thread.currentThread().getName();
		}
		
		public long currentTime() {
			return System.currentTimeMillis();
		}
		public String getUsefulInfo() {
			return "Yes, " + getName();
		}
		
	}

	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		LOG.info("Hello, " + Singleton.getInstance().getUsefulInfo());

		if (Singleton.getInstance().currentTime() > 0) {
			LOG.info("Current time is always positive");
		}
		Launcher launcher = new Launcher();

		// path can be a folder or a file
		// addInputResource can be called several times
		launcher.addInputResource("src/main");

		launcher.buildModel();

		CtModel model = launcher.getModel();
		final List<CtType> types = new ArrayList();
		model.processWith(new AbstractProcessor<CtConstructor<?>>() {

			public void process(CtConstructor<?> element) {
				if (element.getModifiers().contains(ModifierKind.PRIVATE)) {
					element.addModifier(ModifierKind.PUBLIC);
					element.removeModifier(ModifierKind.PRIVATE);
					LOG.info("statements:" + element.getBody().getStatements());
					CtCodeSnippetStatement snippet = getFactory().Core().createCodeSnippetStatement();
					snippet.setValue("LOG.info(\"Empty private Singleton constructors are bad. Now it's public\")");
					element.getBody().addStatement(snippet);
					CtType<?> singletonClass = element.getDeclaringType();
					singletonClass.removeModifier(ModifierKind.STATIC);
					singletonClass.addModifier(ModifierKind.PUBLIC);
					singletonClass.setParent(element.getTopLevelType().getParent());
					
					// Let's extract an interface
					CtInterface<Object> intf = getFactory().createInterface("I" + singletonClass.getSimpleName());
					intf.addModifier(ModifierKind.PUBLIC);
					for (CtMethod<?> x : singletonClass.getMethods()) {
						if (!x.isStatic() && !x.isPrivate()) {
							LOG.info("x: " + x);							
							CtMethod<?> intMethod = x.clone();
							intMethod.getBody().delete();
							intMethod.removeModifier(ModifierKind.PUBLIC);
							intf.addMethod(intMethod);
							
							getFactory().Annotation().annotate(x, Override.class);
						}
					}
					intf.setParent(singletonClass.getParent());
					singletonClass.addSuperInterface(intf.getTypeErasure());
					LOG.info("execs: " + singletonClass.getDeclaredExecutables());
					types.add(element.getDeclaringType());
					types.add(intf);
					LOG.info("Private " + element);
				}
			}
		});
		LOG.info("types: " + types);
		if (!types.isEmpty()) {
			for (CtType type : types) {
				launcher.createOutputWriter().createJavaFile(type);
			}
						
		}
	}
}
