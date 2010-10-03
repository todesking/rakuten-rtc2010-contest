import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;
import jp.ac.washi.quinte.api.TileInfo;

public class Ningengasinu extends Player {
	public Ningengasinu() {
	}

	@Override
	public CursorAction nextCursorAction(GameInfo info) {
		for (int y = 0; y < info.getMap().getSize(); y++) {
			for (int x = 0; x < info.getMap().getSize(); x++) {
				final TileInfo tile = info.getMap().getTile(Point.create(x, y));
				final char c;
				switch (tile.getType()) {
				case BIG_GATE:
					c = 'G';
					break;
				case SMALL_GATE:
					c = 'g';
					break;
				case ROAD:
					if (tile.getOwner() == info.getLeftCountry())
						c = '1';
					else if (tile.getOwner() == info.getOppositeCountry())
						c = '2';
					else if (tile.getOwner() == info.getRightCountry())
						c = '3';
					else if (tile.getOwner() == info.getMyCountry())
						c = '0';
					else if (tile.getOwner() == null)
						c = '?';
					else
						throw new AssertionError();
					break;
				case WALL:
					c = '#';
					break;
				default:
					throw new AssertionError();
				}
				System.out.print(c);
			}
			System.out.println();
		}
		return new CursorAction(RotateType.CLOCKWISE, 3, 12);
	}

	@Override
	public SoldierAction nextSoldierAction(GameInfo info) {
		return SoldierAction.NONE;
	}
}
