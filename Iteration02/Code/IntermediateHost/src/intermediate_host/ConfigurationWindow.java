package intermediate_host;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import javax.swing.Box;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import java.awt.GridLayout;

public class ConfigurationWindow {

  private JFrame frame;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          ConfigurationWindow window = new ConfigurationWindow();
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public ConfigurationWindow() {
    initialize();
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    frame = new JFrame();
    frame.setBounds(100, 100, 584, 417);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
    
    JPanel rrq_panel = new JPanel();
    tabbedPane.addTab("Read Request (RRQ)", null, rrq_panel, null);
    rrq_panel.setLayout(new GridLayout(1, 0, 0, 0));
    
    JPanel panel_2 = new JPanel();
    tabbedPane.addTab("New tab", null, panel_2, null);
    
    JPanel panel_3 = new JPanel();
    tabbedPane.addTab("New tab", null, panel_3, null);
    
    JPanel panel_4 = new JPanel();
    tabbedPane.addTab("New tab", null, panel_4, null);
    
    JPanel panel_5 = new JPanel();
    tabbedPane.addTab("New tab", null, panel_5, null);
    
    JPanel panel = new JPanel();
    frame.getContentPane().add(panel, BorderLayout.SOUTH);
    
    JButton btnNewButton = new JButton("Run!");
    btnNewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      }
    });
    panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    panel.add(btnNewButton);
    
    JButton btnQuit = new JButton("Quit");
    panel.add(btnQuit);
  }

}
