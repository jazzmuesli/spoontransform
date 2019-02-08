package com.mycompany.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.path.CtPath;
import spoon.reflect.reference.CtTypeReference;

public class AnalyseDependencies {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyseDependencies.class);

	public void run() {
		Launcher launcher = new Launcher();

		launcher.addInputResource("src/main");
		launcher.addInputResource("src/test");

		launcher.buildModel();
		List<String> xsds = new ArrayList<String>();

		CtModel model = launcher.getModel();
		final Map<String, CtTypeReference> mocks = new HashMap();
		model.processWith(new AbstractProcessor<CtElement>() {

			@Override
			public void process(CtElement element) {
//				LOG.info("element [{}]={}", element.getClass(), element);
			}
			
		});
		model.processWith(new AbstractProcessor<CtInvocation>() {

			@Override
			public void process(CtInvocation element) {
				String targetClass = org.mockito.Mockito.class.getCanonicalName() + ".mock";
				if (element.toString().startsWith("org.mockito.Mockito.mock")//.getTarget() != null && element.getTarget().toString().equals(targetClass) 
						/*&& element.getExecutable().toString().contains("mock")*/) {
					String elType = element.getTarget().toString();
					CtElement parent = element.getParent();//TODO: fix types, etc.
					String simpleName = ((CtLocalVariable)parent).getSimpleName();
					CtExpression type = ((CtFieldRead)element.getArguments().get(0)).getTarget();
					Set<CtTypeReference<?>> x = type.getReferencedTypes();
					mocks.put(simpleName, x.iterator().next());
					LOG.info("invocation [{}]={} args={} annotations={}", element.getClass(), element, element.getArguments(), element.getAnnotations());
				}
				
			}
			
		});

		model.processWith(new AbstractProcessor<CtField>() {

			@Override
			public void process(CtField element) {
				if (element.getAnnotation(Mock.class) != null) {
					mocks.put(element.getSimpleName(), element.getType());
					LOG.info("field [{}]={} annotations={}", element.getClass(), element, element.getAnnotation(Mock.class));
				}
				
			}
			
		});

		
		model.processWith(new AbstractProcessor<CtConstructorCall>() {
			public void process(CtConstructorCall element) {
				if (!element.getArguments().isEmpty() && element.getArguments().get(0) instanceof CtVariableRead) {
					CtVariableRead read = (CtVariableRead) element.getArguments().get(0);
					CtPath path = read.getPath();
					if (mocks.containsKey(read.getVariable().getSimpleName())) {
						LOG.info("constructor [{}]={}, args={} path={}", element.getClass(), element, element.getArguments(), path);						
					}
					
					
				}
				
			}
		});
		
		mocks.entrySet().stream().forEach(e -> LOG.info("Mock " + e.getKey() + " of type " + e.getValue()));
	}
	
	public static void main(String[] args) {
		new AnalyseDependencies().run();
	}
}
