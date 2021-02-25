package simpleputty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/*
 * 簡易putty管理介面
 */
public class SimplePutty extends JFrame {
  private static final long serialVersionUID = 801502755990692239L;

  private String version = "0.1.0";
  /** 遠端設定. */
  private ArrayList<JsonNode> hostConfigList;

  private JList<String> hostList;
  private DefaultListModel<String> listModel;
  private JTextArea detailsText;
  private TextField commandLine;

  /** 初始化. */
  public void initialize() {
    initGui();
    hostConfigList = loadServersConfig();

    listModel.removeAllElements();

    hostConfigList
        .stream()
        .forEach(
            node -> {
              listModel.addElement(node.get("DisplayName").asText());
            });
    if (hostConfigList.size() > 0) {
      hostList.setSelectedIndex(0);
      selectHost(hostConfigList.get(0));
    }
  }

  private void initGui() {
    setTitle(version);
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout(5, 0));

    setSize(480, 350);

    // 畫面置中
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    double x = (1 - (screen.getHeight() / getHeight())) / 2;
    double y = (1 - (screen.getWidth() / getWidth())) / 2;
    setLocation((int) x, (int) y);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    listModel = new DefaultListModel<>();
    hostList = new JList<>(listModel);
    JScrollPane scroll = new JScrollPane(hostList);
    contentPane.add(scroll, BorderLayout.WEST);

    MouseListener mouseListener =
        new MouseAdapter() {
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
              int index = hostList.locationToIndex(e.getPoint());
              selectHost(hostConfigList.get(index));
            } else if (e.getClickCount() == 2) {
              int index = hostList.locationToIndex(e.getPoint());

              doRemoteConnect(hostConfigList.get(index));
            }
          }
        };
    hostList.addMouseListener(mouseListener);

    detailsText = new JTextArea();
    JScrollPane scrollText =
        new JScrollPane(
            detailsText,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    contentPane.add(scrollText, BorderLayout.CENTER);

    Panel panel = new Panel(new BorderLayout(1, 0));
    commandLine = new TextField();
    panel.add(commandLine, BorderLayout.CENTER);

    Button btn = new Button("連線");
    btn.addActionListener(
        event -> {
          if (hostConfigList.size() > 0) {
            doRemoteConnect(hostConfigList.get(hostList.getSelectedIndex()));
          }
        });
    panel.add(btn, BorderLayout.EAST);
    contentPane.add(panel, BorderLayout.SOUTH);
  }

  private ArrayList<JsonNode> loadServersConfig() {
    ArrayList<JsonNode> list = new ArrayList<>();
    try {
      byte[] configFileBytes = Files.readAllBytes(Paths.get("config/putty.json"));
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(configFileBytes);

      node.get("Putty")
          .get("Node")
          .iterator()
          .forEachRemaining(
              json -> {
                list.add(json);
              });

    } catch (IOException e) {
      e.printStackTrace();
    }
    return list;
  }

  private void selectHost(JsonNode node) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
      detailsText.setText(json);

      String sshCommand =
          String.format(
              "ssh %s@%s", node.get("UserName").asText(), node.get("ServerName").asText());
      commandLine.setText(sshCommand);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private void doRemoteConnect(JsonNode node) {
    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection data = new StringSelection(commandLine.getText());

    systemClipboard.setContents(data, data);

    String commands = "config/openTerminal.sh";
    Runtime rt = Runtime.getRuntime();

    @SuppressWarnings("unused")
    Process process = null;

    try {
      process = rt.exec(commands);
    } catch (IOException e) {
      e.printStackTrace();
      if (e.getMessage().contains("Permission denied")) {
        try {
          process = rt.exec("chmod 757 " + commands);
          process = rt.exec(commands);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }

    try {
      TimeUnit.MILLISECONDS.sleep(450);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      Robot robot = new Robot();
      robot.keyPress(KeyEvent.VK_META);
      robot.delay(500);
      robot.keyPress(KeyEvent.VK_V);
      robot.keyRelease(KeyEvent.VK_V);
      robot.keyRelease(KeyEvent.VK_META);

      robot.delay(300);
      robot.keyPress(KeyEvent.VK_ENTER);
      robot.keyRelease(KeyEvent.VK_ENTER);
    } catch (java.awt.AWTException awte) {
      System.out.println("AWTException");
    }
  }

  /**
   * 支援putty的xml轉為此app使用的json格式.
   *
   * @param puttyXmlPath xml檔案相對路徑
   * @return
   */
  public static String xmlToJson(String puttyXmlPath) throws IOException {
    byte[] configFileBytes = Files.readAllBytes(Paths.get(puttyXmlPath));
    XmlMapper xmlMapper = new XmlMapper();
    JsonNode node = xmlMapper.readTree(configFileBytes);
    String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);

    return json;
  }

  /**
   * main.
   *
   * @param args command line參數
   */
  public static void main(String[] args) {
    if (args.length == 1) {
      try {
        String json = SimplePutty.xmlToJson(args[0]);

        System.out.println(json);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    SimplePutty app = new SimplePutty();

    app.initialize();
    app.setVisible(true);
  }
}
