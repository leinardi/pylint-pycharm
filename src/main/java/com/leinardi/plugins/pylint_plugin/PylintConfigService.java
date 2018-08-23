package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "PylintConfigService",
        storages = {
                @Storage("PylintConfig.xml")}
)
public class PylintConfigService implements PersistentStateComponent<PylintConfigService> {

    private String executableName;
    private String pathSuffix;

    public String getExecutableName() {
        return executableName;
    }

    public void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }

    public void setPathSuffix(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    @Nullable
    @Override
    public PylintConfigService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PylintConfigService config) {
        XmlSerializerUtil.copyBean(config, this);
    }

    @Nullable
    public static PylintConfigService getInstance(Project project) {
        return ServiceManager.getService(project, PylintConfigService.class);
    }
}
