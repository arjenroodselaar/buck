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
import com.facebook.buck.step.fs.RmStep;
import com.facebook.buck.step.Step;
import com.facebook.buck.verilog.VerilogLibraryCreateStep;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;

public class VerilogLibrary extends AbstractBuildRuleWithDeclaredAndExtraDeps {

  public VerilogLibrary(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleParams params) {
    super(buildTarget, projectFilesystem, params);
  }

  private Path getOutput() {
    return BuildTargets.getGenPath(getProjectFilesystem(), getBuildTarget(), "%s");
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    BuildCellRelativePath cellRelativeOutput =
        BuildCellRelativePath.fromCellRelativePath(
            context.getBuildCellRootPath(), getProjectFilesystem(), getOutput());

    return ImmutableList.<Step>builder()
        .add(RmStep.of(cellRelativeOutput))
        .add(new VerilogLibraryCreateStep(
            getProjectFilesystem().getRootPath(), // working directory
            cellRelativeOutput))
        .build();
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return new ExplicitBuildTargetSourcePath(getBuildTarget(), getOutput());
  }
}
