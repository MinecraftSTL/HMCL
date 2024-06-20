package org.jackhuang.hmcl.java;

import org.jackhuang.hmcl.download.ArtifactMalformedException;
import org.jackhuang.hmcl.download.DownloadProvider;
import org.jackhuang.hmcl.download.java.JavaDownloadTask;
import org.jackhuang.hmcl.download.java.RemoteFiles;
import org.jackhuang.hmcl.game.DownloadInfo;
import org.jackhuang.hmcl.game.GameJavaVersion;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * @author Glavo
 */
public final class HMCLJavaRepository implements JavaRepository {
    private final Path root;

    public HMCLJavaRepository(Path root) {
        this.root = root;
    }

    public Path getPlatformRoot(Platform platform) {
        return root.resolve(platform.toString());
    }

    public Path getJavaDir(Platform platform, String name) {
        return getPlatformRoot(platform).resolve(name);
    }

    public Path getManifestFile(Platform platform, String name) {
        return getPlatformRoot(platform).resolve(name + ".json");
    }

    @Override
    public Collection<JavaRuntime> getAllJava(Platform platform) {
        Path root = getPlatformRoot(platform);
        if (!Files.isDirectory(root))
            return Collections.emptyList();

        ArrayList<JavaRuntime> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path file : stream) {
                try {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".json") && Files.isRegularFile(file)) {
                        Path javaDir = file.resolveSibling(name.substring(0, name.length() - ".json".length()));
                        Path executable;
                        try {
                            executable = JavaManager.getExecutable(javaDir).toRealPath();
                        } catch (IOException e) {
                            if (platform.getOperatingSystem() == OperatingSystem.OSX)
                                executable = JavaManager.getMacExecutable(javaDir).toRealPath();
                            else
                                throw e;
                        }

                        if (Files.isDirectory(javaDir)) {
                            JavaManifest manifest;
                            try (InputStream input = Files.newInputStream(file)) {
                                manifest = JsonUtils.fromJsonFully(input, JavaManifest.class);
                            }

                            list.add(JavaRuntime.of(executable, manifest.getInfo(), true));
                        }
                    }
                } catch (Throwable e) {
                    LOG.warning("Failed to parse " + file, e);
                }
            }

        } catch (IOException ignored) {
        }
        return list;
    }

    @Override
    public Task<JavaRuntime> getInstallJavaTask(DownloadProvider downloadProvider, Platform platform, GameJavaVersion gameJavaVersion) {
        Path javaDir = getJavaDir(platform, gameJavaVersion.getComponent());

        return new JavaDownloadTask(downloadProvider, javaDir, gameJavaVersion, JavaManager.getJavaPlatform(platform))
                .thenApplyAsync(result -> {
                    Path executable;
                    try {
                        executable = JavaManager.getExecutable(javaDir).toRealPath();
                    } catch (IOException e) {
                        if (platform.getOperatingSystem() == OperatingSystem.OSX)
                            executable = JavaManager.getMacExecutable(javaDir).toRealPath();
                        else
                            throw e;
                    }

                    JavaInfo info;
                    if (JavaManager.isCompatible(platform)) {
                        info = JavaInfo.fromExecutable(executable, false);
                        if (info == null)
                            throw new ArtifactMalformedException("Unable to read Java information");
                    } else
                        info = new JavaInfo(platform, result.download.getVersion().getName(), null);

                    Map<String, Object> update = new LinkedHashMap<>();
                    update.put("provider", "mojang");
                    update.put("component", gameJavaVersion.getComponent());

                    Map<String, JavaLocalFiles.Local> files = new LinkedHashMap<>();
                    result.remoteFiles.getFiles().forEach((path, file) -> {
                        if (file instanceof RemoteFiles.RemoteFile) {
                            DownloadInfo downloadInfo = ((RemoteFiles.RemoteFile) file).getDownloads().get("raw");
                            if (downloadInfo != null) {
                                files.put(path, new JavaLocalFiles.LocalFile(downloadInfo.getSha1(), downloadInfo.getSize()));
                            }
                        } else if (file instanceof RemoteFiles.RemoteDirectory) {
                            files.put(path, new JavaLocalFiles.LocalDirectory());
                        } else if (file instanceof RemoteFiles.RemoteLink) {
                            files.put(path, new JavaLocalFiles.LocalLink(((RemoteFiles.RemoteLink) file).getTarget()));
                        }
                    });

                    JavaManifest manifest = new JavaManifest(info, update, files);
                    FileUtils.writeText(getManifestFile(platform, gameJavaVersion.getComponent()), JsonUtils.GSON.toJson(manifest));
                    return JavaRuntime.of(executable, info, true);
                });
    }

    @Override
    public Task<Void> getUninstallJavaTask(JavaRuntime java) {
        return Task.runAsync(() -> {
            Path root = getPlatformRoot(java.getPlatform());
            Path relativized = root.relativize(java.getBinary());

            if (relativized.getNameCount() > 1) {
                String name = relativized.getName(0).toString();
                Files.deleteIfExists(getManifestFile(java.getPlatform(), name));
                FileUtils.deleteDirectory(getJavaDir(java.getPlatform(), name).toFile());
            }
        });
    }
}