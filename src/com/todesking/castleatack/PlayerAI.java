package com.todesking.castleatack;

import jp.ac.washi.quinte.api.CursorAction;
import jp.ac.washi.quinte.api.GameInfo;
import jp.ac.washi.quinte.api.SoldierAction;

public class PlayerAI {

	public ActionCommand getNextAction(GameInfo info) {
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

		// 行動の選択
		// 経路が連結されていないなら、カーソルで経路をつなぐ
		// カーソルを使用することでよりよい経路が得られるなら、カーソルを使う
		// 戦士を経路に沿って移動させる

		// 経路を完成させるためのカーソル操作
		final CursorAction ca = getCursorAction(info, targetTilePlacement);

		// 経路に沿って移動するための戦士操作
		final SoldierAction sa = getSoldierAction(info, targetTilePlacement);

		return new ActionCommand(ca, sa);
	}

	private SoldierAction getSoldierAction(GameInfo info,
			int[][] targetTilePlacement) {
		// TODO Auto-generated method stub
		return null;
	}

	private CursorAction getCursorAction(GameInfo info,
			int[][] targetTilePlacement) {

		return null;
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
