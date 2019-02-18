package org.pavelreich.saaremaa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphics.DotGraph;
import groum.GROUMBuilder;
import groum.GROUMGraph;
import groum.GROUMNode;
import spoon.Launcher;
import spoon.compiler.SpoonFolder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.path.CtPath;
import spoon.reflect.reference.CtTypeReference;

public class AnalyseDependencies {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyseDependencies.class);



	public Map<String, ObjectCreationOccurence> run(String path) throws FileNotFoundException {
		Launcher launcher = new Launcher();
//		launcher.addInputResource(path);
		SpoonResource resource = SpoonResourceHelper.createResource(new File(path));
		launcher.addInputResource(resource);
		launcher.buildModel();

		CtModel model = launcher.getModel();
		final Map<String, ObjectCreationOccurence> objectsCreated = new HashMap();
		model.processWith(new AbstractProcessor<CtElement>() {

			@Override
			public void process(CtElement element) {
//				LOG.info("element [{}]={}", element.getClass(), element);
			}

		});
		model.processWith(new AbstractProcessor<CtInvocation>() {

			@Override
			public void process(CtInvocation element) {
				processMockInvocation(objectsCreated, element, "org.mockito.Mockito.mock", InstanceType.MOCKITO);
				processMockInvocation(objectsCreated, element, "PowerMockito.mock", InstanceType.POWERMOCK);
			}

			private void processMockInvocation(final Map<String, ObjectCreationOccurence> objectsCreated,
					CtInvocation element, String mockMask, InstanceType mockType) {
				if (element.toString().contains(mockMask)) {
					@SuppressWarnings("unused")
					String elType = element.getTarget().toString();
					try {
						String simpleName = getSimpleName(element);
						CtExpression type = ((CtFieldRead) element.getArguments().get(0)).getTarget();
						Set<CtTypeReference<?>> x = type.getReferencedTypes();
						CtTypeReference<?> mock = x.iterator().next();
						objectsCreated.put(simpleName, new ObjectCreationOccurence(mock, element, mockType));
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
					CtExpression x = ((CtAssignment) element).getAssignment();
					return x.toString();
				}
				CtElement parent = element.getParent();
				if (parent instanceof CtNamedElement) {
					String simpleName = ((CtNamedElement) parent).getSimpleName();
					return simpleName;
				} else {
					return "UNKNOWN:" + parent.getClass().getSimpleName()+"/"+element.getClass().getSimpleName();
				}
			}

		});

		model.processWith(new AbstractProcessor<CtField>() {

			@Override
			public void process(CtField element) {
				if (element.getAnnotation(Mock.class) != null) {
					objectsCreated.put(element.getSimpleName(), new ObjectCreationOccurence(element.getType(), element, InstanceType.MOCKITO));
					LOG.info("field [{}]={} annotations={}", element.getClass(), element,
							element.getAnnotation(Mock.class));
				}

			}

		});

		model.processWith(new AbstractProcessor<CtConstructorCall>() {
			public void process(CtConstructorCall element) {
				CtElement parent = element.getParent();
				String name = "unknown";
				if (parent instanceof CtNamedElement) {
					name = ((CtNamedElement) parent).getSimpleName();
				}
				objectsCreated.put(name, new ObjectCreationOccurence(element.getType(), element, InstanceType.REAL));
				if (!element.getArguments().isEmpty() && element.getArguments().get(0) instanceof CtVariableRead) {
					CtVariableRead read = (CtVariableRead) element.getArguments().get(0);
					CtPath path = read.getPath();
					if (objectsCreated.containsKey(read.getVariable().getSimpleName())) {
						LOG.info("constructor [{}]={}, args={} path={}",
								new Object[] { element.getClass(), element, element.getArguments(), path });
					}

				}

			}
		});

		//objectsCreated.entrySet().stream().forEach(e -> LOG.info("Object " + e.getKey() + " of type " + e.getValue()));
		return objectsCreated;
	}

	private static String toString(GROUMNode node) {
		String nodeLabel = (String) GROUMNode.labelOfID.get(Integer.valueOf(node.getClassNameId()));
		String nodeLabelObj = (String) GROUMNode.labelOfID.get(Integer.valueOf(node.getObjectNameId()));
		StringBuilder s = new StringBuilder()
				.append((new StringBuilder("node ")).append(node.getId()).append(" with label:").append(nodeLabel)
						.append(".").append(nodeLabelObj).append(".").append(node.getMethod()));
//		return node.getClassName() + "::" + node.getMethod() + "[" + node.getLabel() + "]";
		return node.getLabel();
//						.append(" - ");
//						.append(node.getMethodID()).append(" - ").append(node.getLabel()).toString());
//		return s.toString();
	}

	public static void main(String[] args) {
		runMockAnalysis(args);
//		String path = "/Users/preich/Documents/github/evosuite/client/src/test/java/org/evosuite/junit/naming/methods/TestCoverageGoalNameGeneration.java";
//		path = "/Users/preich/Documents/github/spoontransform/src/test/java/com/mycompany/app/MockTesting.java";
//		runGroom(path);

	}

	private static void runGroom(String path) {
		GROUMBuilder gb = new GROUMBuilder(path);
		gb.build();
		GROUMGraph groum;
		DotGraph.EXEC_DOT = System.getProperty("dot.path", "dot");
		Iterator iterator = gb.getGroums().iterator();
		while (iterator.hasNext()) {
			groum = (GROUMGraph) iterator.next();
			HashSet<GROUMNode> nodes = groum.getNodes();
//			nodes.forEach(n -> LOG.info("node: " + toString(n)));
			groum.toGraphics("/tmp/");
			List<Object> xs = nodes.stream()
					.flatMap(x -> x.getOutEdges().stream().map(e -> toString(x) + " => " + toString(e.getDest())))
					.collect(Collectors.toList());
			xs.forEach(s -> LOG.info("edge: " + String.valueOf(s)));
			LOG.info("nodes: " + nodes);
		}
	}

	private static void runMockAnalysis(String[] args) {
		try (FileWriter fw = new FileWriter("output.csv")) {
			if (args.length != 1) {
				throw new IllegalArgumentException("Usage: directory");
			}
			fw.write("objectType,objectName,objectClass,file,position,rootPath\n");
			Path rootPath = Paths.get(args[0]);
			Files.walk(rootPath)
					.filter(f -> isRelevantFile(f))
					.parallel()
					.forEach(path -> processPath(fw, path, rootPath));

		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static boolean isRelevantFile(Path f) {
		if (Boolean.valueOf(System.getProperty("scan.zip","false"))) {
			return f.toString().endsWith(".zip");
		}
		return f.toFile().isDirectory() & f.toFile().getAbsolutePath().endsWith("src/test");
	}

	private static void processPath(FileWriter fw, Path path, Path rootPath) {
		LOG.info("path: " + path);

		try {
			Map<String, ObjectCreationOccurence> mocks = new AnalyseDependencies().run(path.toFile().getAbsolutePath());
			mocks.entrySet().forEach(e -> {
				try {
					ObjectCreationOccurence mockOc = e.getValue();
					fw.write(e.getKey() + "," + mockOc.toCSV() + "," + path.toFile().getAbsolutePath() + "\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			if (!mocks.isEmpty() && Boolean.valueOf(System.getProperty("enable.groom","false"))) {
				mocks.values().stream().map(x -> x.getAbsolutePath()).filter(p -> p != null).forEach(x -> runGroom(x));
			}
			fw.flush();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	static class ObjectCreationOccurence {
		private CtElement element;
		private CtTypeReference typeRef;
		private InstanceType instanceType;

		public ObjectCreationOccurence(CtTypeReference mock, CtElement element, InstanceType instanceType) {
			this.typeRef = mock;
			this.element = element;
			this.instanceType = instanceType;
		}

		public String toCSV() {
			Integer line = null;
			String absolutePath = null;
			try {
				absolutePath = getAbsolutePath();
				line = typeRef.getPosition() instanceof NoSourcePosition ? null : typeRef.getPosition().getLine();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			return instanceType + "," + typeRef.toString() + "," + absolutePath + "," + line;
		}

		private String getAbsolutePath() {
			try {
				return element.getPosition().getFile().getAbsolutePath();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return "[type=" + typeRef + ", element.position=" + element.getPosition() + "]";
		}
	}

	static enum InstanceType {
		REAL, MOCKITO, POWERMOCK
	};
}
