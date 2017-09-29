package com.facebook.buck.verilog;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.io.BuildCellRelativePath;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.rules.AbstractBuildRuleWithDeclaredAndExtraDeps;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.ExplicitBuildTargetSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.facebook.buck.step.Step;
import com.facebook.buck.verilog.VerilogBuckConfig;
import com.facebook.buck.verilog.VerilogDesignLibraryCreateStep;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;

public class VerilogLibrary extends AbstractBuildRuleWithDeclaredAndExtraDeps {

  private final VerilogBuckConfig verilogBuckConfig;
  private final Path output;

  public VerilogLibrary(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params,
      VerilogBuckConfig verilogBuckConfig) {
    super(buildTarget, projectFilesystem, params);
    this.verilogBuckConfig = verilogBuckConfig;
    this.output = BuildTargets.getGenPath(getProjectFilesystem(), getBuildTarget(), "%s");
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    BuildCellRelativePath parent =
        BuildCellRelativePath.fromCellRelativePath(
            context.getBuildCellRootPath(),
            getProjectFilesystem(),
            output.getParent());

    return ImmutableList.<Step>builder()
        .addAll(MakeCleanDirectoryStep.of(parent))
        .add(new VerilogDesignLibraryCreateStep(
            verilogBuckConfig.getTool("vlib").getCommandPrefix(context.getSourcePathResolver()),
            getBuildTarget().getShortName(),
            parent.getPathRelativeToBuildCellRoot())) // work dir
        .build();
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return new ExplicitBuildTargetSourcePath(getBuildTarget(), output);
  }
}
