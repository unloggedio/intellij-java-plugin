package factory;

import ui.HorBugTable;

public class ProjectService {
    private HorBugTable bugsTable;
    private int currentLineNumber;

    public static ProjectService getInstance() {
        return InstanceHolder.instance;
    }

    public HorBugTable getHorBugTable() {
        return bugsTable;
    }

    public void setHorBugTable(HorBugTable bugsTable) {
        this.bugsTable = bugsTable;
    }

    public void setCurrentLineNumber(int currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public int getCurrentLineNumber() {
        return currentLineNumber;
    }

    private static final class InstanceHolder {
        private static final ProjectService instance = new ProjectService();
    }
}
