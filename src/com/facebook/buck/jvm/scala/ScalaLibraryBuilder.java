/*
 * Copyright 2017-present Facebook, Inc.
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

package com.facebook.buck.jvm.scala;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.jvm.java.ConfiguredCompiler;
import com.facebook.buck.jvm.java.DefaultJavaLibraryBuilder;
import com.facebook.buck.jvm.java.JavaBuckConfig;
import com.facebook.buck.jvm.java.JavacOptions;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.TargetGraph;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ScalaLibraryBuilder extends DefaultJavaLibraryBuilder {
  private final ScalaBuckConfig scalaBuckConfig;
  private ImmutableList<String> extraArguments = ImmutableList.of();

  ScalaLibraryBuilder(
      TargetGraph targetGraph,
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      BuildRuleResolver buildRuleResolver,
      CellPathResolver cellRoots,
      ScalaBuckConfig scalaBuckConfig,
      JavaBuckConfig javaBuckConfig) {
    super(
        targetGraph,
        buildTarget,
        projectFilesystem,
        params,
        buildRuleResolver,
        cellRoots,
        javaBuckConfig);
    this.scalaBuckConfig = scalaBuckConfig;

    // See https://github.com/facebook/buck/issues/1386
    setCompileAgainstAbis(false);
  }

  public ScalaLibraryBuilder setArgs(ScalaLibraryDescription.CoreArg args) {
    this.setSrcs(args.getSrcs())
        .setResources(args.getResources())
        .setResourcesRoot(args.getResourcesRoot())
        .setProvidedDeps(args.getProvidedDeps())
        .setManifestFile(args.getManifestFile())
        .setMavenCoords(args.getMavenCoords());
    extraArguments = args.getExtraArguments();

    return this;
  }

  @Override
  protected BuilderHelper newHelper() {
    return new BuilderHelper();
  }

  @Override
  public ScalaLibraryBuilder setJavacOptions(JavacOptions javacOptions) {
    super.setJavacOptions(javacOptions);
    return this;
  }

  protected class BuilderHelper extends DefaultJavaLibraryBuilder.BuilderHelper {
    @Override
    protected ConfiguredCompiler buildConfiguredCompiler() {
      ScalaBuckConfig scalaBuckConfig =
          Preconditions.checkNotNull(ScalaLibraryBuilder.this.scalaBuckConfig);

      return new ScalacToJarStepFactory(
          scalaBuckConfig.getScalac(buildRuleResolver),
          buildRuleResolver.getRule(scalaBuckConfig.getScalaLibraryTarget()),
          scalaBuckConfig.getCompilerFlags(),
          extraArguments,
          buildRuleResolver.getAllRules(scalaBuckConfig.getCompilerPlugins()),
          getJavac(),
          Preconditions.checkNotNull(getJavacOptions()));
    }
  }
}
