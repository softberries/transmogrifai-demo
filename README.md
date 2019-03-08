TransmogrifAI Hello World for SBT
---------------------------------

First, [Download Spark 2.3.2](https://spark.apache.org/downloads.html)

Run TitanicSimple:

`SPARK_HOME=your_spark_home_dir ./sbt "sparkSubmit --class com.salesforce.hw.OpTitanicSimple -- /full-path-to-project/src/main/resources/TitanicDataset/TitanicPassengersTrainData.csv"`



`SPARK_HOME=/home/kris/Public/spark-2.3.3-bin-hadoop2.7 ./sbt "sparkSubmit --class com.salesforce.hw.OpTitanicSimple -- /home/kris/Downloads/transmogrifai-helloworld-sbt-master/src/main/resources/TitanicDataset/TitanicPassengersTrainData.csv"`
