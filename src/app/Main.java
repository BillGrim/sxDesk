package app;

import app.model.Action;
import app.model.Event;
import app.model.ShanXunManager;
import app.model.bean.IpConfig;
import app.pane.AboutPane;
import app.pane.RouterPane;
import app.pane.ShanXunSettingPane;
import app.utils.TextUtil;
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import res.Res;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

public class Main extends Application implements Event {
    Tab dialTab;
    Tab routerTab;
    Tab aboutTab;
    BorderPane borderPane;
    TabPane tabPane;
    Text actiontarget;
    IpConfig ipConfig;
    private TrayIcon trayIcon;

    @Override
    public void start(Stage primaryStage) throws Exception {
//        enableTray(primaryStage);
        primaryStage.setTitle("闪讯wifi助手");
        borderPane = new BorderPane();
        borderPane.setTop(getTab());
        setFootView();
        borderPane.setBottom(actiontarget);
        Scene scene = new Scene(borderPane, 300, 250);
        primaryStage.setScene(scene);
//        primaryStage.setMaxWidth(400);
//        primaryStage.setMaxHeight(250);
        primaryStage.getIcons().add(new Image(Res.class.getResourceAsStream("ic_launcher.png")));
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            SystemTray.getSystemTray().remove(trayIcon);
            Platform.exit();
        });
    }

    private void setFootView() {
        actiontarget = new Text();
        actiontarget.setText("未使用");
        actiontarget.setCache(true);
        actiontarget.setFill(Color.RED);
    }

    private TabPane getTab() {
        tabPane = new TabPane();
        dialTab = new Tab("拨号");
        dialTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                borderPane.setCenter(ShanXunSettingPane.instance(Main.this));
            }
        });
        dialTab.setClosable(false);

        tabPane.getTabs().add(dialTab);
        routerTab = new Tab("路由器配置");
        routerTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                borderPane.setCenter(RouterPane.instance(Main.this));
            }
        });
        routerTab.setClosable(false);
        tabPane.getTabs().add(routerTab);

        aboutTab = new Tab("关于");
        aboutTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                borderPane.setCenter(AboutPane.instance(Main.this));
            }
        });
        aboutTab.setClosable(false);
        tabPane.getTabs().add(aboutTab);
        return tabPane;
    }

    private void enableTray(final Stage stage) {
        PopupMenu popupMenu = new PopupMenu();
        java.awt.MenuItem openItem = new java.awt.MenuItem("Show");
        java.awt.MenuItem hideItem = new java.awt.MenuItem("Hide");
        java.awt.MenuItem quitItem = new java.awt.MenuItem("Exit");
        ActionListener acl = e -> {
            MenuItem item = (MenuItem) e.getSource();
            Platform.setImplicitExit(false);

            if (item.getLabel().equals("Exit")) {
                SystemTray.getSystemTray().remove(trayIcon);
                Platform.exit();
                return;
            }
            if (item.getLabel().equals("Show")) Platform.runLater(() -> stage.show());
            if (item.getLabel().equals("Hide")) {
                Platform.runLater(() -> stage.hide());
            }

        };
        MouseListener sj = new MouseListener() {
            public void mouseReleased(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
                Platform.setImplicitExit(false);
                if (e.getClickCount() == 2) {
                    if (stage.isShowing()) {
                        Platform.runLater(() -> stage.hide());
                    } else {
                        Platform.runLater(() -> stage.show());
                    }
                }
            }
        };

        openItem.addActionListener(acl);
        quitItem.addActionListener(acl);
        hideItem.addActionListener(acl);

        popupMenu.add(openItem);
        popupMenu.add(hideItem);
        popupMenu.add(quitItem);
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage image = ImageIO.read(Res.class.getResourceAsStream("ic_launcher.png"));
            trayIcon = new TrayIcon(image, "", popupMenu);
            trayIcon.setToolTip("闪讯wifi助手");
            tray.add(trayIcon);
            trayIcon.addMouseListener(sj);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void action(Action action) {
        switch (action) {
            case CHECKROUTER:
                setFootView("路由器配置有误，请检查");
                tabPane.getSelectionModel().select(routerTab);
                break;
            case SAVEROUTER:
                setFootView("已保存");
                tabPane.getSelectionModel().select(dialTab);
                break;
            case DIAL:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        doDial();
                    }
                }).start();
                break;
            case SENDHEART:
                break;
        }
    }

    private void doDial() {
        setFootView("正在拨号");
        if (ShanXunManager.doDial()) {
            setFootView("正在检查网络");
            String json = ShanXunManager.getIp();
            if (TextUtil.isEmpty(json)) {
                setFootView("拨号失败，请重试");
            } else {
                Gson gson = new Gson();
                ipConfig = gson.fromJson(json, IpConfig.class);
                setFootView("连接成功，IP:" + ipConfig.getIp());
                ShanXunManager.sendHeart(this, ipConfig.getIp());
            }
        } else {
            setFootView("拨号失败");
        }
    }

    @Override
    public void setFootView(String s) {
        actiontarget.setText(s);
    }

    @Override
    public void appendCount(int sendTotal, int successTotal) {
        setFootView("连接成功，IP:" + ipConfig.getIp() + " 已发送" + sendTotal + "次,成功" + successTotal + "次");
    }
}
