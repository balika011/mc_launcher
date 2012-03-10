package net.minecraft;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.util.Random;

public class LoginForm extends TransparentPanel
{
  private static final long serialVersionUID = 1L;
  private static final Color LINK_COLOR = new Color(8421631);

  private JTextField userName = new JTextField(20);
  private JPasswordField password = new JPasswordField(20);
  private TransparentCheckbox rememberBox = new TransparentCheckbox("Jelszó megjegyzése.");
  private TransparentButton launchButton = new TransparentButton("Belépés");
  private TransparentButton optionsButton = new TransparentButton("Beállítások");
  private TransparentButton retryButton = new TransparentButton("Újra");
  private TransparentButton offlineButton = new TransparentButton("Offline játék");
  private TransparentLabel errorLabel = new TransparentLabel("", 0);
  private LauncherFrame launcherFrame;
  private boolean outdated = false;
  private JScrollPane scrollPane;

  public LoginForm(final LauncherFrame launcherFrame)
  {
    this.launcherFrame = launcherFrame;

    userName.setBackground(Color.BLACK);
    password.setBackground(Color.BLACK);
    userName.setForeground(Color.WHITE);
    password.setForeground(Color.WHITE);
    userName.setCaretColor(Color.WHITE);
    password.setCaretColor(Color.WHITE);

    BorderLayout gbl = new BorderLayout();
    setLayout(gbl);

    add(buildMainLoginPanel(), "Center");

    readdata();

    retryButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        errorLabel.setText("");
        removeAll();
        add(LoginForm.this.buildMainLoginPanel(), "Center");
        validate();
        launchButton.setText("Belépés");
        launchButton.setEnabled(true);
      }
    });
    offlineButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        launcherFrame.playCached(userName.getText());
      }
    });

  /*launchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setLoggingIn();
        new Thread() {
          public void run() {
            try {
              launcherFrame.login(userName.getText(), new String(password.getPassword()));
            } catch (Exception e) {
              setError(e.toString());
            }
          }
        }
        .start();
      }
    }); */

  launchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setLoggingIn();
        launcherFrame.login(userName.getText(), new String(password.getPassword()));
      } } );

  optionsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        new OptionsPanel(launcherFrame).setVisible(true);
      } } );
  }

  private void readdata() {
    try {
      File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");

      Cipher cipher = getCipher(2, "balika011");
      DataInputStream dis;
      if (cipher != null)
        dis = new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin), cipher));
      else {
        dis = new DataInputStream(new FileInputStream(lastLogin));
      }
      userName.setText(dis.readUTF());
      password.setText(dis.readUTF());
      rememberBox.setSelected(password.getPassword().length > 0);
      dis.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void writedata() {
    try {
      File lastLogin = new File(Util.getWorkingDirectory(), "lastlogin");

      Cipher cipher = getCipher(1, "balika011");
      DataOutputStream dos;
      if (cipher != null)
        dos = new DataOutputStream(new CipherOutputStream(new FileOutputStream(lastLogin), cipher));
      else {
        dos = new DataOutputStream(new FileOutputStream(lastLogin));
      }
      dos.writeUTF(userName.getText());
      dos.writeUTF(rememberBox.isSelected() ? new String(password.getPassword()) : "");
      dos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Cipher getCipher(int mode, String password) throws Exception {
    Random random = new Random(43287234L);
    byte[] salt = new byte[8];
    random.nextBytes(salt);
    PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);

    SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
    Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
    cipher.init(mode, pbeKey, pbeParamSpec);
    return cipher;
  }

  private JScrollPane getUpdateNews()
  {
    if (scrollPane != null) return scrollPane;
    try
    {
      final JTextPane editorPane = new JTextPane()
      {
        private static final long serialVersionUID = 1L;
      };
      editorPane.setText("Hírek betöltése...");
      editorPane.addHyperlinkListener(new HyperlinkListener() {
        public void hyperlinkUpdate(HyperlinkEvent he) {
          if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            try {
              Desktop.getDesktop().browse(he.getURL().toURI());
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
      });
      new Thread() {
        public void run() {
          try {
            editorPane.setPage(new URL("http://rockcraft.clans.hu/login/news.php"));
          } catch (Exception e) {
            e.printStackTrace();
            editorPane.setContentType("text/html");
            editorPane.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center>Nem lehetett frissíteni a híreket!</center></font></body></html>");
          }
        }
      }
      .start();
      editorPane.setBackground(Color.BLACK);
      editorPane.setForeground(Color.WHITE);
      editorPane.setEditable(false);
      scrollPane = new JScrollPane(editorPane);
      scrollPane.setBorder(null);
      editorPane.setMargin(null);

      scrollPane.setBorder(new MatteBorder(0, 0, 2, 0, Color.BLACK));
    } catch (Exception e2) {
      e2.printStackTrace();
    }

    return scrollPane;
  }

  private JPanel buildMainLoginPanel() {
    JPanel p = new TransparentPanel(new BorderLayout());
    p.add(getUpdateNews(), "Center");

    JPanel southPanel = new TexturedPanel();
    southPanel.setLayout(new BorderLayout());
    southPanel.add(new LogoPanel(), "West");
    southPanel.add(new TransparentPanel(), "Center");
    southPanel.add(center(buildLoginPanel()), "East");
    southPanel.setPreferredSize(new Dimension(100, 100));

    p.add(southPanel, "South");
    return p;
  }

  private JPanel buildLoginPanel() {
    TransparentPanel panel = new TransparentPanel();
    panel.setInsets(4, 0, 4, 0);

    BorderLayout layout = new BorderLayout();
    layout.setHgap(0);
    layout.setVgap(8);
    panel.setLayout(layout);

    GridLayout gl1 = new GridLayout(0, 1);
    gl1.setVgap(2);
    GridLayout gl2 = new GridLayout(0, 1);
    gl2.setVgap(2);
    GridLayout gl3 = new GridLayout(0, 1);
    gl3.setVgap(2);

    TransparentPanel titles = new TransparentPanel(gl1);
    TransparentPanel values = new TransparentPanel(gl2);

    titles.add(new TransparentLabel("Felhasználónév:", 4));
    titles.add(new TransparentLabel("Jelszó:", 4));
    titles.add(new TransparentLabel("Kliens by balika011", 4));

    values.add(userName);
    values.add(password);
    values.add(rememberBox);

    panel.add(titles, "West");
    panel.add(values, "Center");

    TransparentPanel loginPanel = new TransparentPanel(new BorderLayout());

    TransparentPanel third = new TransparentPanel(gl3);
    titles.setInsets(0, 0, 0, 4);
    third.setInsets(0, 10, 0, 10);
    try
    {
      if (outdated) {
        TransparentLabel accountLink = getUpdateLink();
        third.add(accountLink);
      }
    }
    catch (Error localError)
    {
    }

    third.add(launchButton);
    third.add(optionsButton);

    loginPanel.add(third, "Center");
    panel.add(loginPanel, "East");

    errorLabel.setFont(new Font(null, 2, 16));
    errorLabel.setForeground(new Color(16728128));
    errorLabel.setText("");
    panel.add(errorLabel, "North");

    return panel;
  }

  private TransparentLabel getUpdateLink() {
    TransparentLabel accountLink = new TransparentLabel("Frissítened kell a launcher-t!") {
      private static final long serialVersionUID = 0L;

      public void paint(Graphics g) { super.paint(g);

        int x = 0;
        int y = 0;

        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(getText());
        int height = fm.getHeight();

        if (getAlignmentX() == 2.0F) x = 0;
        else if (getAlignmentX() == 0.0F) x = getBounds().width / 2 - width / 2;
        else if (getAlignmentX() == 4.0F) x = getBounds().width - width;
        y = getBounds().height / 2 + height / 2 - 1;

        g.drawLine(x + 2, y, x + width - 2, y); }

      public void update(Graphics g)
      {
        paint(g);
      }
    };
    accountLink.setCursor(Cursor.getPredefinedCursor(12));
    accountLink.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent arg0) {
        try {
          Desktop.getDesktop().browse(new URL("http://www.minecraft.net/download.jsp").toURI());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    accountLink.setForeground(LINK_COLOR);
    return accountLink;
  }

  private JPanel buildMainOfflinePanel() {
    JPanel p = new TransparentPanel(new BorderLayout());
    p.add(getUpdateNews(), "Center");

    JPanel southPanel = new TexturedPanel();
    southPanel.setLayout(new BorderLayout());
    southPanel.add(new LogoPanel(), "West");
    southPanel.add(new TransparentPanel(), "Center");
    southPanel.add(center(buildOfflinePanel()), "East");
    southPanel.setPreferredSize(new Dimension(100, 100));

    p.add(southPanel, "South");
    return p;
  }

  private Component center(Component c) {
    TransparentPanel tp = new TransparentPanel(new GridBagLayout());
    tp.add(c);
    return tp;
  }

  private TransparentPanel buildOfflinePanel()
  {
    TransparentPanel panel = new TransparentPanel();
    panel.setInsets(0, 0, 0, 20);

    BorderLayout layout = new BorderLayout();
    panel.setLayout(layout);

    TransparentPanel loginPanel = new TransparentPanel(new BorderLayout());

    GridLayout gl = new GridLayout(0, 1);
    gl.setVgap(2);
    TransparentPanel pp = new TransparentPanel(gl);
    pp.setInsets(0, 8, 0, 0);

    pp.add(retryButton);
    pp.add(offlineButton);

    loginPanel.add(pp, "East");

    boolean canPlayOffline = launcherFrame.canPlayOffline(userName.getText());
    offlineButton.setEnabled(canPlayOffline);
    if (!canPlayOffline) {
      loginPanel.add(new TransparentLabel("(Még nem töltötted le a Minecraftot!)", 4), "South");
    }
    panel.add(loginPanel, "Center");

    TransparentPanel p2 = new TransparentPanel(new GridLayout(0, 1));
    errorLabel.setFont(new Font(null, 2, 16));
    errorLabel.setForeground(new Color(16728128));
    p2.add(errorLabel);
    if (outdated) {
      TransparentLabel accountLink = getUpdateLink();
      p2.add(accountLink);
    }

    loginPanel.add(p2, "Center");

    return panel;
  }

  public void setError(String errorMessage) {
    removeAll();
    add(buildMainLoginPanel(), "Center");
    errorLabel.setText(errorMessage);
    validate();
  }

  public void loginOk() {
    writedata();
  }

  public void setLoggingIn() {
    launchButton.setText("Belépés...");
    launchButton.setEnabled(false);
  }

  public void setNoNetwork() {
    removeAll();
    add(buildMainOfflinePanel(), "Center");
    validate();
  }

  public void setOutdated()
  {
    outdated = true;
  }
}