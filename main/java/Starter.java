import jp.ac.washi.quinte.api.GameOption;
import jp.ac.washi.quinte.api.GameStarter;
import jp.ac.washi.quinte.api.Player;

public class Starter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Player p1 = new Ningengasinu();
		Player p2 = new HighScorePlayer();
		Player p3 = new RandomPlayer();
		Player p4 = new RandomPlayer();

		GameOption option = new GameOption();
		option.setEnableSound(true);
		// setEnableThreadToLimitTime(false) はスレッドを使わないため、デバッグしやすい環境になります
		option.setEnableThreadToLimitTime(true);
		option.setFps(30);
		GameStarter.start(p1, p2, p3, p4, option);
	}
}
