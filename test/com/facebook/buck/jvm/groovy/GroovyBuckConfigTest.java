/*
 * Copyright 2015-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.buck.jvm.groovy;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;

import com.facebook.buck.cli.BuckConfig;
import com.facebook.buck.config.Config;
import com.facebook.buck.config.RawConfig;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.testutil.integration.DebuggableTemporaryFolder;
import com.facebook.buck.util.environment.Architecture;
import com.facebook.buck.util.environment.Platform;
import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class GroovyBuckConfigTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DebuggableTemporaryFolder temporaryFolder = new DebuggableTemporaryFolder();

  @Test
  public void refuseToContinueWhenInsufficientInformationToFindGroovycIsProvided() {
    thrown.expectMessage(
        allOf(
            containsString("Unable to locate groovy compiler"),
            containsString("GROOVY_HOME is not set, and groovy.groovy_home was not provided")));

    ImmutableMap<String, String> environment = ImmutableMap.of();
    ImmutableMap<String, ImmutableMap<String, String>> rawConfig = ImmutableMap.of();
    final GroovyBuckConfig groovyBuckConfig =
        createGroovyConfig(environment, rawConfig);

    groovyBuckConfig.getGroovyCompiler();
  }

  @Test
  public void refuseToContinueWhenInformationResultsInANonExistentGroovycPath() {
    thrown.expectMessage(
        containsString("Unable to locate /oops/bin/groovyc on PATH"));

    ImmutableMap<String, String> environment = ImmutableMap.of("GROOVY_HOME", "/oops");
    ImmutableMap<String, ImmutableMap<String, String>> rawConfig = ImmutableMap.of();
    final GroovyBuckConfig groovyBuckConfig =
        createGroovyConfig(environment, rawConfig);

    groovyBuckConfig.getGroovyCompiler();
  }

  @Test
  public void byDefaultFindGroovycFromGroovyHome() {
    String systemGroovyHome = System.getenv("GROOVY_HOME");
    assumeTrue(systemGroovyHome != null);

    //noinspection ConstantConditions
    ImmutableMap<String, String> environment = ImmutableMap.of("GROOVY_HOME", systemGroovyHome);
    ImmutableMap<String, ImmutableMap<String, String>> rawConfig = ImmutableMap.of();
    final GroovyBuckConfig groovyBuckConfig =
        createGroovyConfig(environment, rawConfig);

    // it's enough that this doesn't throw.
    groovyBuckConfig.getGroovyCompiler();
  }

  @Test
  public void explicitConfigurationOverridesTheEnvironment() {
    String systemGroovyHome = System.getenv("GROOVY_HOME");
    assumeTrue(systemGroovyHome != null);

    // deliberately break the env
    ImmutableMap<String, String> environment = ImmutableMap.of("GROOVY_HOME", "/oops");
    //noinspection ConstantConditions
    ImmutableMap<String, ImmutableMap<String, String>> rawConfig =
        ImmutableMap.of("groovy", ImmutableMap.of("groovy_home", systemGroovyHome));
    final GroovyBuckConfig groovyBuckConfig =
        createGroovyConfig(environment, rawConfig);

    // it's enough that this doesn't throw.
    groovyBuckConfig.getGroovyCompiler();
  }

  private GroovyBuckConfig createGroovyConfig(
      ImmutableMap<String, String> environment,
      ImmutableMap<String, ImmutableMap<String, String>> rawConfig) {
    ProjectFilesystem projectFilesystem = new ProjectFilesystem(temporaryFolder.getRootPath());
    BuckConfig config = new BuckConfig(
        new Config(RawConfig.of(rawConfig)),
        projectFilesystem,
        Architecture.detect(),
        Platform.detect(),
        environment);

    return new GroovyBuckConfig(config);
  }
}
