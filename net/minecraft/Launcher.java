package net.minecraft;

import javax.imageio.ImageIO;
import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Launcher extends Applet
  implements AppletStub
{
  private static final long serialVersionUID = 1L;
  public Map<String, String> customParameters = new HashMap();
  private GameUpdater gameUpdater;
  private boolean gameUpdaterStarted = false;
  private Applet applet;
  private Image bgImage;
  private boolean active = false;
  private int context = 0;
  private VolatileImage img;

  public boolean isActive()
  {
    if (context == 0) {
      context = -1;
      try {
        if (getAppletContext() != null) context = 1; 
      }
      catch (Exception localException) {
      }
    }
    if (context == -1) return active;
    return super.isActive();
  }

  public void init(String userName, String sessionId, String lwjgl, String jinput, String lwjgl_util, String minecraft, boolean online)
  {
    try {
      bgImage = ImageIO.read(LoginForm.class.getResource("dirt.png")).getScaledInstance(32, 32, 16);
    } catch (IOException e) {
      e.printStackTrace();
    }

    customParameters.put("username", userName);
    customParameters.put("sessionid", sessionId);
    customParameters.put("lwjgl", lwjgl);
    customParameters.put("jinput", jinput);
    customParameters.put("lwjgl_util", lwjgl_util);
    customParameters.put("minecraft", minecraft);

    gameUpdater = new GameUpdater(lwjgl, jinput, lwjgl_util, minecraft, online);
  }

  public boolean canPlayOffline(String userName) {
    customParameters.put("username", userName);
    return gameUpdater.canPlayOffline();
  }

  public void init() {
    if (applet != null) {
      applet.init();
      return;
    }
    init(getParameter("userName"), getParameter("sessionId"), getParameter("lwjgl"), getParameter("jinput"), getParameter("lwjgl_util"), getParameter("minecraft"), false);
  }

  public void start() {
    if (applet != null) {
      applet.start();
      return;
    }
    if (gameUpdaterStarted) return;

    Thread t = new Thread() {
      public void run() {
        gameUpdater.run();
        try {
          if (!gameUpdater.fatalError)
            replace(gameUpdater.createApplet());
        }
        catch (ClassNotFoundException e)
        {
          e.printStackTrace();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    };
    t.setDaemon(true);
    t.start();

    t = new Thread() {
      public void run() {
        while (applet == null) {
          repaint();
          try {
            Thread.sleep(10L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    t.setDaemon(true);
    t.start();

    gameUpdaterStarted = true;
  }

  public void stop() {
    if (applet != null) {
      active = false;
      applet.stop();
      return;
    }
  }

  public void destroy() {
    if (applet != null) {
      applet.destroy();
      return;
    }
  }

  public void replace(Applet applet) {
    this.applet = applet;
    applet.setStub(this);
    applet.setSize(getWidth(), getHeight());

    setLayout(new BorderLayout());
    add(applet, "Center");

    applet.init();
    active = true;
    applet.start();
    validate();
  }

  public void update(Graphics g)
  {
    paint(g);
  }

  public void paint(Graphics g2) {
    if (applet != null) return;

    int w = getWidth() / 2;
    int h = getHeight() / 2;
    if ((img == null) || (img.getWidth() != w) || (img.getHeight() != h)) {
      img = createVolatileImage(w, h);
    }

    Graphics g = img.getGraphics();
    for (int x = 0; x <= w / 32; x++) {
      for (int y = 0; y <= h / 32; y++)
        g.drawImage(bgImage, x * 32, y * 32, null);
    }
    g.setColor(Color.LIGHT_GRAY);

    String msg = "Minecraft Letöltése...";
    if (gameUpdater.fatalError) {
      msg = "Az indítás nem sikerült!";
    }

    g.setFont(new Font(null, 1, 20));
    FontMetrics fm = g.getFontMetrics();
    g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);

    g.setFont(new Font(null, 0, 12));
    fm = g.getFontMetrics();
    msg = gameUpdater.getDescriptionForState();
    if (gameUpdater.fatalError) {
      msg = gameUpdater.fatalErrorDescription;
    }

    g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 1);
    msg = gameUpdater.subtaskMessage;
    g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 2);

    if (!gameUpdater.fatalError) {
      g.setColor(Color.black);
      g.fillRect(64, h - 64, w - 128 + 1, 5);
      g.setColor(new Color(32768));
      g.fillRect(64, h - 64, gameUpdater.percentage * (w - 128) / 100, 4);
      g.setColor(new Color(2138144));
      g.fillRect(65, h - 64 + 1, gameUpdater.percentage * (w - 128) / 100 - 2, 1);
    }

    g.dispose();

    g2.drawImage(img, 0, 0, w * 2, h * 2, null);
  }

  public String getParameter(String name) {
    String custom = (String)customParameters.get(name);
    if (custom != null) return custom; try
    {
      return super.getParameter(name);
    } catch (Exception e) {
      customParameters.put(name, null);
    }return null;
  }

  public void appletResize(int width, int height)
  {
  }

  public URL getDocumentBase() {
    try {
      return new URL("http://localhost/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }
}