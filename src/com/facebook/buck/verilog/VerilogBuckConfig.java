package com.facebook.buck.verilog;

import com.facebook.buck.cli.BuckConfig;
import com.facebook.buck.rules.CommandTool;
import com.facebook.buck.rules.HashedFileTool;
import com.facebook.buck.rules.Tool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class VerilogBuckConfig {

  private static final String SECTION = "verilog";

  private final BuckConfig delegate;

  public VerilogBuckConfig(final BuckConfig delegate) {
    this.delegate = delegate;
  }

  public Tool getTool(final String name) {
    Optional<Path> toolPath = delegate.getPath(SECTION, name);
    if (!toolPath.isPresent()) {
      toolPath = Optional.of(Paths.get(name));
    }

    CommandTool.Builder builder = new CommandTool.Builder(new HashedFileTool(toolPath.get()));
    return builder.build();
  }
}
