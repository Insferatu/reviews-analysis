name := "rf-test-task"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.2",
  "org.apache.spark" %% "spark-sql" % "2.0.2",
  "com.databricks" %% "spark-csv" % "1.5.0",
  "com.ibm.icu" % "icu4j" % "56.1",

  "org.scalatest" % "scalatest_2.11" % "2.2.6" % Test
)
    