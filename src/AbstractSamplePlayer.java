import java.util.ArrayList;

import jp.ac.washi.quinte.api.CountryInfo;
import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.Direction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.MapInfo;
import jp.ac.washi.quinte.api.PathSearch;
import jp.ac.washi.quinte.api.Player;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;
import jp.ac.washi.quinte.api.TileInfo;

public abstract class AbstractSamplePlayer extends Player {
	public final int MAX_SEARCH_DEPTH = 15;

	protected MapInfo map;
	protected CountryInfo myCountry;
	protected CountryInfo leftCountry;
	protected CountryInfo oppositeCountry;
	protected CountryInfo rightCountry;

	protected Point targetGatePoint;

	protected final ArrayList<Point> route = new ArrayList<Point>();
	protected Point ignorePoint = new Point(0, 0);

	protected Direction[] road = null;
	protected int iRoad = 0;
	protected Point curPoint = new Point(8, 16);
	protected Point buildingRoadPoint;

	protected Direction direction = Direction.UP;
	protected int iAction = 0;
	protected final Point actionHistory[] =
		{
			Point.create(1, 1),
			Point.create(2, 2),
			Point.create(3, 3),
			Point.create(4, 4) };
	protected Point[] gateList;

	@Override
	public CursorAction nextCursorAction(final GameInfo gameInfo) {
		initializeGameInfo(gameInfo);
		avoidInfiniteLoopAction();
		if (road == null) {
			selectTargetGate();
		}
		return buildRoadOneStep();
	}

	/**
	 * どのゲートを標的にするかを決定します。 このメソッドを書き換えることで、ゲートに関する戦略を設定できます。
	 */
	protected abstract void selectTargetGate();

	@Override
	public SoldierAction nextSoldierAction(final GameInfo gameInfo) {
		// 自分の兵士の位置を取得
		final Point soldierLocation = map.getSoldier(myCountry).getLocation();

		// ターゲットゲートが存在するかチェック
		if (targetGatePoint != null) {
			Direction[] path =
				PathSearch.getNowReachablePath(
					map,
					soldierLocation,
					targetGatePoint,
					myCountry);
			if (path.length > 0) {
				return SoldierAction.fromDirection(path[0]);
			}
		}

		// 到達可能な門へのパスを取得
		for (final Point target : getGateList(map)) {
			Direction[] path =
				PathSearch.getNowReachablePath(
					map,
					soldierLocation,
					target,
					myCountry);
			if (path.length > 0) {
				targetGatePoint = target;
				return SoldierAction.fromDirection(path[0]);
			}
		}

		// 到達可能な門がなければランダムに行動します
		final int rnd = (int) (Math.random() * 4); // 0〜3
		return SoldierAction.values()[rnd];
	}

	protected Point[] getGateList(MapInfo map) {
		if (gateList == null) {
			Point[] gateList =
				{
					map.getCenterGateLocation(leftCountry),
					map.getCenterGateLocation(oppositeCountry),
					map.getCenterGateLocation(rightCountry),
					map.getLeftGateLocation(leftCountry),
					map.getRightGateLocation(leftCountry),
					map.getLeftGateLocation(oppositeCountry),
					map.getRightGateLocation(oppositeCountry),
					map.getLeftGateLocation(rightCountry),
					map.getRightGateLocation(rightCountry) };
			this.gateList = gateList;
		}
		return gateList;
	}

	/**
	 * 目的地までの道の作成工程を1ステップ進めます。
	 */
	protected CursorAction buildRoadOneStep() {
		boolean reachedGate = selectBuildingRoadPoint();
		if (reachedGate)
			return null;

		ArrayList<Point> candidatePoints =
			searchCandidatesOfMovingTile(
				buildingRoadPoint.x,
				buildingRoadPoint.y);

		if (candidatePoints.size() == 0)
			return null;

		for (Point p : candidatePoints) {
			// 回転できない場合は次の候補へ
			if (map.canRotate(p) == false) {
				continue;
			}
			// 無限ループ回避ポイントだった場合は次の候補へ
			if (ignorePoint.equals(p)) {
				continue;
			}
			CursorAction action = moveTileOneStep(p);
			// 移動させるマスが決定したら返します
			if (action != null) {
				setHistory(p);
				return action;
			}

		}
		// 移動させるマスの候補が見つからなかった場合は、現在作成中の道をあきらめます
		road = null;
		iRoad = 0;
		route.clear();
		return null;
	}

	/**
	 * 次に自分の色のタイルを運んでくる場所を決定します。
	 * 
	 * @return 目的地に到達したらtrue, そうでない場合はfalse
	 */
	protected boolean selectBuildingRoadPoint() {
		// 自分の色を運んでくる次のマスを決める(nextPointの決定)
		while (map.getTile(buildingRoadPoint.x, buildingRoadPoint.y).getOwner() == myCountry
			|| map.getTile(buildingRoadPoint.x, buildingRoadPoint.y).isGate()) {
			curPoint = buildingRoadPoint;
			route.add(curPoint);
			if (iRoad == road.length) {
				road = null;
				iRoad = 0;
				return true;
			}
			direction = road[iRoad++];
			buildingRoadPoint = direction.moveFrom(curPoint);
		}
		return false;
	}

	/**
	 * 移動させるタイルをnextPointに近づくように移動させます。
	 * 
	 * @param p
	 *            移動させる対象となるタイルの位置座標
	 * @return 対象となるタイルをnextPointに近づくように移動させるためのCursorAction
	 */
	protected CursorAction moveTileOneStep(Point p) {
		switch (direction) {
		case DOWN:
		case UP:
			if (buildingRoadPoint.y - p.y != 0) {
				if (buildingRoadPoint.y - p.y > 0
					&& buildingRoadPoint.x - p.x > 0) {
					return moveTile(p, Direction.DOWN, Direction.LEFT);
				} else if (buildingRoadPoint.y - p.y > 0
					&& buildingRoadPoint.x - p.x < 0) {
					return moveTile(p, Direction.DOWN, Direction.RIGHT);
				} else if (buildingRoadPoint.y - p.y < 0
					&& buildingRoadPoint.x - p.x > 0) {
					return moveTile(p, Direction.UP, Direction.LEFT);
				} else {
					return moveTile(p, Direction.UP, Direction.RIGHT);
				}
			} else {
				if (buildingRoadPoint.x - p.x > 0) {
					return moveTile(p, Direction.RIGHT, direction);
				} else {
					return moveTile(p, Direction.LEFT, direction);
				}
			}

		case LEFT:
		case RIGHT:
			if (buildingRoadPoint.x - p.x != 0) {
				if (buildingRoadPoint.x - p.x > 0
					&& buildingRoadPoint.y - p.y > 0) {
					return moveTile(p, Direction.RIGHT, Direction.UP);
				} else if (buildingRoadPoint.x - p.x > 0
					&& buildingRoadPoint.y - p.y < 0) {
					return moveTile(p, Direction.RIGHT, Direction.DOWN);
				} else if (buildingRoadPoint.x - p.x < 0
					&& buildingRoadPoint.y - p.y > 0) {
					return moveTile(p, Direction.LEFT, Direction.UP);
				} else {
					return moveTile(p, Direction.LEFT, Direction.DOWN);
				}
			} else {
				if (buildingRoadPoint.y - p.y > 0) {
					return moveTile(p, Direction.DOWN, direction);
				} else {
					return moveTile(p, Direction.UP, direction);
				}
			}
		default:
			return null;
		}
	}

	/**
	 * 利便性のためゲームに関する様々な情報をインスタンス変数に記憶します。
	 * 
	 * @param gameInfo
	 *            ゲーム情報
	 */
	protected void initializeGameInfo(final GameInfo gameInfo) {
		if (map == null) {
			map = gameInfo.getMap();
			myCountry = gameInfo.getMyCountry();
			leftCountry = gameInfo.getLeftCountry();
			oppositeCountry = gameInfo.getOppositeCountry();
			rightCountry = gameInfo.getRightCountry();
		}
	}

	/**
	 * 行動履歴を元に、同じタイルを移動させようとし続けているかどうかをチェックします。
	 * 同じタイルを移動させようとし続けていると判定された場合、そのタイルを、移動させるタイルの候補に含めないようにします。
	 */
	protected void avoidInfiniteLoopAction() {
		final Point prev1 = actionHistory[(iAction + 3) % 4];
		final Point prev2 = actionHistory[(iAction + 2) % 4];
		final Point prev3 = actionHistory[(iAction + 1) % 4];
		final Point prev4 = actionHistory[iAction % 4];

		if (prev1.equals(prev2) && prev1.equals(prev3) && prev1.equals(prev4)) {
			ignorePoint = prev1;
			setHistory(new Point(0, 0));
		}
	}

	protected Point[] ps = new Point[] { new Point(0, 0), // right
		new Point(0, -1), // up
		new Point(-1, 0), // left
		new Point(0, 0) // down
		};

	public AbstractSamplePlayer() {
		super();
	}

	/**
	 * 指定したマスを移動させます。
	 * 
	 * @param p
	 *            移動させるマスの座標
	 * @param to
	 *            移動させる方向を指定します
	 * @param cursorPosition
	 *            移動させる際のカーソルの位置を指定します
	 * @return 移動に必要なPlayerActionを返します
	 */
	protected CursorAction moveTile(final Point p, final Direction to,
			final Direction cursorPosition) {
		Point targetPoint =
			p.add(ps[to.ordinal()]).add(ps[cursorPosition.ordinal()]);
		if (map.isAllOwnedInCursor(targetPoint.x, targetPoint.y)) {
			return null;
		}
		boolean clockwise =
			(to.ordinal() - cursorPosition.ordinal() + 4) % 4 == 1;
		return new CursorAction(clockwise
			? RotateType.CLOCKWISE
			: RotateType.ANTICLOCKWISE, targetPoint);
	}

	/**
	 * カーソルの行動履歴を保存します。
	 * 
	 * @param location
	 *            カーソルが回転を行った位置座標
	 */
	protected void setHistory(final Point location) {
		actionHistory[iAction % 4] = location;
		iAction++;
	}

	/**
	 * 移動させるタイルの候補を探索します。
	 * 
	 * @param x
	 *            探索の中心のx座標
	 * @param y
	 *            探索の中心のy座標
	 * @return 移動させるタイルの候補のリスト
	 */
	protected ArrayList<Point> searchCandidatesOfMovingTile(final int x,
			final int y) {
		ArrayList<Point> candidatePoints = new ArrayList<Point>();

		for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
			for (int dx = -depth; dx < depth + 1; dx++) {
				for (int dy = -depth; dy < depth + 1; dy++) {
					TileInfo tile = map.getTile(x + dx, y + dy);
					if (tile == null) {
						continue;
					}
					if (Math.abs(dx) + Math.abs(dy) != depth) {
						continue;
					}
					if (tile.isRotatable() && tile.getOwner() == myCountry) {
						if (!route.contains(new Point(x + dx, y + dy))) {
							candidatePoints.add(new Point(x + dx, y + dy));
						}
					}
				}
			}
		}
		return candidatePoints;
	}
}