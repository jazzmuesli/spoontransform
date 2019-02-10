package org.pavelreich.saaremaa;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtPath;
import spoon.reflect.reference.CtTypeReference;

public class AnalyseDependencies {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyseDependencies.class);

	public Map<String, MockOccurence> run(String path) {
		Launcher launcher = new Launcher();
		launcher.addInputResource(path);

		launcher.buildModel();
		List<String> xsds = new ArrayList<String>();

		CtModel model = launcher.getModel();
		final Map<String, MockOccurence> mocks = new HashMap();
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
				if (element.toString().startsWith("org.mockito.Mockito.mock")// .getTarget() != null &&
																				// element.getTarget().toString().equals(targetClass)
				/* && element.getExecutable().toString().contains("mock") */) {
					String elType = element.getTarget().toString();
					try {
						String simpleName = getSimpleName(element);
						CtExpression type = ((CtFieldRead) element.getArguments().get(0)).getTarget();
						Set<CtTypeReference<?>> x = type.getReferencedTypes();
						CtTypeReference<?> mock = x.iterator().next();
						mocks.put(simpleName, new MockOccurence(mock, element));
						LOG.info("invocation [{}]={} args={} annotations={}", element.getClass(), element,
								element.getArguments(), element.getAnnotations());
					} catch (Exception e) {
						LOG.error("Can't parse element {} at {} due to error {}",
								new Object[] { element, element.getPosition(), e.getMessage() }, e);
					}
				}

			}

			private String getSimpleName(CtInvocation element) {
				if (element instanceof CtAssignment) {
					CtExpression x = ((CtAssignment)element).getAssignment();
					return x.toString();
				}
				CtElement parent = element.getParent();// TODO: fix types, etc.
				String simpleName = ((CtNamedElement) parent).getSimpleName();
				return simpleName;
			}

		});

		model.processWith(new AbstractProcessor<CtField>() {

			@Override
			public void process(CtField element) {
				if (element.getAnnotation(Mock.class) != null) {
					mocks.put(element.getSimpleName(), new MockOccurence(element.getType(), element));
					LOG.info("field [{}]={} annotations={}", element.getClass(), element,
							element.getAnnotation(Mock.class));
				}

			}

		});

		model.processWith(new AbstractProcessor<CtConstructorCall>() {
			public void process(CtConstructorCall element) {
				if (!element.getArguments().isEmpty() && element.getArguments().get(0) instanceof CtVariableRead) {
					CtVariableRead read = (CtVariableRead) element.getArguments().get(0);
					CtPath path = read.getPath();
					if (mocks.containsKey(read.getVariable().getSimpleName())) {
						LOG.info("constructor [{}]={}, args={} path={}", element.getClass(), element,
								element.getArguments(), path);
					}

				}

			}
		});

		mocks.entrySet().stream().forEach(e -> LOG.info("Mock " + e.getKey() + " of type " + e.getValue()));
		return mocks;
	}

	public static void main(String[] args) {
		try (FileWriter fw = new FileWriter("output.csv")) {
			if (args.length != 1) {
				throw new IllegalArgumentException("Usage: directory");
			}
			fw.write("mockName,mockClass,file,position\n");
			Files.walk(Paths.get(args[0]))
					.filter(f -> f.toFile().isDirectory() & f.toFile().getAbsolutePath().endsWith("src/test"))
					.forEach(path -> processPath(fw, path));

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static void processPath(FileWriter fw, Path x) {
		LOG.info("path: " + x);

		try {
			Map<String, MockOccurence> mocks = new AnalyseDependencies().run(x.toFile().getAbsolutePath());
			mocks.entrySet().forEach(e -> {
				try {
					MockOccurence mockOc = e.getValue();
					fw.write(e.getKey() + "," + mockOc.toCSV() + "\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			fw.flush();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	static class MockOccurence {
		private CtElement element;
		private CtTypeReference typeRef;

		public MockOccurence(CtTypeReference mock, CtElement element) {
			this.typeRef = mock;
			this.element = element;
		}

		public String toCSV() {
			Integer line = null;
			String absolutePath = null;
			try {
				absolutePath = element.getPosition().getFile().getAbsolutePath();
				line = typeRef.getPosition().getLine();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			return typeRef.toString() + "," + absolutePath + "," + line;
		}

		@Override
		public String toString() {
			return "[type=" + typeRef + ", element.position=" + element.getPosition() + "]";
		}
	}
}
