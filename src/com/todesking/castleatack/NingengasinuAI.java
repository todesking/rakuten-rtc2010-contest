package com.todesking.castleatack;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.ac.washi.quinte.api.CountryInfo;
import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.MapInfo;
import jp.ac.washi.quinte.api.Point;
import jp.ac.washi.quinte.api.RotateType;
import jp.ac.washi.quinte.api.SoldierAction;
import jp.ac.washi.quinte.api.TileInfo;
import jp.ac.washi.quinte.api.TileType;

import org.apache.commons.lang.NotImplementedException;

import com.google.common.collect.Lists;

public class NingengasinuAI implements PlayerAI {
	int x = 0;

	public ActionCommand getNextAction(final GameInfo info) {
		// 行動方針の決定: 攻撃、防御、工作
		// 攻撃優先
		// やることがなかったら敵から自分への経路を破壊する
		// 安全そうだったらスコアが高い敵を妨害
		// →経路破壊、ボトムの敵からトップの敵への通路開拓

		// とりあえず攻撃すると仮定

		// 攻撃対象の選択
		// 戦士現在位置から各城門までの到達容易性スコアを取得
		// 到達容易性、予測される得点でランキングし攻撃対象を選択

		// とりあえず左の小門を対象とする

		// 経路の決定
		// 戦士から対象までの経路を決定する

		// とりあえず辺に沿って移動する
		int[][] targetTilePlacement = getTilePlacement(info);

		final List<Point> mismatchedPoints =
			getMismatchedPoints(info, targetTilePlacement);

		// 行動の選択 // 経路が連結されていないなら、カーソルで経路をつなぐ
		// カーソルを使用することでよりよい経路が得られるなら、カーソルを使う

		final CursorAction ca;
		if (!mismatchedPoints.isEmpty()) {
			// とりあえず決めうちの経路ができてなかったら作る
			// 自分とこの兵士に近い道から埋めていく
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
			ca =
				getCursorActionForFillRoute(
					info,
					targetTilePlacement,
					fillTargetPoint);
		} else {
			// TODO: 経路ができてた場合、嫌がらせなど行う
			ca = getCursorAction(info, targetTilePlacement);
		}

		// 経路に沿って移動するための戦士操作
		final SoldierAction sa = getSoldierAction(info, targetTilePlacement);

		return new ActionCommand(ca, sa);
	}

	private CursorAction getCursorActionForFillRoute(GameInfo info,
			int[][] targetTilePlacement, Point point) {
		final MapInfo map = info.getMap();
		final Point nearestMyTile =
			getNearestTile(
				info,
				point,
				info.getMyCountry(),
				targetTilePlacement);
		throw new NotImplementedException();
	}

	private Point getNearestTile(GameInfo info, Point point,
			CountryInfo country, int[][] targetTilePlacement) {
		final MapInfo map = info.getMap();
		for (Point p : Util.spiralPoints(map.getSize(), point)) {
			final TileInfo tile = map.getTile(p);
			if (tile != null
				&& tile.getType() == TileType.ROAD
				&& tile.getOwner() == country
				&& targetTilePlacement[p.y][p.x] == T_DONT_CARE)
				return p;
		}
		return null; // wtf
	}

	private List<Point> getMismatchedPoints(GameInfo info,
			int[][] targetTilePlacement) {
		final List<Point> result = Lists.newArrayList();
		for (int y = 0; y < targetTilePlacement.length; y++) {
			for (int x = 0; x < targetTilePlacement[0].length; x++) {
				switch (targetTilePlacement[y][x]) {
				case T_DONT_CARE:
					break; // do nothing
				case T_MY_ROAD:
					result.add(Point.create(x, y));
					break;
				default:
					throw new AssertionError();
				}
			}
		}
		return result;
	}

	private SoldierAction getSoldierAction(GameInfo info,
			int[][] targetTilePlacement) {
		return null;
	}

	private CursorAction getCursorAction(GameInfo info,
			int[][] targetTilePlacement) {
		x = (x + 1) % info.getMap().getSize();
		return new CursorAction(RotateType.CLOCKWISE, x, 10);
	}

	final int T_DONT_CARE = 0;
	final int T_MY_ROAD = 1;

	private int[][] getTilePlacement(GameInfo info) {
		final int[][] tiles = new int[info.getMap().getSize()][];
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = new int[info.getMap().getSize()];

		for (int x = 0; x <= 8; x++)
			tiles[15][x] = 1;
		for (int y = 8; y <= 15; y++)
			tiles[y][15] = 1;
		return tiles;
	}
}
