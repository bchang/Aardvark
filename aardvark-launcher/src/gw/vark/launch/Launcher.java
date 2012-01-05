/*
 * Copyright (c) 2010 Guidewire Software, Inc.
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

package gw.vark.launch;

import org.apache.tools.ant.launch.LaunchException;
import org.apache.tools.ant.launch.Locator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Launcher extends AntLauncher {

  public static final String MAIN_CLASS = "gw.vark.Aardvark";

  /**
   * Entry point for starting command line Aardvark.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    int exitCode;
    try {
      Launcher launcher = new Launcher();
      exitCode = launcher.run(args);
      launcher.cleanup();
    } catch (LaunchException e) {
      exitCode = EXIT_CODE_ERROR;
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      exitCode = EXIT_CODE_ERROR;
      t.printStackTrace(System.err);
    }
    if (exitCode != 0) {
      if (launchDiag) {
        System.out.println("Exit code: " + exitCode);
      }
      System.exit(exitCode);
    }
  }

  private PrintStream _logfile;

  @Override
  public String getHomePropertyName() {
    return "aardvark.home";
  }

  @Override
  public String getLibDirPropertyName() {
    return "aardvark.library.dir";
  }

  @Override
  public String getMainClassName() {
    return MAIN_CLASS;
  }

  @Override
  protected int readArgs(String[] args, int i) throws LaunchException {
    if (args[i].equals("-logfile")) {
      if (i == args.length - 1) {
        throw new LaunchException("The -logfile argument must be followed by an output file path");
      }
      try {
        _logfile = new PrintStream(new FileOutputStream(args[++i]));
        System.setOut(_logfile);
        System.setErr(_logfile);
      }
      catch (IOException e) {
        String msg = "Cannot write on the specified logfile " + args[i] + ". "
                + "Make sure the path exists and you have write permissions.";
        throw new LaunchException(msg);
      }
      return i;
    }
    else {
      return super.readArgs(args, i);
    }
  }

  @Override
  protected File findHomeRelativeToDir(File dir) {
    if (dir == null) {
      throw new RuntimeException("could not find aardvark home");
    }
    if (new File(dir, "bin/vark").exists()) {
      return dir;
    }
    return findHomeRelativeToDir(dir.getParentFile());
  }

  @Override
  protected URL[] getSystemURLs(File launcherDir) throws MalformedURLException {
    if (isAardvarkDev()) {
      System.out.println("aardvark.dev is on");
      List<URL> urls = new ArrayList<URL>();

      File homeDir = new File(System.getProperty(getHomePropertyName()));

      System.out.println("Using vark-compiled classes");
      File launcherJar = new File(homeDir, "launcher" + File.separatorChar + "target" + File.separatorChar + "classes");
      File aardvarkJar = new File(homeDir, "aardvark" + File.separatorChar + "target" + File.separatorChar + "classes");

      urls.add(Locator.fileToURL(launcherJar));
      urls.add(Locator.fileToURL(aardvarkJar));
      File libDir = new File(homeDir, "aardvark" + File.separatorChar + "target" + File.separatorChar + "testlib");

      if (!libDir.exists()) {
        throw new IllegalStateException(libDir + " does not exist - run 'mvn compile'");
      }
      urls.addAll(Arrays.asList(Locator.getLocationURLs(libDir)));

      return urls.toArray(new URL[urls.size()]);
    }
    else {
      return super.getSystemURLs(launcherDir);
    }
  }

  private void cleanup() {
    if (_logfile != null) {
      _logfile.flush();
      _logfile.close();
    }
  }

  private static boolean isAardvarkDev() {
    return "true".equals(System.getProperty("aardvark.dev"));
  }
}