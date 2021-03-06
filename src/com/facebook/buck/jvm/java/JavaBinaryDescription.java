/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.buck.jvm.java;

import com.facebook.buck.cxx.toolchain.CxxPlatform;
import com.facebook.buck.cxx.toolchain.CxxPlatforms;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.model.FlavorDomain;
import com.facebook.buck.model.InternalFlavor;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.CommonDescriptionArg;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.HasDeclaredDeps;
import com.facebook.buck.rules.HasTests;
import com.facebook.buck.rules.ImplicitDepsInferringDescription;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathRuleFinder;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.util.immutables.BuckStyleImmutable;
import com.facebook.buck.versions.VersionRoot;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import org.immutables.value.Value;

public class JavaBinaryDescription
    implements Description<JavaBinaryDescriptionArg>,
        ImplicitDepsInferringDescription<JavaBinaryDescription.AbstractJavaBinaryDescriptionArg>,
        VersionRoot<JavaBinaryDescriptionArg> {

  private static final Flavor FAT_JAR_INNER_JAR_FLAVOR = InternalFlavor.of("inner-jar");

  private final JavaOptions javaOptions;
  private final JavacOptions javacOptions;
  private final JavaBuckConfig javaBuckConfig;
  private final CxxPlatform defaultCxxPlatform;
  private final FlavorDomain<CxxPlatform> cxxPlatforms;

  public JavaBinaryDescription(
      JavaOptions javaOptions,
      JavacOptions javacOptions,
      JavaBuckConfig javaBuckConfig,
      CxxPlatform defaultCxxPlatform,
      FlavorDomain<CxxPlatform> cxxPlatforms) {
    this.javaOptions = javaOptions;
    this.javacOptions = javacOptions;
    this.javaBuckConfig = javaBuckConfig;
    this.defaultCxxPlatform = defaultCxxPlatform;
    this.cxxPlatforms = cxxPlatforms;
  }

  @Override
  public Class<JavaBinaryDescriptionArg> getConstructorArgType() {
    return JavaBinaryDescriptionArg.class;
  }

  private CxxPlatform getCxxPlatform(AbstractJavaBinaryDescriptionArg args) {
    return args.getDefaultCxxPlatform().map(cxxPlatforms::getValue).orElse(defaultCxxPlatform);
  }

  @Override
  public BuildRule createBuildRule(
      TargetGraph targetGraph,
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      BuildRuleResolver resolver,
      CellPathResolver cellRoots,
      JavaBinaryDescriptionArg args) {

    SourcePathRuleFinder ruleFinder = new SourcePathRuleFinder(resolver);
    ImmutableMap<String, SourcePath> nativeLibraries =
        JavaLibraryRules.getNativeLibraries(params.getBuildDeps(), getCxxPlatform(args));
    BuildTarget binaryBuildTarget = buildTarget;
    BuildRuleParams binaryParams = params;

    // If we're packaging native libraries, we'll build the binary JAR in a separate rule and
    // package it into the final fat JAR, so adjust it's params to use a flavored target.
    if (!nativeLibraries.isEmpty()) {
      binaryBuildTarget = binaryBuildTarget.withAppendedFlavors(FAT_JAR_INNER_JAR_FLAVOR);
    }

    // Construct the build rule to build the binary JAR.
    ImmutableSet<JavaLibrary> transitiveClasspathDeps =
        JavaLibraryClasspathProvider.getClasspathDeps(binaryParams.getBuildDeps());
    ImmutableSet<SourcePath> transitiveClasspaths =
        JavaLibraryClasspathProvider.getClasspathsFromLibraries(transitiveClasspathDeps);
    BuildRule rule =
        new JavaBinary(
            binaryBuildTarget,
            projectFilesystem,
            binaryParams.copyAppendingExtraDeps(transitiveClasspathDeps),
            javaOptions.getJavaRuntimeLauncher(),
            args.getMainClass().orElse(null),
            args.getManifestFile().orElse(null),
            args.getMergeManifests().orElse(true),
            args.getDisallowAllDuplicates().orElse(false),
            args.getMetaInfDirectory().orElse(null),
            args.getBlacklist(),
            transitiveClasspathDeps,
            transitiveClasspaths,
            javaBuckConfig.shouldCacheBinaries());

    // If we're packaging native libraries, construct the rule to build the fat JAR, which packages
    // up the original binary JAR and any required native libraries.
    if (!nativeLibraries.isEmpty()) {
      BuildRule innerJarRule = rule;
      resolver.addToIndex(innerJarRule);
      SourcePath innerJar = innerJarRule.getSourcePathToOutput();
      rule =
          new JarFattener(
              buildTarget,
              projectFilesystem,
              params.copyAppendingExtraDeps(
                  Suppliers.<Iterable<BuildRule>>ofInstance(
                      ruleFinder.filterBuildRuleInputs(
                          ImmutableList.<SourcePath>builder()
                              .add(innerJar)
                              .addAll(nativeLibraries.values())
                              .build()))),
              JavacFactory.create(ruleFinder, javaBuckConfig, null),
              javacOptions,
              innerJar,
              nativeLibraries,
              javaOptions.getJavaRuntimeLauncher());
    }

    return rule;
  }

  @Override
  public void findDepsForTargetFromConstructorArgs(
      BuildTarget buildTarget,
      CellPathResolver cellRoots,
      AbstractJavaBinaryDescriptionArg constructorArg,
      ImmutableCollection.Builder<BuildTarget> extraDepsBuilder,
      ImmutableCollection.Builder<BuildTarget> targetGraphOnlyDepsBuilder) {
    extraDepsBuilder.addAll(CxxPlatforms.getParseTimeDeps(getCxxPlatform(constructorArg)));
  }

  @VisibleForTesting
  public CxxPlatform getDefaultCxxPlatform() {
    return defaultCxxPlatform;
  }

  @Override
  public boolean isVersionRoot(ImmutableSet<Flavor> flavors) {
    return true;
  }

  @BuckStyleImmutable
  @Value.Immutable
  interface AbstractJavaBinaryDescriptionArg
      extends CommonDescriptionArg, HasDeclaredDeps, HasTests {
    Optional<String> getMainClass();

    Optional<SourcePath> getManifestFile();

    Optional<Boolean> getMergeManifests();

    Optional<Boolean> getDisallowAllDuplicates();

    Optional<Path> getMetaInfDirectory();

    ImmutableSet<Pattern> getBlacklist();

    Optional<Flavor> getDefaultCxxPlatform();
  }
}
