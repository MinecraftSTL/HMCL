package org.jackhuang.hmcl.ui.main;

import com.jfoenix.controls.JFXButton;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.stage.FileChooser;
import org.jackhuang.hmcl.game.GameJavaVersion;
import org.jackhuang.hmcl.java.JavaManager;
import org.jackhuang.hmcl.java.JavaRuntime;
import org.jackhuang.hmcl.setting.ConfigHolder;
import org.jackhuang.hmcl.setting.DownloadProviders;
import org.jackhuang.hmcl.task.Schedulers;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.*;
import org.jackhuang.hmcl.ui.construct.MessageDialogPane;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/**
 * @author Glavo
 */
public final class JavaManagementPage extends ListPageBase<JavaItem> {

    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Collection<JavaRuntime>> listener;

    public JavaManagementPage() {
        this.listener = FXUtils.onWeakChangeAndOperate(JavaManager.getAllJavaProperty(), this::loadJava);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JavaPageSkin(this);
    }

    public void onAddJava() {
        FileChooser chooser = new FileChooser();
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java", "java.exe"));
        chooser.setTitle(i18n("settings.game.java_directory.choose"));
        File file = chooser.showOpenDialog(Controllers.getStage());
        if (file != null) {
            try {
                Path path = file.toPath().toRealPath();
                Task.supplyAsync("Get Java", () -> JavaManager.getJava(path))
                        .whenComplete(Schedulers.javafx(), ((result, exception) -> {
                            if (result != null && JavaManager.isCompatible(result.getPlatform())) {
                                String pathString = path.toString();

                                ConfigHolder.globalConfig().getDisabledJava().remove(pathString);
                                if (ConfigHolder.globalConfig().getUserJava().add(pathString)) {
                                    JavaManager.addJava(result);
                                }
                            } else {
                                Controllers.dialog(i18n("java.add.failed"), i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                            }
                        })).start();
            } catch (IOException ignored) {
            }
        }
    }

    public void onInstallJava() {
        Controllers.dialog(new JavaDownloadDialog(DownloadProviders.getDownloadProvider(), GameJavaVersion.getSupportedVersions(Platform.SYSTEM_PLATFORM)));
    }

    // FXThread
    private void loadJava(Collection<JavaRuntime> javaRuntimes) {
        if (javaRuntimes != null) {
            List<JavaItem> items = new ArrayList<>();
            for (JavaRuntime java : javaRuntimes) {
                items.add(new JavaItem(java));
            }
            this.setItems(FXCollections.observableList(items));
            this.setLoading(false);
        } else
            this.setLoading(true);
    }

    private static final class JavaPageSkin extends ToolbarListPageSkin<JavaManagementPage> {

        JavaPageSkin(JavaManagementPage skinnable) {
            super(skinnable);
        }

        @Override
        protected List<Node> initializeToolbar(JavaManagementPage skinnable) {
            JFXButton refreshButton = createToolbarButton2(i18n("button.refresh"), SVG.REFRESH, JavaManager::refresh);
            JFXButton downloadButton = createToolbarButton2(i18n("java.download"), SVG.DOWNLOAD_OUTLINE, skinnable::onInstallJava);
            if (GameJavaVersion.getSupportedVersions(Platform.SYSTEM_PLATFORM).isEmpty())
                downloadButton.setDisable(true);
            JFXButton addJavaButton = createToolbarButton2(i18n("java.add"), SVG.PLUS, skinnable::onAddJava);

            return Arrays.asList(refreshButton, downloadButton, addJavaButton);
        }
    }
}
