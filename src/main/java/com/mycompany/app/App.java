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
	}

	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		LOG.info("Hello, " + Singleton.getInstance());

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
					types.add(element.getDeclaringType());
					LOG.info("Private " + element);
				}
			}
		});
		LOG.info("types: " + types);
		if (!types.isEmpty()) {
			launcher.createOutputWriter().createJavaFile(types.get(0));			
		}
	}
}
