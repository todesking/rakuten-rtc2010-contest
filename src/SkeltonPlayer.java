import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.SoldierAction;

public class SkeltonPlayer extends Player {
	@Override
	public CursorAction nextCursorAction(final GameInfo gameInfo) {
		return null;
	}

	@Override
	public SoldierAction nextSoldierAction(final GameInfo gameInfo) {
		return SoldierAction.NONE;
	}
}
