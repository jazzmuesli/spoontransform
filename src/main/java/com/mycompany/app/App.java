package com.mycompany.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.Filter;

public class App {
	static class Singleton {
		private static final Singleton instance = new Singleton();
		private Singleton() {
			
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
//		model.filterChildren(new Filter() {
//
//			public boolean matches(CtElement element) {
//				return element.getPath().;
//			}
//			
//		})
		Processor<?> processor = new AbstractProcessor<CtStatement>() {
			@Override
			public void process(CtStatement element) {
				LOG.info("path: " + element.getPath());
				LOG.info("element:" + element);
				
			}
			
		};
		model.processWith(processor);
//		LOG.info("results: " + model.getRootPackage().filterChildren((CtStatement m)  -> m.getLabel().contains("get")).list());
//		
//		LOG.info("model: " + model.getAllTypes());
	}
}
