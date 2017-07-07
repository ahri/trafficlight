# Traffic Light

System tray icon to run commands and show green/red depending on exit codes. Tested on Linux, Mac and Windows.

There were a few domain-specific tools that are very configurable and user-friendly, but ultimately very limited in flexibility;
I wanted to run some arbitrary commands on a regular basis and easily see whether any of them were failing. The way I've written it,
you can write scripts in any language, affording you any level of complexity. You can also execute simple commands without any hassle.

Most vaguely useful stuff on the Mac cost $$$ too, so there's that.

The only downside is that it requires Java.

## Run
Here's a demo command line that will use pings to google.com to work out whether you're online, and demonstrates how to configure the
command that's executed when a particular task is clicked in the systray menu.

Linux
```sh
java -jar trafficlight.jar "Test" "/bin/ping -c1 google.com" 10 "/usr/bin/xdg-open http://router"
```
Mac
```sh
java -jar trafficlight.jar "Test" "/bin/ping -c1 google.com" 10 "open http://router"
```
Windows
```sh
java -jar trafficlight.jar "Test" "ping -n 1 google.com" 10 "cmd /C start http://router"
```

## Build
  $ gradle fatJar
  
## The future...
I don't really like Java's sys tray support (AWT doesn't support icons in the popup menu, and a JPopupMenu is not supported by
SystemTray).
The code was all thrown together in a couple of hours with no tests.
I'd like to switch over to Haskell/GTK and provide native binaries instead of a jar.
Providing some useful scripts would be nice.
