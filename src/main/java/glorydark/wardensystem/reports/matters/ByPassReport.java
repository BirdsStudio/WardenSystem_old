package glorydark.wardensystem.reports.matters;

import lombok.Data;

@Data
public class ByPassReport implements Report {
    public String info;
    public String player;

    public String suspect;
    public long millis;

    public boolean anonymous;

    public ByPassReport(String info, String player, String suspect, long millis, boolean anonymous) {
        this.info = info;
        this.player = player;
        this.millis = millis;
        this.suspect = suspect;
        this.anonymous = anonymous;
    }
}
