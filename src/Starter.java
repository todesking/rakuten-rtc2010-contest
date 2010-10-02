import jp.ac.washi.quinte.api.GameOption;
import jp.ac.washi.quinte.api.GameStarter;
import jp.ac.washi.quinte.api.Player;

public class Starter {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Player p1 = new あなたのチーム名();
        Player p2 = new RandomPlayer();
        Player p3 = new HighScorePlayer();
        Player p4 = new SkeltonPlayer();

        GameOption option = new GameOption();
        option.setEnableSound(true);
        // setEnableThreadToLimitTime(false) はスレッドを使わないため、デバッグしやすい環境になります
        option.setEnableThreadToLimitTime(true);
        GameStarter.start(p1, p2, p3, p4, option);
    }
}
