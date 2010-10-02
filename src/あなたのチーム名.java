import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;

public class あなたのチーム名 extends Player {
    public あなたのチーム名() {
    }

    @Override
    public CursorAction nextCursorAction(GameInfo info) {
        return new CursorAction(RotateType.CLOCKWISE, 3, 12);
    }

    @Override
    public SoldierAction nextSoldierAction(GameInfo info) {
        return SoldierAction.NONE;
    }
}
