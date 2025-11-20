/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javelit.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DeployUtils.class);

  private static final String RAILWAY_BASE = "https://railway.com/new/template/javelit-app?referralCode=NFgD4z&utm_medium=integration&utm_source=template&utm_campaign=devmode";

  @Nonnull
  static String generateRailwayDeployUrl(final @Nullable Path appPath, final @Nullable String originalUrl) {
    if (appPath == null) {
      // not implemented yet - need to implement a dedicated template for maven projects
      return "";
    }
    final String publicUrl = originalUrl != null ? originalUrl : inferGitHubTreeUrl(appPath);
    if (publicUrl == null) {
      return RAILWAY_BASE;
    }
    return RAILWAY_BASE + "&APP_URL=" + publicUrl;
  }

  /**
   * Infers the GitHub tree URL for the parent directory of the given app path.
   * Returns null if:
   * - appPath is null
   * - not in a git repository
   * - remote is not GitHub
   * - any error occurs during inference
   *
   * @param appPath The path to the app file
   * @return GitHub tree URL pointing to the folder containing the app (e.g., "https://github.com/user/repo/tree/main/src/") or null
   */
  public static @Nullable String inferGitHubTreeUrl(final @Nonnull Path appPath) {
    try {
      final Path absolutePath = appPath.toAbsolutePath();

      // Find git root
      final Path gitRoot = findGitRoot(absolutePath);
      if (gitRoot == null) {
        LOG.debug("No git repository found for path: {}", absolutePath);
        return null;
      }

      // Parse git config to get GitHub info
      final GitHubRepoInfo repoInfo = parseGitConfig(gitRoot);
      if (repoInfo == null) {
        LOG.debug("Not a GitHub repository or no remote origin found");
        return null;
      }

      // Get current branch
      final String branch = getCurrentBranch(gitRoot);

      // Compute relative path from git root to app path's parent directory
      final Path appDirectory = absolutePath.getParent();
      final Path relativePath = gitRoot.relativize(appDirectory);
      final String pathStr = relativePath.toString().replace("\\", "/");

      // Construct GitHub tree URL
      final String url = String.format("https://github.com/%s/%s/tree/%s/%s",
                                       repoInfo.owner(),
                                       repoInfo.repo(),
                                       branch,
                                       pathStr);
      LOG.debug("Inferred GitHub tree URL: {}", url);
      return url;

    } catch (Exception e) {
      LOG.warn("Failed to infer GitHub tree URL for path: {}", appPath, e);
      return "";
    }
  }

  /**
   * Walks up the directory tree to find the git root (directory containing .git folder).
   *
   * @param startPath The path to start searching from
   * @return Path to git root, or null if not found
   */
  private static Path findGitRoot(final @Nonnull Path startPath) {
    Path current = startPath.getParent();
    while (current != null) {
      final Path gitDir = current.resolve(".git");
      if (Files.exists(gitDir) && Files.isDirectory(gitDir)) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Parses .git/config to extract GitHub repository information.
   *
   * @param gitRoot The git repository root path
   * @return GitHubRepoInfo if GitHub remote found, null otherwise
   */
  private static GitHubRepoInfo parseGitConfig(final @Nonnull Path gitRoot) {
    final Path configPath = gitRoot.resolve(".git/config");
    if (!Files.exists(configPath)) {
      return null;
    }

    try {
      final List<String> lines = Files.readAllLines(configPath);
      boolean inRemoteOrigin = false;
      String remoteUrl = null;

      for (final String line : lines) {
        final String trimmed = line.trim();

        // Check for [remote "origin"] section
        if ("[remote \"origin\"]".equals(trimmed)) {
          inRemoteOrigin = true;
          continue;
        }

        // Check if we've left the remote origin section
        if (inRemoteOrigin && trimmed.startsWith("[")) {
          break;
        }

        // Extract URL from remote origin section
        if (inRemoteOrigin && trimmed.startsWith("url = ")) {
          remoteUrl = trimmed.substring(6).trim();
          break;
        }
      }

      if (remoteUrl == null || !remoteUrl.contains("github.com")) {
        return null;
      }

      // Parse GitHub URL to extract owner and repo
      // Support both HTTPS and SSH formats:
      // https://github.com/owner/repo.git
      // git@github.com:owner/repo.git
      final Pattern httpsPattern = Pattern.compile("https://github\\.com/([^/]+)/([^/\\.]+)(?:\\.git)?");
      final Pattern sshPattern = Pattern.compile("git@github\\.com:([^/]+)/([^/\\.]+)(?:\\.git)?");

      Matcher matcher = httpsPattern.matcher(remoteUrl);
      if (!matcher.find()) {
        matcher = sshPattern.matcher(remoteUrl);
        if (!matcher.find()) {
          LOG.debug("Could not parse GitHub URL: {}", remoteUrl);
          return null;
        }
      }

      final String owner = matcher.group(1);
      final String repo = matcher.group(2);

      return new GitHubRepoInfo(owner, repo, null, null);

    } catch (IOException e) {
      LOG.warn("Failed to read git config", e);
      return null;
    }
  }

  /**
   * Reads the current branch name from .git/HEAD.
   *
   * @param gitRoot The git repository root path
   * @return Branch name, or "main" as fallback
   */
  @Nonnull
  private static String getCurrentBranch(final @Nonnull Path gitRoot) {
    final Path headPath = gitRoot.resolve(".git/HEAD");
    if (!Files.exists(headPath)) {
      return "main";
    }

    try {
      final String headContent = Files.readString(headPath).trim();

      // Parse format: ref: refs/heads/branch-name
      if (headContent.startsWith("ref: refs/heads/")) {
        return headContent.substring(16);
      }

      // If HEAD is detached (contains commit SHA), use main as fallback
      LOG.debug("HEAD is detached or in unexpected format: {}", headContent);
      return "main";

    } catch (IOException e) {
      LOG.warn("Failed to read .git/HEAD", e);
      return "main";
    }
  }

  // duplicated in RemoteFileUtils but I think it's better to duplicate for the moment
  private record GitHubRepoInfo(String owner, String repo, String branch, String path) {
  }

  private DeployUtils() {
  }

}
