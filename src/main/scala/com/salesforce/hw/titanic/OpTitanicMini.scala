/*
 * Copyright (c) 2018, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.hw.titanic

import com.salesforce.op._
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types._
import com.salesforce.op.readers.DataReaders
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelsToTry.{OpLogisticRegression, OpRandomForestClassifier}
import com.salesforce.op.stages.impl.classification._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
 * A minimal Titanic Survival example with TransmogrifAI
 */
object OpTitanicMini {

  case class Passenger
  (
    id: Long,
    survived: Double,
    pClass: Option[Long],
    name: Option[String],
    sex: Option[String],
    age: Option[Double],
    sibSp: Option[Long],
    parCh: Option[Long],
    ticket: Option[String],
    fare: Option[Double],
    cabin: Option[String],
    embarked: Option[String]
  )

  def main(args: Array[String]): Unit = {
    implicit val spark = SparkSession.builder.config(new SparkConf()).getOrCreate()
    import spark.implicits._

    // Read Titanic data as a DataFrame
    val pathToData = Option(args(0))
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

    println("Model summary:\n" + model.summaryPretty())
  }

}
