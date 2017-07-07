package net.ahri.trafficlight;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;

public class TrafficLight
{
    private static final char RED = '/';
    private static final char ORANGE = '~';
    private static final char GREEN = 'o';
    private static final Runtime RUNTIME = Runtime.getRuntime();

    public static void main(String[] args) throws Exception
    {
        if (!SystemTray.isSupported())
        {
            System.err.println("System tray is not supported");
            System.exit(1);
        }

        if (args.length < 4 || (args.length % 4) != 0)
        {
            System.err.println("Usage: task_name_1 cmd_1 delay_1 click_cmd_1[ task_name_n cmd_n delay_n click_cmd_n...]");
            System.exit(1);
        }

        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final PopupMenu trayPopupMenu = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(
                defaultToolkit.getImage(TrafficLight.class.getResource("/orange.png")),
                "Traffic Light",
                trayPopupMenu
        );
        final Updater updater = new Updater(
                trayIcon,
                defaultToolkit.getImage(TrafficLight.class.getResource("/red.png")),
                defaultToolkit.getImage(TrafficLight.class.getResource("/green.png"))
        );
        trayIcon.setImageAutoSize(true);

        for (int i = 0; i < args.length; i += 4)
        {
            new Task(args[i], args[i+1], Integer.parseInt(args[i+2]), args[i+3], trayPopupMenu, updater.tracker());
        }

        final MenuItem close = new MenuItem("Close");
        close.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });

        trayPopupMenu.add(close);

        try
        {
            SystemTray.getSystemTray().add(trayIcon);
        }
        catch (AWTException awtException)
        {
            awtException.printStackTrace();
        }
    }

    private static class Task
    {
        public enum Status
        {
            RED,
            ORANGE,
            GREEN;
        }

        public Task(final String name, final String cmd, int delayS, final String clickCmd, PopupMenu trayPopupMenu, final Updater.Tracker tracker)
        {
            final int delayMs = delayS * 1000;

            final MenuItem action = new MenuItem(taskLabel(name, Status.ORANGE));
            action.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent ev)
                {
                    if (clickCmd.isEmpty())
                    {
                        return;
                    }

                    exec(clickCmd);
                }
            });

            trayPopupMenu.add(action);

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    while (true)
                    {
                        action.setLabel(taskLabel(name, Status.ORANGE));

                        final Status status = exec(cmd) == 0
                                ? Status.GREEN
                                : Status.RED;

                        action.setLabel(taskLabel(name, status));
                        tracker.track(status);

                        sleep(delayMs);
                    }
                }
            }).start();
        }

        private static String taskLabel(String name, Status status)
        {
            char c = RED;
            switch (status)
            {
                case RED:
                    c = RED;
                    break;
                case ORANGE:
                    c = ORANGE;
                    break;
                case GREEN:
                    c = GREEN;
                    break;
            }

            return String.format("[%c] %s", c, name);
        }
    }

    private static class Updater
    {
        private final TrayIcon trayIcon;
        private final Image red;
        private final Image green;
        private final Map<Tracker, Task.Status> trackers = new IdentityHashMap<Tracker, Task.Status>();

        public Updater(TrayIcon trayIcon, Image red, Image green)
        {
            this.trayIcon = trayIcon;
            this.red = red;
            this.green = green;
        }

        public Tracker tracker()
        {
            final Tracker tracker = new Tracker();
            trackers.put(tracker, Task.Status.ORANGE);
            return tracker;
        }

        private class Tracker
        {
            public void track(Task.Status state)
            {
                synchronized (trackers)
                {
                    trackers.put(this, state);
                    updateIcon();
                }
            }
        }

        private void updateIcon()
        {
            Image newIcon = green;

            for (Task.Status status : trackers.values())
            {
                if (status == Task.Status.RED)
                {
                    newIcon = red;
                }
            }

            if (trayIcon.getImage() != newIcon)
            {
                trayIcon.setImage(newIcon);
            }
        }
    }

    private static int exec(String cmd)
    {
        try
        {
            Process process = RUNTIME.exec(cmd);
            process.waitFor();

            if (process.exitValue() == 0)
            {
                return 0;
            }

            System.err.println("Error(" + process.exitValue() + ") from command \"" + cmd);
            final InputStream stdout = process.getInputStream();
            final InputStream stderr = process.getErrorStream();

            int size = 0;
            byte[] buffer = new byte[1024];

            boolean writtenStdOutHeader = false;
            while ((size = stdout.read(buffer)) != -1)
            {
                if (!writtenStdOutHeader)
                {
                    System.err.print("STDOUT: ");
                    writtenStdOutHeader = true;
                }

                System.err.write(buffer, 0, size);
            }

            if (writtenStdOutHeader)
            {
                System.err.println();
            }

            boolean writtenStdErrHeader = false;
            while ((size = stderr.read(buffer)) != -1)
            {
                if (!writtenStdErrHeader)
                {
                    System.err.print("STDERR: ");
                    writtenStdErrHeader = true;
                }

                System.err.write(buffer, 0, size);
            }

            if (writtenStdErrHeader)
            {
                System.err.println();
            }

            return process.exitValue();
        }
        catch (Exception e)
        {
            System.err.println("Exception: " + e.getMessage());
            System.err.println("    executing command: " + cmd);
        }

        return 1;
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignored)
        {
        }
    }
}