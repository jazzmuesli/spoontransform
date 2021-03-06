package com.mycompany.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.SourceCodeProcessor;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaClassMetricKey;
import spoon.Launcher;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

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

		public void doSomething() throws Exception {
			LOG.info("doSomething");
		}

		private String getName() {
			Properties props = new Properties();
			try {
				props.load(getClass().getResourceAsStream("app.properties"));
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}

			String name = "Unknown";
			try {
				name = props.getProperty("name");
			} finally {
				// Violation found by PMD
			}
			return name;

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
		LOG.info("Hello, {}", Singleton.getInstance().getUsefulInfo());

		if ("Pavel".equals(Singleton.getInstance().getName())) {
			if (Singleton.getInstance().currentTime() > 0) {
				LOG.info("Current time is always positive");
			} else {
				LOG.info("non-positive");
			}
		}

		extractCodeMetricsUsingPMD();
//		TODO: uncomment for debugging PMD.main("-d src/main/java -f xml -R rulesets/java/basic.xml -version 1.8 -language java".split("\\s+"));

//		runSpoon();
	}

	private static void runSpoon() {
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
					LOG.info("statements: {}", element.getBody().getStatements());
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
							LOG.info("x: {}" , x);
							CtMethod<?> intMethod = x.clone();
							intMethod.getBody().delete();
							intMethod.removeModifier(ModifierKind.PUBLIC);
							intf.addMethod(intMethod);

							getFactory().Annotation().annotate(x, Override.class);
						}
					}
					intf.setParent(singletonClass.getParent());
					singletonClass.addSuperInterface(intf.getTypeErasure());
					LOG.info("execs: {}", singletonClass.getDeclaredExecutables());
					types.add(element.getDeclaringType());
					types.add(intf);
					LOG.info("Private {}", element);
				}
			}
		});
		LOG.info("types: {}", types);
		if (!types.isEmpty()) {
			for (CtType type : types) {
				launcher.createOutputWriter().createJavaFile(type);
			}

		}
	}

	private static void extractCodeMetricsUsingPMD() {
		PMDConfiguration configuration = new PMDConfiguration();
		configuration.setReportFormat("xml");
		configuration.setInputPaths("src/main/java");
		LanguageVersion version = new JavaLanguageModule().getVersion("1.8");
		configuration.setDefaultLanguageVersion(version);
		net.sourceforge.pmd.SourceCodeProcessor processor = new SourceCodeProcessor(configuration);
		RuleContext ctx = new RuleContext();
		File sourceCodeFile = new File("src/main/java/com/mycompany/app/App.java");
		String filename = sourceCodeFile.getAbsolutePath();
		Report report = Report.createReport(ctx, filename);
		ctx.setSourceCodeFile(sourceCodeFile);
		ctx.setSourceCodeFilename(filename);
		try (InputStream sourceCode = new FileInputStream(sourceCodeFile)){
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceCode))) {
				Parser parser = PMD.parserFor(version, configuration);
				ASTCompilationUnit compilationUnit = (ASTCompilationUnit) parser.parse(filename, reader);
				LOG.info("compilationUnit: {}", compilationUnit);

//				RuleSets ruleSets = new RuleSetFactory().createRuleSets("rulesets/java/design.xml");
//				processor.processSourceCode(sourceCode, ruleSets, ctx);

				List<ASTAnyTypeDeclaration> astClassOrInterfaceDeclarations = compilationUnit
						.findDescendantsOfType(ASTAnyTypeDeclaration.class);
				LOG.info("astClassOrInterfaceDeclarations: " + astClassOrInterfaceDeclarations);
				for (ASTAnyTypeDeclaration declaration : astClassOrInterfaceDeclarations) {
					double metricValue = JavaMetrics.get(JavaClassMetricKey.NCSS, declaration);
					LOG.info("for declaration: " + declaration.getImage() + ", metric: " + metricValue + ", lines: "
							+ declaration.getBeginLine() + ":" + declaration.getEndLine());
				}
				
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		LOG.info("report: {}", report);
		for (RuleViolation viol : report) {
			LOG.info("Violation in class " + viol.getPackageName() + "." + viol.getClassName() + " on lines "
					+ viol.getBeginLine() + ":" + viol.getEndLine() + ", rule: " + viol.getRule());
		}
		LOG.info("report.summary: {}", report.getSummary());
	}
}
