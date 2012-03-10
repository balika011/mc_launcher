package net.minecraft;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URLEncoder;

public class LauncherFrame extends Frame
{
  public static final int VERSION = 1;
  private static final long serialVersionUID = 1L;
  private Launcher launcher;
  private static LoginForm loginForm;

  public LauncherFrame()
  {
    super("RockCraft By balika011");

    setBackground(Color.BLACK);
    loginForm = new LoginForm(this);
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    p.add(loginForm, "Center");

    p.setPreferredSize(new Dimension(854, 480));

    setLayout(new BorderLayout());
    add(p, "Center");

    pack();
    setLocationRelativeTo(null);
    try
    {
      setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
    } catch (IOException e1) {
      e1.printStackTrace();
    }

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent arg0) {
        new Thread() {
          public void run() {
            try {
              Thread.sleep(30000L);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            System.out.println("FORCING EXIT!");
            System.exit(0);
          }
        }
        .start();
        if (launcher != null) {
          launcher.stop();
          launcher.destroy();
        }
        System.exit(0);
      } } );
  }

  public void playCached(String userName) {
    try {
      launcher = new Launcher();
      launcher.customParameters.put("userName", userName);
      launcher.init();
      removeAll();
      add(launcher, "Center");
      validate();
      loginForm.loginOk();
      launcher.start();
      loginForm = null;
      setTitle("RockCraft By balika011 - OFFLINE");
    } catch (Exception e) {
      e.printStackTrace();
      showError(e.toString());
    }
  }

  public void login(String userName, String password) {
    try {
        String parameters = "user=" + URLEncoder.encode(userName, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&version=" + VERSION;
        String result = Util.excutePost("http://rockcraft.clans.hu/login/login.php",parameters);
        if (result == null) {
        showError("Nem sikerült a csatlakozás a minecraft.net-hez");
        loginForm.setNoNetwork();
        return;
      }
      if (!result.contains(":")) {
        if (result.trim().equals("Bad login")) {
          showError("Sikertelen belépés");
        } else if (result.trim().equals("Old version")) {
          loginForm.setOutdated();
          showError("Lejárt launcher");
        } else if (result.trim().equals("Database error")) {
          showError("Adatbázis hiba");
        } else {
          showError(result);
        }
        loginForm.setNoNetwork();
        return;
      }
      String[] values = result.split(":");

      launcher = new Launcher();
      launcher.customParameters.put("userName", values[0].trim());
      launcher.customParameters.put("sessionId", values[1].trim());
      launcher.customParameters.put("lwjgl", values[2].trim());
      launcher.customParameters.put("jinput", values[3].trim());
      launcher.customParameters.put("lwjgl_util", values[4].trim());
      launcher.customParameters.put("minecraft", values[5].trim());
      launcher.init();

      removeAll();
      add(launcher, "Center");
      validate();
      launcher.start();
      loginForm.loginOk();
      loginForm = null;
      setTitle("RockCraft By balika011");
    } catch (Exception e) {
      e.printStackTrace();
      showError(e.toString());
      loginForm.setNoNetwork();
    }
  }

  private void showError(String error) {
    removeAll();
    add(loginForm);
    loginForm.setError(error);
    validate();
  }

  public boolean canPlayOffline(String userName) {
    Launcher launcher = new Launcher();
    launcher.init(userName, null, null, null, null, null, true);
    return launcher.canPlayOffline(userName);
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception localException) {
    }
    LauncherFrame launcherFrame = new LauncherFrame();
    launcherFrame.setVisible(true);
  }
}