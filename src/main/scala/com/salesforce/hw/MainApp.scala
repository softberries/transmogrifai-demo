/*
 * Copyright (c) 2018, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.hw

import akka.http.scaladsl.settings.ServerSettings
import com.salesforce.hw.util.AppConfig
import com.salesforce.hw.web.WebServer
import com.salesforce.op.OpWorkflow
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.readers.DataReaders
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import com.salesforce.op._
import com.salesforce.op.evaluators.Evaluators
import com.salesforce.op.features.FeatureBuilder
import com.salesforce.op.features.types._
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelSelector
import com.salesforce.op.stages.impl.classification.BinaryClassificationModelsToTry.OpLogisticRegression
import com.typesafe.config.ConfigFactory

object MainApp {

  def main(args: Array[String]): Unit = {
    println("main app")

    // Starting the server
    WebServer.startServer("localhost", AppConfig.akkaHttpPort, ServerSettings(ConfigFactory.load))

    println(s"Server online at http://localhost:", AppConfig.akkaHttpPort, "/")
  }
}
