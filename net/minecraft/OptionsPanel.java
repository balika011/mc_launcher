package net.minecraft;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class OptionsPanel extends JDialog
{
  private static final long serialVersionUID = 1L;

  public OptionsPanel(Window parent)
  {
    super(parent);

    setModal(true);

    setTitle("Beállítások");
    JPanel panel = new JPanel(new BorderLayout());
    JLabel label = new JLabel("Launcher beállítások", 0);
    label.setBorder(new EmptyBorder(0, 0, 16, 0));
    label.setFont(new Font("Default", 1, 16));
    panel.add(label, "North");

    JPanel optionsPanel = new JPanel(new BorderLayout());
    JPanel labelPanel = new JPanel(new GridLayout(0, 1));
    JPanel fieldPanel = new JPanel(new GridLayout(0, 1));
    optionsPanel.add(labelPanel, "West");
    optionsPanel.add(fieldPanel, "Center");



    final JButton forceButton = new JButton("Erőszakos frissítés!");
    forceButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        GameUpdater.forceUpdate = true;
        forceButton.setText("Frissítve lesz!");
        forceButton.setEnabled(false);
      }
    });
    labelPanel.add(new JLabel("Erőszakos frissítés: ", 4));
    fieldPanel.add(forceButton);

    labelPanel.add(new JLabel("Játék helye: ", 4));
    TransparentLabel dirLink = new TransparentLabel(Util.getWorkingDirectory().toString()) {
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
    dirLink.setCursor(Cursor.getPredefinedCursor(12));
    dirLink.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent arg0) {
        try {
          Desktop.getDesktop().browse(new URL("file://" + Util.getWorkingDirectory().getAbsolutePath()).toURI());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    dirLink.setForeground(new Color(2105599));

    fieldPanel.add(dirLink);

    panel.add(optionsPanel, "Center");

    JPanel buttonsPanel = new JPanel(new BorderLayout());
    buttonsPanel.add(new JPanel(), "Center");
    JButton doneButton = new JButton("Kész");
    doneButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setVisible(false);
      }
    });
    buttonsPanel.add(doneButton, "East");
    buttonsPanel.setBorder(new EmptyBorder(16, 0, 0, 0));

    panel.add(buttonsPanel, "South");

    add(panel);
    panel.setBorder(new EmptyBorder(16, 24, 24, 24));
    pack();
    setLocationRelativeTo(parent);
  }
}