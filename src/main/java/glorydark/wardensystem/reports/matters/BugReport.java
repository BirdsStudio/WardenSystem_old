package glorydark.wardensystem.reports.matters;

import lombok.Data;

@Data
public class BugReport implements Report {

    public String info;

    public String player;

    public long millis;

    public boolean anonymous;

    public BugReport(String info, String player, long millis, boolean anonymous) {
        this.info = info;
        this.player = player;
        this.millis = millis;
        this.anonymous = anonymous;
    }
}
