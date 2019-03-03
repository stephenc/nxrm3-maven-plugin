/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.maven.staging.mojo;

import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.maven.staging.test.support.StagingMavenPluginITSupport;

public class StagingDeleteIT
    extends StagingMavenPluginITSupport
{
  private static final String GROUP_ID = "test.group";

  private static final String VERSION = "1.0.0";

  private static final List<String> DEPLOY_GOALS =
      Arrays.asList(new String[]{"install", "nexus-staging:staging-deploy"});

  private static final List<String> DELETE_GOALS = Arrays.asList(new String[]{"nexus-staging:delete"});

  private String artifactId;

  private String deployTag;

  @Before
  public void setup() throws Exception {
    deployTag = randomUUID().toString();
    artifactId = randomUUID().toString();
    stagingDeployProject();
  }
  
  @After
  public void tearDown() throws Exception {
   verifier.deleteArtifacts(GROUP_ID, artifactId, VERSION);
  }

  /**
   * When no tag or empty tag is provided, a default value is read from the properties file
   * @throws Exception
   */
  @Test
  public void testStagingDeleteWithDefaultTag() throws Exception {
    assertStagingWithDeleteGoal("");
  }

  @Test
  public void testStagingDelete() throws Exception {
    assertStagingWithDeleteGoal(deployTag);
  }

  @Test
  public void failStagingDeleteOnInvalidTag() throws Exception {
    setupProject(false);

    verifier.addCliOption("-Dtag=random");

    assertStagingErrorWithDeleteGoal("Delete components was unsuccessful (404 response from server)");
  }

  @Test
  public void failStagingDeleteIfOffline() throws Exception {
    setupProject(false);

    verifier.addCliOption("-Dtag=" + deployTag);
    verifier.addCliOption("-o");

    assertStagingErrorWithDeleteGoal("Goal requires online mode for execution but Maven is currently offline");
  }

  @Test
  public void failStagingDeleteForMissingPropertiesFile() throws Exception {
    File propertiesFile = new File(projectDir.getAbsolutePath() + "/target/nexus-staging/staging/staging.properties");

    forceDelete(propertiesFile);

    setupProject(false);

    verifier.addCliOption("-Dtag=" + "");

    assertStagingErrorWithDeleteGoal("Encountered an error while accessing 'staging.tag' property from staging properties file");
  }

  private void assertStagingWithDeleteGoal(String deleteTag) throws Exception {
    setupProject(false);

    verifier.addCliOption("-Dtag=" + deleteTag);

    verifier.executeGoals(DELETE_GOALS);

    verifyComponentNotFound(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, deployTag);
  }

  private void assertStagingErrorWithDeleteGoal(String errorMessage) throws Exception {
    try {
      verifier.executeGoals(DELETE_GOALS);
      Assert.fail("Expected LifecycleExecutionException");
    }
    catch (Exception e) {
      assertThat(e.getMessage(), containsString(errorMessage));
    }
  }

  private void stagingDeployProject() throws Exception {
    setupProject(true);

    verifier.addCliOption("-Dtag=" + deployTag);

    verifier.executeGoals(DEPLOY_GOALS);
  }

  private void setupProject(boolean autoClean) throws Exception {
    initialiseVerifier(projectDir);

    verifier.setAutoclean(autoClean);

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
  }

}
