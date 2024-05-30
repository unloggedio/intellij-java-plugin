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

    public RemoteRobotController(RemoteRobot remoteRobot, Keyboard keyboard) {
        this.remoteRobot = remoteRobot;
        this.keyboard = keyboard;
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
        waitFor(ofMinutes(10), () -> !ideaFrameReference.isDumbMode());
    }
}
