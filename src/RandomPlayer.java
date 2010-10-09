import jp.ac.washi.quinte.api.PathSearch;
import jp.ac.washi.quinte.api.Point;

public class RandomPlayer extends AbstractSamplePlayer {

	/**
	 * どのゲートを標的にするかを決定します。 このメソッドを書き換えることで、ゲートに関する戦略を設定できます。
	 */
	@Override
	protected void selectTargetGate() {
		// 敵国の門の一覧を作成します
		final Point[] gateList = getGateList(map);

		// ゲートを１つランダムに選んで道を作ります
		final int i = (int) (Math.random() * 9); // 0〜8
		road =
			PathSearch.getReachablePath(map, map
				.getSoldier(myCountry)
				.getLocation(), gateList[i]);
		buildingRoadPoint = map.getSoldier(myCountry).getLocation();
		route.clear();
	}
}