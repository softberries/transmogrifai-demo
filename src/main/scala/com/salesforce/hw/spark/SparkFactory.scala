/*
 * Copyright (c) 2018, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.hw.spark

import com.salesforce.hw.util.AppConfig
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * Created by sruthi on 03/07/17.
  * Creates one SparkSession which is shared and reused among multiple HttpRequests
  */
object SparkFactory {
//  val spark = SparkSession.builder.config(new SparkConf()).getOrCreate()
  val spark: SparkSession = SparkSession.builder
    .master(AppConfig.sparkMaster)
    .appName(AppConfig.sparkAppName)
    .getOrCreate

  val sc = spark.sparkContext
  val sparkConf = sc.getConf
}
