package net.minecraft;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Util
{
  private static File workDir = null;

  public static File getWorkingDirectory() {
    if (workDir == null) workDir = getWorkingDirectory("rockcraft");
    return workDir;
  }

  public static File getWorkingDirectory(String applicationName) {
    String userHome = System.getProperty("user.home", ".");
    File workingDirectory;
    switch (getPlatform().ordinal()) {
    case 1:
    case 2:
     String applicationData = System.getenv("APPDATA");
      if (applicationData != null) workingDirectory = new File(applicationData, "." + applicationName + '/'); else
        workingDirectory = new File(userHome, '.' + applicationName + '/');
      break;
    case 3:
      String applicationData1 = System.getenv("APPDATA");
      if (applicationData1 != null) workingDirectory = new File(applicationData1, "." + applicationName + '/'); else
        workingDirectory = new File(userHome, '.' + applicationName + '/');
      break;
    case 4:
      workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
      break;
    default:
      workingDirectory = new File(userHome, applicationName + '/');
    }
    if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) throw new RuntimeException("The working directory could not be created: " + workingDirectory);
    return workingDirectory;
  }

  private static OS getPlatform() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) return OS.windows;
    if (osName.contains("mac")) return OS.macos;
    if (osName.contains("solaris")) return OS.solaris;
    if (osName.contains("sunos")) return OS.solaris;
    if (osName.contains("linux")) return OS.linux;
    if (osName.contains("unix")) return OS.linux;
    return OS.unknown;
  }

  public static String excutePost(String targetURL, String urlParameters)
  {
    HttpURLConnection connection = null;
    try
    {
      URL url = new URL(targetURL);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
      connection.setRequestProperty("Content-Language", "en-US");

      connection.setUseCaches(false);
      connection.setDoInput(true);
      connection.setDoOutput(true);

      connection.connect();

      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.writeBytes(urlParameters);
      wr.flush();
      wr.close();

      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));

      StringBuffer response = new StringBuffer();
      String line;
      while ((line = rd.readLine()) != null)
      {
        response.append(line);
        response.append('\r');
      }
      rd.close();

      String str1 = response.toString();
      return str1;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
    finally
    {
      if (connection != null)
        connection.disconnect();
    }
  }

  private static enum OS
  {
    linux, solaris, windows, macos, unknown;
  }
}