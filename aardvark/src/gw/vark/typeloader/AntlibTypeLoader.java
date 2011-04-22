package gw.vark.typeloader;

import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeLoader;
import gw.lang.reflect.TypeLoaderBase;
import gw.lang.reflect.TypeSystem;
import gw.util.GosuExceptionUtil;
import gw.util.StreamUtil;
import gw.vark.Aardvark;
import org.apache.tools.ant.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AntlibTypeLoader extends TypeLoaderBase implements ITypeLoader{
  private static final String GW_VARK_TASKS_PACKAGE = "gw.vark.antlibs.";
  private static final String ANT_ANTLIB_SYMBOL = "Ant";
  private static final String ANT_ANTLIB_RESOURCE = "org/apache/tools/ant/taskdefs/defaults.properties";
  private static final String IVY_ANTLIB_SYMBOL = "Ivy";
  private static final String IVY_ANTLIB_RESOURCE = "org/apache/ivy/ant/antlib.xml";
  private static final String ANTLIB_DEFINE_TOKEN = "#antlib";

  private HashMap<String, IType> _types = new HashMap<String, IType>();

  public AntlibTypeLoader(File varkFile) {
    LinkedHashMap<String, String> antlibs = new LinkedHashMap<String, String>();
    antlibs.put(ANT_ANTLIB_SYMBOL, ANT_ANTLIB_RESOURCE);
    antlibs.put(IVY_ANTLIB_SYMBOL, IVY_ANTLIB_RESOURCE);
    scanForUserAntlibs(varkFile, antlibs);
    for (Map.Entry<String, String> antlib : antlibs.entrySet()) {
      addAntlib(antlib.getKey(), antlib.getValue());
    }
  }

  @Override
  public IType getType(String fullyQualifiedName) {
    if (fullyQualifiedName.startsWith(GW_VARK_TASKS_PACKAGE)) {
      String name = fullyQualifiedName.substring(GW_VARK_TASKS_PACKAGE.length());
      return _types.get(name);
    } else {
      return null;
    }
  }

  @Override
  public Set<? extends CharSequence> getAllTypeNames() {
    return _types.keySet();
  }

  @Override
  public List<String> getHandledPrefixes() {
    return Collections.emptyList();
  }

  private static void scanForUserAntlibs(File varkFile, Map<String, String> antlibs) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(varkFile));
      while (true) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        if (line.contains(ANTLIB_DEFINE_TOKEN)) {
          String[] antlibDef = line.substring(line.indexOf(ANTLIB_DEFINE_TOKEN) + ANTLIB_DEFINE_TOKEN.length()).trim().split("\\s");
          if (antlibDef.length != 2) {
            Aardvark.getProject().log("Bad syntax for antlib definition" +
                    ", expecting \"" + ANTLIB_DEFINE_TOKEN + " symbol antlibresource\"" +
                    ", e.g. \"" + ANTLIB_DEFINE_TOKEN + " Ivy org/apache/ivy/ant/antlib.xml\"",
                    Project.MSG_ERR);
          }
          if (antlibs.containsKey(antlibDef[0])) {
            Aardvark.getProject().log("Overriding previously defined antlib symbol, " + antlibDef[0], Project.MSG_WARN);
          }
          antlibs.put(antlibDef[0], antlibDef[1]);
        }
      }
    } catch (IOException e) {
      throw GosuExceptionUtil.forceThrow(e);
    } finally {
      try {
        StreamUtil.close(reader);
      } catch (IOException e) { }
    }
  }

  private void addAntlib(String antlibName, String resourceName) {
    String typeName = GW_VARK_TASKS_PACKAGE + antlibName;
    _types.put(typeName, TypeSystem.getOrCreateTypeReference(new AntlibType(typeName, resourceName, this)));
  }

}
