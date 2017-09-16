package com.facebook.buck.verilog;

import com.facebook.buck.io.BuildCellRelativePath;
import com.facebook.buck.shell.ShellStep;
import com.facebook.buck.step.ExecutionContext;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;

public class VerilogLibraryCreateStep extends ShellStep {

  private final BuildCellRelativePath output;

  public VerilogLibraryCreateStep(
      Path workingDirectory,
      BuildCellRelativePath output) {
    super(workingDirectory);
    this.output = output;
  }

  @Override
  protected ImmutableList<String> getShellCommandInternal(ExecutionContext context) {
    Path absolutePath =
        context.getBuildCellRootPath()
            .resolve(output.getPathRelativeToBuildCellRoot());

    return ImmutableList.<String>builder()
            .add("vlib")
            .add(absolutePath.toString())
            .build();
  }

  @Override
  public String getShortName() {
    return "vlib";
  }
}
