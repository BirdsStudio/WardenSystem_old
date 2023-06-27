package glorydark.wardensystem.reports.matters;

public interface Report {
    boolean isAnonymous();

    String getInfo();

    String getPlayer();

    long getMillis();

}
