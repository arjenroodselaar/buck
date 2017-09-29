package com.facebook.buck.verilog;

import static org.junit.Assert.assertTrue;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargetFactory;
//import com.facebook.buck.testutil.integration.BuckBuildLog;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.TemporaryPaths;
import com.facebook.buck.testutil.integration.TestDataHelper;

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;

public class VerilogLibraryIntegrationTest {
  @Rule
  public TemporaryPaths tmp = new TemporaryPaths();

  private ProjectWorkspace workspace;

  @Test
  public void testCreateDesignLibrary() throws IOException, InterruptedException {
    workspace =
        TestDataHelper.createProjectWorkspaceForScenario(this, "project", tmp);
    workspace.setUp();

    workspace.runBuckBuild(workspace.newBuildTarget("//lib1:lib1").toString()).assertSuccess();
    assertTrue(Files.exists(workspace.resolve("buck-out/gen/lib1/lib1/_info")));
  }
}
