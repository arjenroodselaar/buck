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

package com.facebook.buck.intellij.ideabuck.ui;

import com.facebook.buck.intellij.ideabuck.config.BuckSettingsProvider;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Buck Setting GUI, located in "Preference > Tools > Buck". */
public class BuckSettingsUI extends JPanel {

  public static final String CUSTOMIZED_INSTALL_FLAGS_HINT =
      "input your additional install flags here: eg. --no-cache";

  private TextFieldWithBrowseButton buckPathField;
  private TextFieldWithBrowseButton adbPathField;
  private JBTextField customizedInstallSettingField;
  private JCheckBox showDebug;
  private JCheckBox enableAutoDeps;
  private JCheckBox runAfterInstall;
  private JCheckBox multiInstallMode;
  private JCheckBox uninstallBeforeInstall;
  private JCheckBox customizedInstallSetting;
  private BuckSettingsProvider optionsProvider;

  public BuckSettingsUI() {
    optionsProvider = BuckSettingsProvider.getInstance();
    init();
  }

  private void init() {
    setLayout(new BorderLayout());
    JPanel container = this;

    buckPathField = new TextFieldWithBrowseButton();
    FileChooserDescriptor buckFileChooserDescriptor =
        new FileChooserDescriptor(true, false, false, false, false, false);
    buckPathField.addBrowseFolderListener(
        "Buck Executable",
        "If unspecified, dynamically find one on your PATH",
        null,
        buckFileChooserDescriptor,
        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

    adbPathField = new TextFieldWithBrowseButton();
    FileChooserDescriptor adbFileChooserDescriptor =
        new FileChooserDescriptor(true, false, false, false, false, false);
    adbPathField.addBrowseFolderListener(
        "Adb Executable",
        "If unspecified, dynamically find one on your PATH or relative to ANDROID_SDK",
        null,
        adbFileChooserDescriptor,
        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    customizedInstallSettingField = new JBTextField();
    customizedInstallSettingField.getEmptyText().setText(CUSTOMIZED_INSTALL_FLAGS_HINT);
    customizedInstallSettingField.setEnabled(false);

    showDebug = new JCheckBox("Show debug in tool window");
    enableAutoDeps = new JCheckBox("Enable auto dependencies");
    runAfterInstall = new JCheckBox("Run after install (-r)");
    multiInstallMode = new JCheckBox("Multi-install mode (-x)");
    uninstallBeforeInstall = new JCheckBox("Uninstall before installing (-u)");
    customizedInstallSetting = new JCheckBox("Use customized install setting:  ");
    initCustomizedInstallCommandListener();

    JPanel buckSettings = new JPanel(new GridBagLayout());
    buckSettings.setBorder(IdeBorderFactory.createTitledBorder("Buck Settings", true));
    container.add(container = new JPanel(new BorderLayout()), BorderLayout.NORTH);
    container.add(buckSettings, BorderLayout.NORTH);
    final GridBagConstraints constraints =
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0);

    buckSettings.add(new JLabel("Buck Executable Path:"), constraints);
    constraints.gridx = 1;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    buckSettings.add(buckPathField, constraints);
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 1;
    buckSettings.add(new JLabel("Adb Executable Path:"), constraints);
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    buckSettings.add(adbPathField, constraints);
    constraints.gridx = 0;
    constraints.gridy = 2;
    buckSettings.add(showDebug, constraints);
    constraints.gridx = 0;
    constraints.gridy = 3;
    buckSettings.add(enableAutoDeps, constraints);

    JPanel installSettings = new JPanel(new BorderLayout());
    installSettings.setBorder(IdeBorderFactory.createTitledBorder("Buck Install Settings", true));
    container.add(container = new JPanel(new BorderLayout()), BorderLayout.SOUTH);
    container.add(installSettings, BorderLayout.NORTH);

    installSettings.add(runAfterInstall, BorderLayout.NORTH);
    installSettings.add(installSettings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);

    installSettings.add(multiInstallMode, BorderLayout.NORTH);
    installSettings.add(installSettings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);

    installSettings.add(uninstallBeforeInstall, BorderLayout.NORTH);
    installSettings.add(installSettings = new JPanel(new BorderLayout()), BorderLayout.SOUTH);

    final GridBagConstraints customConstraints =
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0);
    JPanel customizedInstallSetting = new JPanel(new GridBagLayout());
    customizedInstallSetting.add(this.customizedInstallSetting, customConstraints);
    customConstraints.gridx = 1;
    customConstraints.weightx = 1;
    customConstraints.fill = GridBagConstraints.HORIZONTAL;
    customizedInstallSetting.add(customizedInstallSettingField, customConstraints);
    installSettings.add(customizedInstallSetting, BorderLayout.NORTH);
  }

  // When displaying an empty Optional in a text field, use "".
  private String optionalToText(Optional<String> optional) {
    return optional.orElse("");
  }

  // Empty or all-whitespace text fields should be parsed as Optional.empty()
  private Optional<String> textToOptional(String text) {
    if (text == null || text.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(text);
  }

  public boolean isModified() {
    return !Comparing.equal(
            buckPathField.getText().trim(),
            optionalToText(optionsProvider.getBuckExecutableOverride()))
        || !Comparing.equal(
            adbPathField.getText().trim(),
            optionalToText(optionsProvider.getAdbExecutableOverride()))
        || optionsProvider.isRunAfterInstall() != runAfterInstall.isSelected()
        || optionsProvider.isShowDebugWindow() != showDebug.isSelected()
        || optionsProvider.isAutoDepsEnabled() != enableAutoDeps.isSelected()
        || optionsProvider.isMultiInstallMode() != multiInstallMode.isSelected()
        || optionsProvider.isUninstallBeforeInstalling() != uninstallBeforeInstall.isSelected()
        || optionsProvider.isUseCustomizedInstallSetting() != customizedInstallSetting.isSelected()
        || !optionsProvider
            .getCustomizedInstallSettingCommand()
            .equals(customizedInstallSettingField.getText());
  }

  public void apply() {
    optionsProvider.setBuckExecutableOverride(textToOptional(buckPathField.getText()));
    optionsProvider.setAdbExecutableOverride(textToOptional(adbPathField.getText()));
    optionsProvider.setShowDebugWindow(showDebug.isSelected());
    optionsProvider.setAutoDepsEnabled(enableAutoDeps.isSelected());
    optionsProvider.setRunAfterInstall(runAfterInstall.isSelected());
    optionsProvider.setMultiInstallMode(multiInstallMode.isSelected());
    optionsProvider.setUninstallBeforeInstalling(uninstallBeforeInstall.isSelected());
    optionsProvider.setUseCustomizedInstallSetting(customizedInstallSetting.isSelected());
    optionsProvider.setCustomizedInstallSettingCommand(customizedInstallSettingField.getText());
  }

  public void reset() {
    buckPathField.setText(optionsProvider.getBuckExecutableOverride().orElse(""));
    adbPathField.setText(optionsProvider.getAdbExecutableOverride().orElse(""));
    showDebug.setSelected(optionsProvider.isShowDebugWindow());
    enableAutoDeps.setSelected(optionsProvider.isAutoDepsEnabled());
    runAfterInstall.setSelected(optionsProvider.isRunAfterInstall());
    multiInstallMode.setSelected(optionsProvider.isMultiInstallMode());
    uninstallBeforeInstall.setSelected(optionsProvider.isUninstallBeforeInstalling());
    customizedInstallSetting.setSelected(optionsProvider.isUseCustomizedInstallSetting());
    customizedInstallSettingField.setText(optionsProvider.getCustomizedInstallSettingCommand());
  }

  private void initCustomizedInstallCommandListener() {
    customizedInstallSetting.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
              customizedInstallSettingField.setEnabled(true);
              runAfterInstall.setEnabled(false);
              multiInstallMode.setEnabled(false);
              uninstallBeforeInstall.setEnabled(false);
            } else {
              customizedInstallSettingField.setEnabled(false);
              runAfterInstall.setEnabled(true);
              multiInstallMode.setEnabled(true);
              uninstallBeforeInstall.setEnabled(true);
            }
          }
        });
  }
}
