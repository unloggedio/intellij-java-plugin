package com.insidious.plugin.UITests.wrapper;

import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.utils.Keyboard;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

public class RemoteRobotController {
    private RemoteRobot remoteRobot;
    private final Keyboard keyboard;
    private IdeaFrame ideaFrameReference;
    private OperatingSystem operatingSystem;

    public RemoteRobotController(RemoteRobot remoteRobot, Keyboard keyboard) {
        this.remoteRobot = remoteRobot;
        this.keyboard = keyboard;
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
            this.operatingSystem = OperatingSystem.WINDOWS;
            this.isWindows = true;
        } else if (osname.startsWith("Mac")) {
            this.operatingSystem = OperatingSystem.MAC;
            this.isMac = true;
        } else {
            this.operatingSystem = OperatingSystem.LINUX;
            this.isLinux = true;
        }
    }

    public RemoteRobot getRemoteRobot() {
        return remoteRobot;
    }

    public void setRemoteRobot(RemoteRobot remoteRobot) {
        this.remoteRobot = remoteRobot;
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public IdeaFrame getIdeaFrame() {
        if (ideaFrameReference == null) {
            this.ideaFrameReference = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
            waitForIndex();
        }
        return ideaFrameReference;
    }

    public void waitForIndex() {
        if (ideaFrameReference == null) {
            getIdeaFrame();
        }
        waitFor(ofMinutes(3), () -> !ideaFrameReference.isDumbMode());
    }

    public void unsetIdeaFrame() {
        this.ideaFrameReference = null;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public enum OperatingSystem {WINDOWS, MAC, LINUX}

    private boolean isWindows = false;
    private boolean isMac = false;
    private boolean isLinux = false;

    public boolean isWindows() {
        return isWindows;
    }

    public boolean isMac() {
        return isMac;
    }

    public boolean isLinux() {
        return isLinux;
    }
}
