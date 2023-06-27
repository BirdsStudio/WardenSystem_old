package glorydark.wardensystem.reports;

import glorydark.wardensystem.reports.matters.Report;
import lombok.Data;

@Data
public class DealtReportInfo {
    public String name;

    public String warden;

    public String comment;

    public Report report;

    public DealtReportInfo(String name, String warden, String comment, Report report) {
        this.name = name;
        this.warden = warden;
        this.comment = comment;
        this.report = report;
    }
}
