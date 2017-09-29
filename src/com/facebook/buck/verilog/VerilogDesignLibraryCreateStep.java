package com.facebook.buck.verilog;

import com.facebook.buck.shell.ShellStep;
import com.facebook.buck.step.ExecutionContext;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;

public class VerilogDesignLibraryCreateStep extends ShellStep {

  private final ImmutableList<String> designLibraryToolCommandPrefix;
  private final String libraryName;

  public VerilogDesignLibraryCreateStep(
      ImmutableList<String> designLibraryToolCommandPrefix,
      String libraryName,
      Path output) {
    super(output);
    this.designLibraryToolCommandPrefix = designLibraryToolCommandPrefix;
    this.libraryName = libraryName;
  }

  @Override
  protected ImmutableList<String> getShellCommandInternal(ExecutionContext context) {
    return ImmutableList.<String>builder()
        .addAll(designLibraryToolCommandPrefix)
        .add(libraryName)
        .build();
  }

  @Override
  public String getShortName() {
    return "create_design_library";
  }
}
