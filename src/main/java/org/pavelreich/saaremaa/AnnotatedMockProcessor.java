package org.pavelreich.saaremaa;

import java.util.Map;

import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtField;

public class AnnotatedMockProcessor extends AbstractProcessor<CtField> {
	static final Logger LOG = LoggerFactory.getLogger(AnalyseDependencies.class);
	
	private final Map<String, ObjectCreationOccurence> objectsCreated;

	public AnnotatedMockProcessor(Map<String, ObjectCreationOccurence> objectsCreated) {
		this.objectsCreated = objectsCreated;
	}

	@Override
	public void process(CtField element) {
		if (element.getAnnotation(Mock.class) != null) {
			objectsCreated.put(element.getSimpleName(),
					new ObjectCreationOccurence(element.getType(), element, InstanceType.MOCKITO));
			LOG.info("field [{}]={} annotations={}", element.getClass(), element,
					element.getAnnotation(Mock.class));
		}

	}

}