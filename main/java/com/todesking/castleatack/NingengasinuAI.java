package com.todesking.castleatack;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jp.ac.washi.quinte.api.CountryInfo;
import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.Direction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.MapInfo;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;
import jp.ac.washi.quinte.api.SoldierInfo;
import jp.ac.washi.quinte.api.TileInfo;
import jp.ac.washi.quinte.api.TileType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NingengasinuAI implements PlayerAI {
	int x = 0;

	private Point attackTarget = new Point(16, 12);
	private int[][] targetTilePlacement = null;

	private int prevScore = 0;
	private int deffensiveTimer = 0;

	@Override
	public CursorAction nextCursorAction(final GameInfo info) {
		final PrintStream log = Util.log("ai");
		// 行動方針の決定: 攻撃、防御、工作
		// 攻撃優先
		// やることがなかったら敵から自分への経路を破壊する
		// 安全そうだったらスコアが高い敵を妨害
		// →経路破壊、ボトムの敵からトップの敵への通路開拓

		// とりあえず攻撃すると仮定

		// 攻撃対象の選択
		// 戦士現在位置から各城門までの到達容易性スコアを取得
		// 到達容易性、予測される得点でランキングし攻撃対象を選択

		// とりあえず右の小門を対象とする
		attackTarget = new Point(16, 12);

		// 経路の決定
		// 戦士から対象までの経路を決定する

		// 攻撃検知
		if (info.getMyCountry().getScore() < prevScore)
			deffensiveTimer = 30;
		if (0 < deffensiveTimer) {
			targetTilePlacement = getTilePlacementForDefence(info);
		} else {
			targetTilePlacement = getTilePlacement(info, attackTarget);
		}
		deffensiveTimer--;

		{
			final PrintStream tlog = Util.log("ai.tilePlacement");
			for (int y = 0; y < targetTilePlacement.length; y++) {
				for (int x = 0; x < targetTilePlacement[y].length; x++) {
					tlog.print(targetTilePlacement[y][x] > 0 ? '*' : '.');
				}
				tlog.println();
			}
		}

		final List<Point> mismatchedPoints =
			getMismatchedPoints(info.getMap(), info.getMyCountry());

		// 行動の選択 // 経路が連結されていないなら、カーソルで経路をつなぐ
		// カーソルを使用することでよりよい経路が得られるなら、カーソルを使う

		CursorAction ca = null;
		if (!mismatchedPoints.isEmpty()) {
			// 経路ができてなかったら作る
			Collections.sort(mismatchedPoints, new Comparator<Point>() {
				@Override
				public int compare(Point o1, Point o2) {
					final Point soldierLocation =
						info.getMySoldier().getLocation();
					final int d1 = Util.manhattanDistance(soldierLocation, o1);
					final int d2 = Util.manhattanDistance(soldierLocation, o2);
					return Double.compare(d1, d2);
				}
			});
			final Point fillTargetPoint = mismatchedPoints.get(0);
			log.println("try to make road: " + Util.inspect(fillTargetPoint));
			ca = getCursorActionForFillRoute(info, fillTargetPoint);
			log.println(Util.inspect(ca));
		}
		if (ca == null) {
			// TODO: 経路ができてた場合、嫌がらせなど行う
			ca = getCursorAction(info, targetTilePlacement);
		}
		return ca;
	}

	@Override
	public SoldierAction nextSoldierAction(GameInfo info) {
		return getSoldierAction(info, attackTarget);
	}

	private CursorAction getCursorActionForFillRoute(GameInfo info, Point point) {
		final MapInfo map = info.getMap();
		final CountryInfo country = info.getMyCountry();
		if (map.getTile(point).getOwner() == info.getMyCountry())
			throw new IllegalArgumentException();
		final Set<Point> excludes = Sets.newHashSet();
		CursorAction ca = null;
		do {
			final Point moveTarget =
				getNearestTile(
					info,
					point,
					info.getMyCountry(),
					targetTilePlacement,
					excludes);
			if (moveTarget == null)
				return null;
			excludes.add(moveTarget);
			ca = findMove(info, point, map, country, moveTarget);
		} while (ca == null);
		return ca;
	}

	private CursorAction findMove(GameInfo info, Point point,
			final MapInfo map, final CountryInfo country, Point moveTarget) {
		Util.log("ai").println("nearest tile: " + Util.inspect(moveTarget));
		if (moveTarget == null) // not found??? wtf
			return null;
		if (moveTarget.x == point.x)
			return moveY(map, targetTilePlacement, point, moveTarget, country);
		else if (moveTarget.y == point.y)
			return moveX(map, targetTilePlacement, point, moveTarget, country);
		else {
			if (yBlocked(map, country, moveTarget) || Util.cointoss())
				return moveX(
					map,
					targetTilePlacement,
					point,
					moveTarget,
					country);
			else
				return moveY(
					map,
					targetTilePlacement,
					point,
					moveTarget,
					country);
		}
	}

	private boolean yBlocked(final MapInfo map, final CountryInfo country,
			final Point nearestMyTile) {
		return map.getTile(Util.down(nearestMyTile)).getOwner() == country;
	}

	private CursorAction moveY(MapInfo map, int[][] targetTilePlacement,
			Point point, final Point nearestMyTile, CountryInfo country) {
		Util.log("ai").println("moveY");
		if (nearestMyTile.y > point.y) { // y-
			final Point ccw = Util.upleft(nearestMyTile);
			final Point cw = Util.up(nearestMyTile);
			if (!map.canRotate(cw) && !map.canRotate(ccw))
				return null;
			if (Util.isRoadAllOwnedInCursor(map, cw, country)
				|| !map.canRotate(cw)
				|| Util.cointoss())
				return new CursorAction(RotateType.ANTICLOCKWISE, ccw);
			else
				return new CursorAction(RotateType.CLOCKWISE, cw);
		} else { // y+
			final Point cw = Util.left(nearestMyTile);
			final Point ccw = nearestMyTile;
			if (!map.canRotate(cw) && !map.canRotate(ccw))
				return null;
			if (Util.isRoadAllOwnedInCursor(map, ccw, country)
				|| !map.canRotate(ccw)
				|| Util.cointoss())
				return new CursorAction(RotateType.CLOCKWISE, cw);
			else
				return new CursorAction(RotateType.ANTICLOCKWISE, ccw);
		}
	}

	private CursorAction moveX(MapInfo map, int[][] targetTilePlacement,
			Point point, final Point nearestMyTile, CountryInfo country) {
		Util.log("ai").println("moveX");
		if (nearestMyTile.x > point.x) { // x-
			final Point cw = Util.upleft(nearestMyTile);
			final Point ccw = Util.left(nearestMyTile);
			if (!map.canRotate(cw) && !map.canRotate(ccw))
				return null;
			if (Util.isRoadAllOwnedInCursor(map, cw, country)
				|| map.canRotate(cw)
				|| Util.cointoss()) {
				return new CursorAction(RotateType.ANTICLOCKWISE, ccw);
			} else
				return new CursorAction(RotateType.CLOCKWISE, cw);
		} else { // x+
			final Point ccw = Util.up(nearestMyTile);
			final Point cw = nearestMyTile;
			if (!map.canRotate(cw) && !map.canRotate(ccw))
				return null;
			if (Util.isRoadAllOwnedInCursor(map, ccw, country)
				|| !map.canRotate(ccw)
				|| Util.cointoss())
				return new CursorAction(RotateType.CLOCKWISE, cw);
			else
				return new CursorAction(RotateType.ANTICLOCKWISE, ccw);
		}
	}

	private Point getNearestTile(GameInfo info, Point point,
			CountryInfo country, int[][] targetTilePlacement,
			Set<Point> excludes) {
		final MapInfo map = info.getMap();
		final int size = map.getSize();
		boolean surrounded =
			isSurrounded(map, point, targetTilePlacement, country);
		if (surrounded)
			Util.log("ai").println("its surrounded.");
		loop: for (Point p : Util.nearPoints(size, point)) {
			if (!Util.between(p.x, 0, size - 1)
				|| !Util.between(p.y, 0, size - 1))
				continue;
			if (excludes.contains(p))
				continue;
			final TileInfo tile = map.getTile(p);
			if (tile == null)
				continue;
			if (tile.getType() != TileType.ROAD)
				continue;
			int surroundSoldiers = 0;
			for (SoldierInfo s : Util.getSoldiers(info)) {
				if (s.getLocation().equals(p))
					continue loop;
				for (Point pp : Util.spiralPoints(1, p))
					if (s.getLocation().equals(pp))
						surroundSoldiers++;
			}
			if (surroundSoldiers > 3)
				continue loop;
			if (tile.getOwner() == country
				&& (surrounded || targetTilePlacement[p.y][p.x] == T_DONT_CARE))
				return p;
		}
		return null; // wtf
	}

	private boolean isSurrounded(MapInfo map, Point point,
			int[][] targetTilePlacement, CountryInfo country) {
		int blocked = 0;
		for (Direction d : Direction.values()) {
			final Point p = d.moveFrom(point);
			if ((targetTilePlacement[p.y][p.x] != T_DONT_CARE && map
				.getTile(p)
				.getOwner() == country)
				|| map.getTile(p).getType() != TileType.ROAD) {
				blocked++;
			}
		}
		return blocked == 4;
	}

	private List<Point> getMismatchedPoints(MapInfo map, CountryInfo myCountry) {
		final List<Point> result = Lists.newArrayList();
		for (int y = 0; y < targetTilePlacement.length; y++) {
			for (int x = 0; x < targetTilePlacement[0].length; x++) {
				switch (targetTilePlacement[y][x]) {
				case T_DONT_CARE:
					break; // do nothing
				case T_MY_ROAD:
					if (map.getTile(x, y).getOwner() != myCountry)
						result.add(Point.create(x, y));
					break;
				default:
					throw new AssertionError();
				}
			}
		}
		return result;
	}

	private SoldierAction getSoldierAction(GameInfo info, Point target) {
		final Point currentLocation = info.getMySoldier().getLocation();
		final int currentDist = Util.manhattanDistance(currentLocation, target);
		final CountryInfo country = info.getMyCountry();

		Direction direction = null;
		int dist = currentDist;
		for (Direction dir : Direction.values()) {
			final Point candidatePoint = dir.moveFrom(currentLocation);
			final TileInfo tile = info.getMap().getTile(candidatePoint);
			if (tile == null || (!tile.isGate() && tile.getOwner() != country))
				continue;
			final int d = Util.manhattanDistance(candidatePoint, target);
			if (d < dist) {
				direction = dir;
				dist = d;
			}
		}
		// final Point nextLoction =
		// direction == null ? currentLocation : direction
		// .moveFrom(currentLocation);
		// if (Util.inCursor(ca.getLocation(), nextLoction)) {
		// Util.log("ai").println("my soldier blocks cursor");
		// for (Direction dir : Direction.values()) {
		// final Point point = dir.moveFrom(currentLocation);
		// if (!Util.inCursor(ca.getLocation(), point)
		// && info.getMap().getTile(point).getOwner() == country) {
		// return SoldierAction.fromDirection(dir);
		// }
		// }
		// }
		return SoldierAction.fromDirection(direction);
	}

	private CursorAction getCursorAction(GameInfo info,
			int[][] targetTilePlacement) {
		x = (x + 1) % info.getMap().getSize();
		return new CursorAction(RotateType.CLOCKWISE, x, 10);
	}

	final int T_DONT_CARE = 0;
	final int T_MY_ROAD = 1;

	private int[][] getTilePlacement(GameInfo info, Point attackTarget) {
		final int[][] tiles = new int[info.getMap().getSize()][];
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = new int[info.getMap().getSize()];

		tiles[15][4] = 1;
		tiles[15][7] = 1;
		tiles[15][8] = 1;
		tiles[15][9] = 1;
		tiles[15][12] = 1;

		final Point mySoldier = info.getMySoldier().getLocation();
		final int px = clip(mySoldier.x, 1, 15);
		final int py = clip(mySoldier.y, 1, 15);

		for (int x : Util.range(px, clip(attackTarget.x, 1, 15)))
			tiles[py][x] = 1;
		for (int y : Util.range(py, clip(attackTarget.y, 1, 15)))
			tiles[y][15] = 1;
		return tiles;
	}

	private int[][] getTilePlacementForDefence(GameInfo info) {
		final int[][] tiles = new int[info.getMap().getSize()][];
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = new int[info.getMap().getSize()];

		tiles[15][3] = 1;
		tiles[15][4] = 1;
		tiles[15][5] = 1;
		tiles[15][7] = 1;
		tiles[15][8] = 1;
		tiles[15][9] = 1;
		tiles[15][11] = 1;
		tiles[15][12] = 1;
		tiles[15][13] = 1;

		return tiles;
	}

	private int clip(int value, int min, int max) {
		return Math.min(Math.max(min, value), max);
	}
}
