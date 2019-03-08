/*
 * Copyright (c) 2018, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.hw.web

import java.net.PasswordAuthentication

import akka.actor.{ActorSystem, Props}
import com.salesforce.hw.actors.TrainingActor
import com.salesforce.hw.spark.SparkFactory
import com.salesforce.op.OpWorkflow
import com.salesforce.op.evaluators.Evaluators
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types.PickList
import com.salesforce.op.readers.DataReaders
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelSelector
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelsToTry.{OpLogisticRegression, OpRandomForestClassifier}
import org.apache.spark.sql.SparkSession
import com.salesforce.op.features.types._
import com.salesforce.op._
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by sruthi on 03/07/17.
  * Service class computing the value for route bindings "/activeStreams" and "/count" respectively.
  */
object HttpService {

  val sc: SparkContext = SparkFactory.sc
  val csvFilePath = "/home/kris/Downloads/transmogrifai-helloworld-sbt-master/src/main/resources/TitanicDataset/TitanicPassengersTrainData.csv"
  val system = ActorSystem("mySystem")
  val trainingActor = system.actorOf(Props[TrainingActor], "trainingActor")

  // To server http://host:port/count route binding
  // Random spark job counting a seq of integers split into 25 partitions
  def count(): String = sc.parallelize(0 to 500000, 25).count.toString

  // To server http://host:port/activeStreams route binding
  // Returns how many streams are active in sparkSession currently
  def activeStreamsInSparkContext(): Int = SparkFactory.spark.streams.active.length


  def train(): String = {
    trainingActor ! "test"
    import com.salesforce.hw.titanic.OpTitanicMini.Passenger

    implicit val spark: SparkSession = SparkFactory.spark
    import spark.implicits._

    // Read Titanic data as a DataFrame
    val pathToData = Option(csvFilePath)
    val passengersData = DataReaders.Simple.csvCase[Passenger](pathToData, key = _.id.toString).readDataset().toDF()

    // Automated feature engineering
    val (survived, features) = FeatureBuilder.fromDataFrame[RealNN](passengersData, response = "survived")
    val featureVector = features.transmogrify()

    // Automated feature selection
    val checkedFeatures = survived.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)

    // Automated model selection
    val prediction = BinaryClassificationModelSelector
      .withCrossValidation(modelTypesToUse = Seq(OpLogisticRegression, OpRandomForestClassifier))
      .setInput(survived, checkedFeatures).getOutput()

    val model = new OpWorkflow().setInputDataset(passengersData).setResultFeatures(prediction).train()
    model.save("/home/kris/Downloads/tatanka", overwrite = true)
    "Model summary:\n" + model.summaryPretty()

//    val model2 = new OpWorkflow().setResultFeatures(prediction).loadModel("/home/kris/Downloads/tatanka/").setInputDataset(passengersData)
//    val dataframe = model2.score(None,true)
//    dataframe.collect.foreach(println)
    "done"
  }

  def predict(): String = {
    import com.salesforce.hw.titanic.OpTitanicMini.Passenger

    implicit val spark: SparkSession = SparkFactory.spark
    import spark.implicits._

    val pathToData = Option(csvFilePath)
//    val testData = List(Passenger(1000L,0.0,None,"Kris",Some("male"),Some(32.0),))
    val passengersData = DataReaders.Simple.csvCase[Passenger](pathToData, key = _.id.toString).readDataset().toDF()
    val (survived, features) = FeatureBuilder.fromDataFrame[RealNN](passengersData, response = "survived")
    val featureVector = features.transmogrify()
    val checkedFeatures = survived.sanityCheck(featureVector, checkSample = 1.0, removeBadFeatures = true)
    val prediction = BinaryClassificationModelSelector
      .withCrossValidation(modelTypesToUse = Seq(OpLogisticRegression, OpRandomForestClassifier))
      .setInput(survived, checkedFeatures).getOutput()
    val model2 = new OpWorkflow().setResultFeatures(prediction).loadModel("/home/kris/Downloads/tatanka/").setInputDataset(passengersData)
    val dataframe = model2.score(None,true)
    dataframe.collect.mkString("\n")
  }

  def train2(): String = {

    import com.salesforce.hw.Passenger
    println(s"Using user-supplied CSV file path: $csvFilePath")

    // Set up a SparkSession as normal
    implicit val spark: SparkSession = SparkFactory.spark
    import spark.implicits._ // Needed for Encoders for the Passenger case class

    // Define features using the OP types based on the data
    val survived = FeatureBuilder.RealNN[Passenger].extract(_.survived.toRealNN).asResponse
    val pClass = FeatureBuilder.PickList[Passenger].extract(_.pClass.map(_.toString).toPickList).asPredictor
    val name = FeatureBuilder.Text[Passenger].extract(_.name.toText).asPredictor
    val sex = FeatureBuilder.PickList[Passenger].extract(_.sex.map(_.toString).toPickList).asPredictor
    val age = FeatureBuilder.Real[Passenger].extract(_.age.toReal).asPredictor
    val sibSp = FeatureBuilder.Integral[Passenger].extract(_.sibSp.toIntegral).asPredictor
    val parCh = FeatureBuilder.Integral[Passenger].extract(_.parCh.toIntegral).asPredictor
    val ticket = FeatureBuilder.PickList[Passenger].extract(_.ticket.map(_.toString).toPickList).asPredictor
    val fare = FeatureBuilder.Real[Passenger].extract(_.fare.toReal).asPredictor
    val cabin = FeatureBuilder.PickList[Passenger].extract(_.cabin.map(_.toString).toPickList).asPredictor
    val embarked = FeatureBuilder.PickList[Passenger].extract(_.embarked.map(_.toString).toPickList).asPredictor

    ////////////////////////////////////////////////////////////////////////////////
    // TRANSFORMED FEATURES
    /////////////////////////////////////////////////////////////////////////////////

    // Do some basic feature engineering using knowledge of the underlying dataset
    val familySize = sibSp + parCh + 1
    val estimatedCostOfTickets = familySize * fare
    val pivotedSex = sex.pivot()
    val normedAge = age.fillMissingWithMean().zNormalize()
    val ageGroup = age.map[PickList](_.value.map(v => if (v > 18) "adult" else "child").toPickList)

    // Define a feature of type vector containing all the predictors you'd like to use
    val passengerFeatures = Seq(
      pClass, name, age, sibSp, parCh, ticket,
      cabin, embarked, familySize, estimatedCostOfTickets,
      pivotedSex, ageGroup, normedAge
    ).transmogrify()

    // Optionally check the features with a sanity checker
    val checkedFeatures = survived.sanityCheck(passengerFeatures, removeBadFeatures = true)

    // Define the model we want to use (here a simple logistic regression) and get the resulting output
    val prediction = BinaryClassificationModelSelector.withTrainValidationSplit(
      modelTypesToUse = Seq(OpLogisticRegression)
    ).setInput(survived, checkedFeatures).getOutput()

    val evaluator = Evaluators.BinaryClassification().setLabelCol(survived).setPredictionCol(prediction)

    ////////////////////////////////////////////////////////////////////////////////
    // WORKFLOW
    /////////////////////////////////////////////////////////////////////////////////

    // Define a way to read data into our Passenger class from our CSV file
    val dataReader = DataReaders.Simple.csvCase[Passenger](path = Option(csvFilePath), key = _.id.toString)

    // Define a new workflow and attach our data reader
    val workflow = new OpWorkflow().setResultFeatures(survived, prediction).setReader(dataReader)

    // Fit the workflow to the data
    val model = workflow.train()

//    val model = workflow.loadModel("/home/kris/Downloads/titanic.model").setReader(dataReader)
    println(s"Model summary:\n${model.summaryPretty()}")

    // Manifest the result features of the workflow
    println("Scoring the model")
    val (scores, metrics) = model.scoreAndEvaluate(evaluator = evaluator)

    println("Metrics:\n" + metrics)

    //    println("SCORES: \n")
    //    scores.collect.foreach(println)
    model.save("/home/kris/Downloads/titanic.model")

    "model trained"
  }
}
