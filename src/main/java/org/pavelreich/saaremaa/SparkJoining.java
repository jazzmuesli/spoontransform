package org.pavelreich.saaremaa;

import static org.apache.spark.sql.functions.callUDF;

import java.util.Arrays;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

public class SparkJoining {
	private static UDF1 extractProject = new UDF1<String, String>() {
		public String call(final String fname) throws Exception {
			return fname.replaceAll(".*?\\./([^/]+/[^/]+).*", "$1");
		}
	};

	public static void main(String[] args) {
		String dir = args[0];

		SparkSession spark = SparkSession.builder().appName("CSV to Dataset").master("local[*]").getOrCreate();
		Dataset<Row> mocks = spark.read().format("csv").option("delimiter", ";").option("header", "true")
				.load(dir + "results.csv").drop("LOC");

		Dataset<Row> metrics = spark.read().format("csv").option("delimiter", ";").option("header", "true")
				.load(dir + "class-metrics.csv").drop("loc");

		spark.udf().register("extractProject", extractProject, DataTypes.StringType);
		metrics = metrics.withColumn("project", callUDF("extractProject", metrics.col("file")));
		mocks = mocks.withColumn("project", callUDF("extractProject", mocks.col("fileName")));
		mocks = mocks.withColumn("class", mocks.col("mockClass"));
		Dataset<Row> ret = mocks.join(
				metrics, scala.collection.JavaConverters
						.asScalaIteratorConverter(Arrays.asList("project", "class").iterator()).asScala().toSeq(),
				"inner");
		ret.show();

		System.out.println("count: " + ret.count());
		ret.repartition(1).write().option("header", "true").csv(dir+"merged.csv");
	}
}
