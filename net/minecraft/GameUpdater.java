package net.minecraft;

import java.applet.Applet;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

public class GameUpdater
  implements Runnable
{
  public int percentage;
  public int currentSizeDownload;
  public int totalSizeDownload;
  public int currentSizeExtract;
  public int totalSizeExtract;
  protected URL[] urlList;
  private static ClassLoader classLoader;
  protected Thread loaderThread;
  public boolean fatalError;
  public String fatalErrorDescription;
  protected String subtaskMessage = "";
  protected int state = 1;

  protected boolean lzmaSupported = false;
  protected boolean pack200Supported = false;

  protected boolean certificateRefused;

  protected static boolean natives_loaded = false;
  public static boolean forceUpdate = false;
  String lwjgl;
  String jinput;
  String lwjgl_util;
  String minecraft;
  boolean online;

  public GameUpdater(String lwjgl, String jinput, String lwjgl_util, String minecraft, boolean online)
  {
    this.lwjgl = lwjgl;
    this.jinput = jinput;
    this.lwjgl_util = lwjgl_util;
    this.minecraft = minecraft;
    this.online = online;
  }

  public void init() {
    state = 1;
    try
    {
      Class.forName("LZMA.LzmaInputStream");
      lzmaSupported = true;
    }
    catch (Throwable localThrowable) {
    }
    try {
      Pack200.class.getSimpleName();
      pack200Supported = true;
    } catch (Throwable localThrowable1) {
    }
  }

 protected String getDescriptionForState()
  {
    switch (state) {
    case 1:
      return "Loader Betöltése...";
    case 2:
      return "Letölteni kívánt csomagok meghatározása...";
    case 3:
      return "Gyorsítótár elleneőrtése a megévő fájlokhoz";
    case 4:
      return "Csomagok letöltése...";
    case 5:
      return "Letöltött csomagok kibontása...";
    case 6:
      return "Classpath frissítése...";
    case 7:
      return "Applet csere...";
    case 8:
      return "Valós applet meghatározása...";
    case 9:
      return "Valós applet indítása...";
    case 10:
      return "Letöltés kész.";
    }
    return "Ismeretlen státusz....";
  }

  protected String trimExtensionByCapabilities(String file)
  {
    if (!pack200Supported) {
      file = file.replaceAll(".pack", "");
    }

    if (!lzmaSupported) {
      file = file.replaceAll(".lzma", "");
    }
    return file;
  }

  protected void loadJarURLs() throws Exception {
    state = 2;

    StringTokenizer jar = new StringTokenizer(trimExtensionByCapabilities("lwjgl.jar, jinput.jar, lwjgl_util.jar, minecraft.jar"), ", ");
    int jarCount = jar.countTokens() + 1;

    urlList = new URL[jarCount];

    URL path = new URL("http://rockcraft.clans.hu/update/");

    for (int i = 0; i < jarCount - 1; i++) {
      urlList[i] = new URL(path, jar.nextToken());
    }

    String osName = System.getProperty("os.name");
    String nativeJar = null;

    if (osName.startsWith("Win"))
      nativeJar = "windows_natives.jar.lzma";
    else if (osName.startsWith("Linux"))
      nativeJar = "linux_natives.jar.lzma";
    else if (osName.startsWith("Mac"))
      nativeJar = "macosx_natives.jar.lzma";
    else if ((osName.startsWith("Solaris")) || (osName.startsWith("SunOS")))
      nativeJar = "solaris_natives.jar.lzma";
    else {
      fatalErrorOccured("Operációs renszer (" + osName + ") nem támogatott.", null);
    }

    if (nativeJar == null) {
      fatalErrorOccured("lwjgl fájlok nem taláhatóak", null);
    } else {
      nativeJar = trimExtensionByCapabilities(nativeJar);
      urlList[(jarCount - 1)] = new URL(path, nativeJar);
    }
  }

  public void run()
  {
    init();
    state = 3;

    percentage = 5;
    try
    {
      loadJarURLs();

      String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws Exception {
          return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
        }
      });
      File dir = new File(path);

      if (!dir.exists()) {
        dir.mkdirs();
      }

      if(online) {
        boolean shoudupdate = false;
        if (forceUpdate) {
          shoudupdate = true;
        }
        if ((new File(path + "lwjgl.jar")).exists() || lwjgl.indexOf(getMD5Checksum(path + "lwjgl.jar")) == -1) {
              shoudupdate = true;
        }
        if ((new File(path + "jinput.jar")).exists() || jinput.indexOf(getMD5Checksum(path + "jinput.jar")) == -1) {
              shoudupdate = true;
        }
        if ((new File(path + "lwjgl_util.jar")).exists() || lwjgl_util.indexOf(getMD5Checksum(path + "lwjgl_util.jar")) == -1) {
               shoudupdate = true;
        }
        if ((new File(path + "minecraft.jar")).exists() || minecraft.indexOf(getMD5Checksum(path + "minecraft.jar")) == -1) {
               shoudupdate = true;
        }
        if (shoudupdate) {
              downloadJars(path);
              extractJars(path);
              extractNatives(path);
        }
      }

      updateClassPath(dir);
      state = 10;
    } catch (AccessControlException ace) {
      fatalErrorOccured(ace.getMessage(), ace);
      certificateRefused = true;
    } catch (Exception e) {
      fatalErrorOccured(e.getMessage(), e);
    } finally {
      loaderThread = null;
    }
  }

  public static byte[] createChecksum(String filename) throws Exception {
           InputStream fis =  new FileInputStream(filename);

           byte[] buffer = new byte[1024];
           MessageDigest complete = MessageDigest.getInstance("MD5");
           int numRead;

           do {
               numRead = fis.read(buffer);
               if (numRead > 0) {
                   complete.update(buffer, 0, numRead);
               }
           } while (numRead != -1);

           fis.close();
           return complete.digest();
  }

  public static String getMD5Checksum(String filename) throws Exception {
           byte[] b = createChecksum(filename);
           String result = "";

           for (int i=0; i < b.length; i++) {
               result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
           }
           return result;
   }

  protected void updateClassPath(File dir)
    throws Exception
  {
    state = 6;

    percentage = 95;

    URL[] urls = new URL[urlList.length];
    for (int i = 0; i < urlList.length; i++) {
      urls[i] = new File(dir, getJarName(urlList[i])).toURI().toURL();
    }

    if (classLoader == null)
      classLoader = new URLClassLoader(urls) {
        protected PermissionCollection getPermissions(CodeSource codesource) {
          PermissionCollection perms = null;
          try
          {
            Method method = SecureClassLoader.class.getDeclaredMethod("getPermissions", new Class[] { CodeSource.class });
            method.setAccessible(true);
            perms = (PermissionCollection)method.invoke(getClass().getClassLoader(), new Object[] { codesource });

            String host = "minecraft.net";

            if ((host != null) && (host.length() > 0))
            {
              perms.add(new SocketPermission(host, "connect,accept"));
            } else codesource.getLocation().getProtocol().equals("file");

            perms.add(new FilePermission("<<ALL FILES>>", "read"));
          }
          catch (Exception e) {
            e.printStackTrace();
          }

          return perms;
        }
      };
    String path = dir.getAbsolutePath();
    if (!path.endsWith(File.separator)) path = path + File.separator;
    unloadNatives(path);

    System.setProperty("org.lwjgl.librarypath", path + "natives");
    System.setProperty("net.java.games.input.librarypath", path + "natives");

    natives_loaded = true;
  }

  private void unloadNatives(String nativePath)
  {
    if (!natives_loaded) {
      return;
    }
    try
    {
      Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
      field.setAccessible(true);
      Vector libs = (Vector)field.get(getClass().getClassLoader());

      String path = new File(nativePath).getCanonicalPath();

      for (int i = 0; i < libs.size(); i++) {
        String s = (String)libs.get(i);

        if (s.startsWith(path)) {
          libs.remove(i);
          i--;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException
  {
    Class appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
    return (Applet)appletClass.newInstance();
  }

  protected void downloadJars(String path)
    throws Exception
  {
    state = 4;

    int[] fileSizes = new int[urlList.length];

    for (int i = 0; i < urlList.length; i++) {
      URLConnection urlconnection = urlList[i].openConnection();
      urlconnection.setDefaultUseCaches(false);
      if ((urlconnection instanceof HttpURLConnection)) {
        ((HttpURLConnection)urlconnection).setRequestMethod("HEAD");
      }
      fileSizes[i] = urlconnection.getContentLength();
      totalSizeDownload += fileSizes[i];
    }

    int initialPercentage = this.percentage = 10;

    byte[] buffer = new byte[65536];
    for (int i = 0; i < urlList.length; i++)
    {
      int unsuccessfulAttempts = 0;
      int maxUnsuccessfulAttempts = 3;
      boolean downloadFile = true;

      while (downloadFile) {
        downloadFile = false;

        URLConnection urlconnection = urlList[i].openConnection();

        if ((urlconnection instanceof HttpURLConnection)) {
          urlconnection.setRequestProperty("Cache-Control", "no-cache");
          urlconnection.connect();
        }

        String currentFile = getFileName(urlList[i]);
        InputStream inputstream = getJarInputStream(currentFile, urlconnection);
        FileOutputStream fos = new FileOutputStream(path + currentFile);

        long downloadStartTime = System.currentTimeMillis();
        int downloadedAmount = 0;
        int fileSize = 0;
        String downloadSpeedMessage = "";
        int bufferSize;
        while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
        {
          fos.write(buffer, 0, bufferSize);
          currentSizeDownload += bufferSize;
          fileSize += bufferSize;
          percentage = (initialPercentage + currentSizeDownload * 45 / totalSizeDownload);
          subtaskMessage = ("Folyamatban: " + currentFile + " " + currentSizeDownload * 100 / totalSizeDownload + "%");

          downloadedAmount += bufferSize;
          long timeLapse = System.currentTimeMillis() - downloadStartTime;

          if (timeLapse >= 1000L)
          {
            float downloadSpeed = downloadedAmount / (float)timeLapse;

            downloadSpeed = (int)(downloadSpeed * 100.0F) / 100.0F;

            downloadSpeedMessage = "   " + downloadSpeed + " KB/másodperc";

            downloadedAmount = 0;

            downloadStartTime += 1000L;
          }

          subtaskMessage += downloadSpeedMessage;
        }

        inputstream.close();
        fos.close();

        if ((!(urlconnection instanceof HttpURLConnection)) || (fileSize == fileSizes[i]) ||
          (fileSizes[i] <= 0))
        {
          continue;
        }
        unsuccessfulAttempts++;

        if (unsuccessfulAttempts < maxUnsuccessfulAttempts) {
          downloadFile = true;
          currentSizeDownload -= fileSize;
        }
        else {
          throw new Exception("A " + currentFile + "fájlt nem sikerült letölteni!");
        }
      }

    }

    subtaskMessage = "";
  }

  protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection)
    throws Exception
  {
    final InputStream[] is = new InputStream[1];

    for (int j = 0; (j < 3) && (is[0] == null); j++) {
      Thread t = new Thread() {
        public void run() {
          try {
            is[0] = urlconnection.getInputStream();
          }
          catch (IOException localIOException)
          {
          }
        }
      };
      t.setName("JarInputStreamThread");
      t.start();

      int iterationCount = 0;
      while ((is[0] == null) && (iterationCount++ < 5)) {
        try {
          t.join(1000L);
        }
        catch (InterruptedException localInterruptedException)
        {
        }
      }
      if (is[0] != null) continue;
      try {
        t.interrupt();
        t.join();
      }
      catch (InterruptedException localInterruptedException1)
      {
      }
    }

    if (is[0] == null) {
      throw new Exception("A " + currentFile + " fájlt nem sikerült letölteni!");
    }

    return is[0];
  }

  protected void extractLZMA(String in, String out)
    throws Exception
  {
    File f = new File(in);
    FileInputStream fileInputHandle = new FileInputStream(f);

    Class clazz = Class.forName("LZMA.LzmaInputStream");
    Constructor constructor = clazz.getDeclaredConstructor(new Class[] { InputStream.class });
    InputStream inputHandle = (InputStream)constructor.newInstance(new Object[] { fileInputHandle });

    OutputStream outputHandle = new FileOutputStream(out);

    byte[] buffer = new byte[16384];

    int ret = inputHandle.read(buffer);
    while (ret >= 1) {
      outputHandle.write(buffer, 0, ret);
      ret = inputHandle.read(buffer);
    }

    inputHandle.close();
    outputHandle.close();

    outputHandle = null;
    inputHandle = null;

    f.delete();
  }

  protected void extractPack(String in, String out)
    throws Exception
  {
    File f = new File(in);
    if (!f.exists()) return;

    FileOutputStream fostream = new FileOutputStream(out);
    JarOutputStream jostream = new JarOutputStream(fostream);

    Pack200.Unpacker unpacker = Pack200.newUnpacker();
    unpacker.unpack(f, jostream);
    jostream.close();

    f.delete();
  }

  protected void extractJars(String path)
    throws Exception
  {
    state = 5;

    float increment = 10.0F / urlList.length;

    for (int i = 0; i < urlList.length; i++) {
      percentage = (55 + (int)(increment * (i + 1)));
      String filename = getFileName(urlList[i]);

      if (filename.endsWith(".pack.lzma")) {
        subtaskMessage = ("A \"" + filename + "\" nevű fájl kicsomagolása \"" + filename.replaceAll(".lzma", "") + "\"-kénet.");
        extractLZMA(path + filename, path + filename.replaceAll(".lzma", ""));

        subtaskMessage = ("A \"" + filename.replaceAll(".lzma", "") + "\" nevű fájl kicsomagolása \""  + filename.replaceAll(".pack.lzma", "") + "\"-kénet.");
        extractPack(path + filename.replaceAll(".lzma", ""), path + filename.replaceAll(".pack.lzma", ""));
      } else if (filename.endsWith(".pack")) {
        subtaskMessage = ("A \"" + filename + "\" nevű fájl kicsomagolása \"" + filename.replace(".pack", "") + "\"-kénet.");
        extractPack(path + filename, path + filename.replace(".pack", ""));
      } else if (filename.endsWith(".lzma")) {
        subtaskMessage = ("A \"" + filename + "\" nevű fájl kicsomagolása \"" + filename.replace(".lzma", "") + "\"-kénet.");
        extractLZMA(path + filename, path + filename.replace(".lzma", ""));
      }
    }
  }

  protected void extractNatives(String path) throws Exception
  {
    state = 5;

    int initialPercentage = percentage;

    String nativeJar = getJarName(urlList[(urlList.length - 1)]);

    Certificate[] certificate = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();

    if (certificate == null) {
      URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();

      JarURLConnection jurl = (JarURLConnection)new URL("jar:" + location.toString() + "!/net/minecraft/Launcher.class").openConnection();
      jurl.setDefaultUseCaches(true);
      try {
        certificate = jurl.getCertificates();
      }
      catch (Exception localException)
      {
      }
    }
    File nativeFolder = new File(path + "natives");
    if (!nativeFolder.exists()) {
      nativeFolder.mkdir();
    }

    File file = new File(path + nativeJar);
    if (!file.exists()) return;
    JarFile jarFile = new JarFile(file, true);
    Enumeration entities = jarFile.entries();

    totalSizeExtract = 0;

    while (entities.hasMoreElements()) {
      JarEntry entry = (JarEntry)entities.nextElement();

      if ((entry.isDirectory()) || (entry.getName().indexOf(47) != -1)) {
        continue;
      }
      totalSizeExtract = (int)(totalSizeExtract + entry.getSize());
    }

    currentSizeExtract = 0;

    entities = jarFile.entries();

    while (entities.hasMoreElements()) {
      JarEntry entry = (JarEntry)entities.nextElement();

      if ((entry.isDirectory()) || (entry.getName().indexOf(47) != -1))
      {
        continue;
      }
      File f = new File(path + "natives" + File.separator + entry.getName());
      if ((f.exists()) && (!f.delete()))
      {
        continue;
      }

      InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
      OutputStream out = new FileOutputStream(path + "natives" + File.separator + entry.getName());

      byte[] buffer = new byte[65536];
      int bufferSize;
      while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1)
      {
        out.write(buffer, 0, bufferSize);
        currentSizeExtract += bufferSize;

        percentage = (initialPercentage + currentSizeExtract * 20 / totalSizeExtract);
        subtaskMessage = ("Kibontás: " + entry.getName() + " " + currentSizeExtract * 100 / totalSizeExtract + "%");
      }

      validateCertificateChain(certificate, entry.getCertificates());

      in.close();
      out.close();
    }
    subtaskMessage = "";

    jarFile.close();

    File f = new File(path + nativeJar);
    f.delete();
  }

  protected static void validateCertificateChain(Certificate[] ownCerts, Certificate[] native_certs)
    throws Exception
  {
    if (ownCerts == null) return;
    if (native_certs == null) throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");

    if (ownCerts.length != native_certs.length) throw new Exception("Unable to validate certificate chain. Chain differs in length [" + ownCerts.length + " vs " + native_certs.length + "]");

    for (int i = 0; i < ownCerts.length; i++)
      if (!ownCerts[i].equals(native_certs[i]))
        throw new Exception("Certificate mismatch: " + ownCerts[i] + " != " + native_certs[i]);
  }

  protected String getJarName(URL url)
  {
    String fileName = url.getFile();

    if (fileName.contains("?")) {
      fileName = fileName.substring(0, fileName.indexOf("?"));
    }
    if (fileName.endsWith(".pack.lzma"))
      fileName = fileName.replaceAll(".pack.lzma", "");
    else if (fileName.endsWith(".pack"))
      fileName = fileName.replaceAll(".pack", "");
    else if (fileName.endsWith(".lzma")) {
      fileName = fileName.replaceAll(".lzma", "");
    }

    return fileName.substring(fileName.lastIndexOf(47) + 1);
  }

  protected String getFileName(URL url) {
    String fileName = url.getFile();
    if (fileName.contains("?")) {
      fileName = fileName.substring(0, fileName.indexOf("?"));
    }
    return fileName.substring(fileName.lastIndexOf(47) + 1);
  }

  protected void fatalErrorOccured(String error, Exception e) {
    e.printStackTrace();
    fatalError = true;
    fatalErrorDescription = (error);
  }

  public boolean canPlayOffline()
  {
    try
    {
      String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws Exception {
          return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
        }
      });
      File dir = new File(path);
      if (!dir.exists()) return false;

      dir = new File(dir, "minecraft.jar");
      if (!dir.exists()) {
          return false;
      } else {
          return true;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}