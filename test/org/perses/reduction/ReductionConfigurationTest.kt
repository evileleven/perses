/*
 * Copyright (C) 2003-2017 Chengnian Sun.
 *
 * This file is part of Perses.
 *
 * Perses is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * Perses is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Perses; see the file LICENSE.  If not see <http://www.gnu.org/licenses/>.
 */
package org.perses.reduction

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.perses.TestUtility
import org.perses.reduction.ReductionConfiguration.Companion.getTempRootFolderName
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

/** Test for [ReductionConfiguration]  */
@RunWith(JUnit4::class)
class ReductionConfigurationTest {
  private val testScript = File(FOLDER + "r.sh")
  private var workingDirectory: File? = null
  private val sourceFile = File(FOLDER + "t.c")
  @Before
  @Throws(IOException::class)
  fun setup() {
    workingDirectory = TestUtility.createCleanWorkingDirectory(ReductionConfigurationTest::class.java)
  }

  @After
  @Throws(IOException::class)
  fun teardown() {
    MoreFiles.deleteRecursively(workingDirectory!!.toPath(), RecursiveDeleteOption.ALLOW_INSECURE)
  }

  @Test
  @Throws(IOException::class)
  fun testConfiguration() {
    Truth.assertThat(workingDirectory!!.isDirectory).isTrue()
    val bestFile = sourceFile
    val numOfReductionThreads = 2
    val maxReductionLevel = 100
    val configuration = ReductionConfiguration(
        workingFolder = workingDirectory!!,
        testScriptFile = testScript,
        fileToReduce = sourceFile,
        bestResultFile = bestFile,
        statisticsFile = null,
        progressDumpFile = null,
        keepOriginalCodeFormat = true,
        fixpointReduction = true,
        enableTestScriptExecutionCaching = true,
        useRealDeltaDebugger = false,
        maxReductionLevel = maxReductionLevel,
        numOfReductionThreads = numOfReductionThreads,
        multiNodePartitionReductionPolicy = TestUtility.PURE_PERSES_MULTI_NODE_PARTITION_REDUCTION_POLICY,
        singleNodePartitionReductionPolicy = TestUtility.PURE_PERSES_SINGLE_NODE_PARTITION_REDUCTION_POLICY)
    Truth.assertThat(configuration.bestResultFile).isEqualTo(bestFile)
    Truth.assertThat(configuration.fileToReduce.file).isEqualTo(sourceFile)
    Truth.assertThat(configuration.fileToReduce.fileContent)
        .isEqualTo(MoreFiles.asCharSource(sourceFile.toPath(), StandardCharsets.UTF_8).read())
    Truth.assertThat(configuration.maxReductionLevel).isEqualTo(maxReductionLevel)
    Truth.assertThat(configuration.numOfReductionThreads).isEqualTo(numOfReductionThreads)
    Truth.assertThat(configuration.workingFolder).isEqualTo(workingDirectory)
    run {
      val tempRootFolder = configuration.tempRootFolder
      Truth.assertThat(tempRootFolder.parentFile).isEqualTo(workingDirectory)
      Truth.assertThat(tempRootFolder.name).startsWith("PersesTempRoot_t.c_r.sh_")
    }
    Truth.assertThat(configuration.isFixpointReduction).isTrue()
  }

  @Test
  fun testGetTempRootFolderName() {
    val time = LocalDateTime.of(2000, 1, 21, 1, 2, 3)
    Truth.assertThat(getTempRootFolderName("t.c", "r.sh", time))
        .isEqualTo("PersesTempRoot_t.c_r.sh_20000121_010203")
  }

  companion object {
    private const val FOLDER = "test_data/delta_1/"
  }
}