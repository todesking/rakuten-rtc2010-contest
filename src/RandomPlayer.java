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

public class RandomPlayer extends Player {

    // field
    private final ArrayList<Point> candidatePoints = new ArrayList<Point>();
    private final ArrayList<Point> route = new ArrayList<Point>();
    private Point ignorePoint = new Point(0, 0);

    public final int MAX_SEARCH_DEPTH = 20;

    private Direction[] road = null;
    private int iRoad = 0;
    private Point curPoint = new Point(8, 16);
    private Point nextPoint;

    private Direction direction = Direction.UP;

    private MapInfo map;
    private CountryInfo myCountry;
    private CountryInfo leftCountry;
    private CountryInfo oppositeCountry;
    private CountryInfo rightCountry;

    private final Point actionHistory[] = { Point.create(1, 1),
            Point.create(2, 2), Point.create(3, 3), Point.create(4, 4) };
    private int iAction = 0;

    private Direction[] soldierPath = null;
    int iSoldierPath = 0;

    int check[][] = new int[17][17];

    private void checkInfiniteLoopAction() {
        final Point prev1 = actionHistory[(iAction + 3) % 4];
        final Point prev2 = actionHistory[(iAction + 2) % 4];
        final Point prev3 = actionHistory[(iAction + 1) % 4];
        final Point prev4 = actionHistory[iAction % 4];

        if (prev1.x == prev2.x && prev1.y == prev2.y) {
            if (prev1.x == prev3.x && prev1.y == prev3.y) {
                if (prev1.x == prev4.x && prev1.y == prev4.y) {
                    ignorePoint = prev1;
                    setHistory(new Point(0, 0));
                }
            }
        }
        return;
    }

    private void clearCheck() {
        for (int k = 0; k < map.getSize(); k++) {
            for (int j = 0; j < map.getSize(); j++) {
                check[k][j] = 0;
            }
        }
    }

    /**
     * 指定した座標から指定した色について到達可能な地点をチェックします
     * 
     * @param x
     *            起点とするマスのx座標
     * @param y
     *            起点とするマスのy座標
     * @param country
     *            マスの色
     * @return 連結しているマスの数
     */
    private int combineCheck(final int x, final int y, final CountryInfo country) {
        int com = 0;
        if (x < 0 || 16 < x) {
            return com;
        }
        if (y < 0 || 16 < y) {
            return com;
        }

        // 自分の色のマスかゲートであれば移動可能
        if ((map.getTile(x, y).getOwner() == country || map.getTile(x, y)
                .isGate()) && check[y][x] == 0) {
            check[y][x] = 1;
            com++;
            com += combineCheck(x - 1, y, country);
            com += combineCheck(x + 1, y, country);
            com += combineCheck(x, y - 1, country);
            com += combineCheck(x, y + 1, country);
        }
        return com;
    }

    /**
     * 指定した2地点間を通るために必要なマスを方向の配列として返します
     */
    private Direction[] getPath(final int startX, final int startY,
            final int distX, final int distY) {

        final Direction[] _path = PathSearch.getPath(map, new Point(startX,
                startY), new Point(distX, distY), Direction.values());

        nextPoint = new Point(startX, startY);
        return _path;
    }

    private Direction[] getPath(final Point start, final Point dist) {
        return this.getPath(start.x, start.y, dist.x, dist.y);
    }

    /**
     * 指定したマスを起点とした2*2のマスすべてが自分の色のマスであるかどうかを返します
     * 
     * @param x
     *            指定するマスのx座標
     * @param y
     *            指定するマスのy座標
     * @return 指定したマスを起点とした2*2のマスすべてが自分の色のマスである場合はtrue, そうでない場合はfalse
     */
    private boolean isCube(final int x, final int y) {
        if (x < 0 || 15 < x) {
            return true;
        }
        if (y < 0 || 15 < y) {
            return true;
        }

        if (map.getTile(x, y).getOwner() != myCountry
                && map.getTile(x, y).isRotatable()) {
            return false;
        }
        if (map.getTile(x, y + 1).getOwner() != myCountry
                && map.getTile(x, y + 1).isRotatable()) {
            return false;
        }
        if (map.getTile(x + 1, y).getOwner() != myCountry
                && map.getTile(x + 1, y).isRotatable()) {
            return false;
        }
        if (map.getTile(x + 1, y + 1).getOwner() != myCountry
                && map.getTile(x + 1, y + 1).isRotatable()) {
            return false;
        }
        return true;
    }

    /**
     * 指定した2地点間で指定した色について到達可能かどうかを返します
     * 
     * @param startX
     *            開始地点とするマスのx座標
     * @param startY
     *            開始地点とするマスのy座標
     * @param distX
     *            終点地点とするマスのx座標
     * @param distY
     *            終点地点とするマスのy座標
     * @param country
     *            マスの色
     * @return 指定した2地点間で指定した色について到達可能であればtrue, そうでない場合はfalse
     */
    private boolean isReachable(final int startX, final int startY,
            final int distX, final int distY, final CountryInfo country) {
        clearCheck();
        combineCheck(startX, startY, country);
        if (check[distY][distX] == 1) {
            return true;
        }
        return false;
    }

    /**
     * 指定した2地点間で指定した色について到達可能かどうかを返します
     * 
     * @param start
     *            開始地点とするマスを示すポイント
     * @param dist
     *            終点地点とするマスを示すポイント
     * @param country
     *            マスの色
     * @return 指定した2地点間で指定した色について到達可能であればtrue, そうでない場合はfalse
     */
    private boolean isReachable(final Point start, final Point dist,
            final CountryInfo country) {
        return this.isReachable(start.x, start.y, dist.x, dist.y, country);
    }

    /**
     * 指定したマスを下に移動させます
     * 
     * @param x
     *            移動させるマスのx座標
     * @param y
     *            移動させるマスのy座標
     * @param LorR
     *            移動させるマスよりカーソルが右にあるか左にあるかを指定します
     * @return 移動に必要なPlayerActionを返します
     */
    private CursorAction moveToDown(final int x, final int y,
            final Direction LorR) {
        if (LorR == Direction.LEFT) {
            if (isCube(x - 1, y)) {
                return null;
            }
            return new CursorAction(RotateType.CLOCKWISE, new Point(x - 1, y));
        } else if (LorR == Direction.RIGHT) {
            if (isCube(x, y)) {
                return null;
            }
            return new CursorAction(RotateType.ANTICLOCKWISE, new Point(x, y));
        } else {
            System.err.println("Please set WEST or EAST.");
            return null;
        }
    }

    /**
     * 指定したマスを左に移動させます
     * 
     * @param x
     *            移動させるマスのx座標
     * @param y
     *            移動させるマスのy座標
     * @param NorS
     *            移動させるマスよりカーソルが上にあるか下にあるかを指定します
     * @return 移動に必要なPlayerActionを返します
     */
    private CursorAction moveToLeft(final int x, final int y,
            final Direction UorD) {
        if (UorD == Direction.UP) {
            if (isCube(x - 1, y - 1)) {
                return null;
            }
            return new CursorAction(RotateType.CLOCKWISE, new Point(x - 1,
                    y - 1));
        } else if (UorD == Direction.DOWN) {
            if (isCube(x - 1, y)) {
                return null;
            }
            return new CursorAction(RotateType.ANTICLOCKWISE, new Point(x - 1,
                    y));
        } else {
            System.err.println("Please set NORTH or SOUTH.");
            return null;
        }
    }

    /**
     * 指定したマスを右に移動させます
     * 
     * @param x
     *            移動させるマスのx座標
     * @param y
     *            移動させるマスのy座標
     * @param UorD
     *            移動させるマスよりカーソルが上にあるか下にあるかを指定します
     * @return 移動に必要なPlayerActionを返します
     */
    private CursorAction moveToRight(final int x, final int y,
            final Direction UorD) {
        if (UorD == Direction.UP) {
            if (isCube(x, y - 1)) {
                return null;
            }
            return new CursorAction(RotateType.ANTICLOCKWISE, new Point(x,
                    y - 1));
        } else if (UorD == Direction.DOWN) {
            if (isCube(x, y)) {
                return null;
            }
            return new CursorAction(RotateType.CLOCKWISE, new Point(x, y));
        } else {
            System.err.println("Please set NORTH or SOUTH.");
            return null;
        }
    }

    /**
     * 指定したマスを上に移動させます
     * 
     * @param x
     *            移動させるマスのx座標
     * @param y
     *            移動させるマスのy座標
     * @param LorR
     *            移動させるマスよりカーソルが右にあるか左にあるかを指定します
     * @return 移動に必要なPlayerActionを返します
     */
    private CursorAction moveToUp(final int x, final int y, final Direction LorR) {
        if (LorR == Direction.LEFT) {
            if (isCube(x - 1, y - 1)) {
                return null;
            }
            return new CursorAction(RotateType.ANTICLOCKWISE, new Point(x - 1,
                    y - 1));
        } else if (LorR == Direction.RIGHT) {
            if (isCube(x, y - 1)) {
                return null;
            }
            return new CursorAction(RotateType.CLOCKWISE, new Point(x, y - 1));
        } else {
            System.err.println("Please set WEST or EAST.");
            return null;
        }
    }

    @Override
    public CursorAction nextCursorAction(final GameInfo gameInfo) {

        // ゲーム環境に関する情報の更新
        map = gameInfo.getMap();
        myCountry = gameInfo.getMyCountry();
        leftCountry = gameInfo.getLeftCountry();
        oppositeCountry = gameInfo.getOppositeCountry();
        rightCountry = gameInfo.getRightCountry();

        // 移動させるマスの候補のリストをクリア
        candidatePoints.clear();

        // 行動がループしていないかチェック
        checkInfiniteLoopAction();

        // 作る道が決まっている場合
        if (road != null) {
            // 自分の色を運んでくる次のマスを決める(nextPointの決定)
            while (map.getTile(nextPoint.x, nextPoint.y).getOwner() == myCountry
                    || map.getTile(nextPoint.x, nextPoint.y).isGate()) {
                curPoint = nextPoint;
                route.add(curPoint);
                if (iRoad == road.length) {
                    road = null;
                    iRoad = 0;
                    return null; // 目的地に到達したら、そのターンはとりあえず何もしない
                } else {
                    nextPoint = road[iRoad].moveFrom(curPoint);
                    direction = road[iRoad];
                    iRoad++;
                }
            }

            switch (direction) {
            case DOWN:
                searchBlockSouth(nextPoint.x, nextPoint.y);
                break;
            case UP:
                searchBlockNorth(nextPoint.x, nextPoint.y);
                break;
            case LEFT:
                searchBlockWest(nextPoint.x, nextPoint.y);
                break;
            case RIGHT:
                searchBlockEast(nextPoint.x, nextPoint.y);
                break;
            default:
            }

            // 移動させる候補マスがある場合
            if (candidatePoints.size() != 0) {

                Point p = null;
                int iCandidate = 0;
                CursorAction action = null;

                while (iCandidate < candidatePoints.size()) {
                    p = candidatePoints.get(iCandidate++);

                    // 回転できない場合は次の候補へ
                    if (!map.canRotate(p)) {
                        continue;
                    }
                    // 無限ループ回避ポイントだった場合は次の候補へ
                    if (ignorePoint.x == p.x && ignorePoint.y == p.y) {
                        continue;
                    }

                    // 候補マスをnextPointに近づくように移動
                    switch (direction) {
                    case DOWN:
                        if (nextPoint.y - p.y > 0) {
                            action = moveToDown(p.x, p.y, Direction.LEFT);
                        } else if (nextPoint.y - p.y < 0) {
                            action = moveToUp(p.x, p.y, Direction.LEFT);
                        } else if (nextPoint.x - p.x > 0) {
                            action = moveToRight(p.x, p.y, Direction.DOWN);
                        } else {
                            action = moveToLeft(p.x, p.y, Direction.DOWN);
                        }
                        break;

                    case UP:
                        if (nextPoint.y - p.y > 0) {
                            action = moveToDown(p.x, p.y, Direction.RIGHT);
                        } else if (nextPoint.y - p.y < 0) {
                            action = moveToUp(p.x, p.y, Direction.RIGHT);
                        } else if (curPoint.x - p.x > 0) {
                            action = moveToRight(p.x, p.y, Direction.UP);
                        } else {
                            action = moveToLeft(p.x, p.y, Direction.UP);
                        }
                        break;

                    case LEFT:
                        if (nextPoint.x - p.x > 0) {
                            action = moveToRight(p.x, p.y, Direction.UP);
                        } else if (nextPoint.x - p.x < 0) {
                            action = moveToLeft(p.x, p.y, Direction.UP);
                        } else if (curPoint.y - p.y > 0) {
                            action = moveToDown(p.x, p.y, Direction.LEFT);
                        } else {
                            action = moveToUp(p.x, p.y, Direction.LEFT);
                        }
                        break;

                    case RIGHT:
                        if (nextPoint.x - p.x < 0) {
                            action = moveToLeft(p.x, p.y, Direction.DOWN);
                        } else if (nextPoint.x - p.x > 0) {
                            action = moveToRight(p.x, p.y, Direction.DOWN);
                        } else if (curPoint.y - p.y > 0) {
                            action = moveToDown(p.x, p.y, Direction.RIGHT);
                        } else {
                            action = moveToUp(p.x, p.y, Direction.RIGHT);
                        }
                        break;

                    default:
                        action = null;
                    }
                    // 移動させるマスが決定したらwhileループを抜ける
                    if (action != null) {
                        break;
                    }
                }

                if (action != null) {
                    setHistory(p);
                } else {
                    road = null;
                    iRoad = 0;
                    route.clear();
                }
                return action;
            }
        }

        // road == null
        else {

            // 敵国の門の一覧を作成します
            final Point[] gateList = { map.getCenterGateLocation(leftCountry),
                    map.getCenterGateLocation(oppositeCountry),
                    map.getCenterGateLocation(rightCountry),
                    map.getLeftGateLocation(leftCountry),
                    map.getRightGateLocation(leftCountry),
                    map.getLeftGateLocation(oppositeCountry),
                    map.getRightGateLocation(oppositeCountry),
                    map.getLeftGateLocation(rightCountry),
                    map.getRightGateLocation(rightCountry) };

            // ゲートを１つランダムに選んで道を作ります
            final int i = (int) (Math.random() * 9); // 0〜8

            road = this.getPath(map.getSoldier(myCountry).getLocation(),
                    gateList[i]);
            route.clear();

            return null;
        }

        return null;
    }

    @Override
    public SoldierAction nextSoldierAction(final GameInfo gameInfo) {

        // ゲーム環境に関する情報の更新
        map = gameInfo.getMap();
        myCountry = gameInfo.getMyCountry();
        leftCountry = gameInfo.getLeftCountry();
        oppositeCountry = gameInfo.getOppositeCountry();
        rightCountry = gameInfo.getRightCountry();

        // 兵士の行動ルートがある場合
        if (soldierPath != null) {
            if (iSoldierPath < soldierPath.length) {
                return SoldierAction.fromDirection(soldierPath[iSoldierPath++]);
            } else {
                soldierPath = null;
                iSoldierPath = 0;
            }
        }

        // soldierPath == null
        else {

            // 自分の兵士の位置を取得します
            final Point soldierLocation = map.getSoldier(myCountry)
                    .getLocation();
            // 敵国の門の一覧を作成します
            final Point[] gateList = { map.getCenterGateLocation(leftCountry),
                    map.getCenterGateLocation(oppositeCountry),
                    map.getCenterGateLocation(rightCountry),
                    map.getLeftGateLocation(leftCountry),
                    map.getRightGateLocation(leftCountry),
                    map.getLeftGateLocation(oppositeCountry),
                    map.getRightGateLocation(oppositeCountry),
                    map.getLeftGateLocation(rightCountry),
                    map.getRightGateLocation(rightCountry) };

            // 到達可能な門へのパスを取得します
            for (final Point target : gateList) {
                if (this.isReachable(soldierLocation, target, myCountry)) {
                    soldierPath = PathSearch.getPath(map, soldierLocation,
                            target, myCountry);
                    return SoldierAction.NONE;
                }
            }

        }

        // 到達可能な門がなければランダムに行動します
        final int rnd = (int) (Math.random() * 4) + 1; // 1〜4
        switch (rnd) {
        case 1:
            return SoldierAction.UP;
        case 2:
            return SoldierAction.DOWN;
        case 3:
            return SoldierAction.RIGHT;
        case 4:
            return SoldierAction.LEFT;
        }

        return SoldierAction.NONE;

    }

    /**
     * 指定した座標から右方向にある自分の色のマスを取得します
     * 
     * @param x
     *            起点とするマスのx座標
     * @param y
     *            起点とするマスのy座標
     */
    private void searchBlockEast(final int x, final int y) {
        for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
            for (int dx = 0; dx < depth + 1; dx++) {
                for (int dy = -depth + dx; dy < depth - dx + 1; dy++) {
                    if (x + dx < 1 || 15 < x + dx) {
                        continue;
                    }
                    if (y + dy < 1 || 15 < y + dy) {
                        continue;
                    }
                    if (dx + Math.abs(dy) != depth) {
                        continue;
                    }
                    if (map.getTile(x + dx, y + dy).getOwner() == myCountry) {
                        if (!route.contains(new Point(x + dx, y + dy))) {
                            candidatePoints.add(new Point(x + dx, y + dy));
                        }
                    }
                }
            }
        }
        // 右方向に候補が見つからない場合、方向から候補を探します
        if (candidatePoints.size() == 0) {
            searchBlockSouth(x, y);
        }
    }

    /**
     * 指定した座標から上方向にある自分の色のマスを取得します
     * 
     * @param x
     *            起点とするマスのx座標
     * @param y
     *            起点とするマスのy座標
     */
    private void searchBlockNorth(final int x, final int y) {
        for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
            for (int dy = 0; dy < depth + 1; dy++) {
                for (int dx = -depth + dy; dx < depth - dy + 1; dx++) {
                    if (x + dx < 1 || 15 < x + dx) {
                        continue;
                    }
                    if (y - dy < 1 || 15 < y - dy) {
                        continue;
                    }
                    if (dy + Math.abs(dx) != depth) {
                        continue;
                    }
                    if (map.getTile(x + dx, y - dy).getOwner() == myCountry) {
                        if (!route.contains(new Point(x + dx, y - dy))) {
                            candidatePoints.add(new Point(x + dx, y - dy));
                        }
                    }
                }
            }
        }
        // 上方向に候補が見つからない場合、右方向から候補を探します
        if (candidatePoints.size() == 0) {
            searchBlockEast(x, y);
        }
    }

    /**
     * 指定した座標から下方向にある自分の色のマスを取得します
     * 
     * @param x
     *            起点とするマスのx座標
     * @param y
     *            起点とするマスのy座標
     */
    private void searchBlockSouth(final int x, final int y) {
        for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
            for (int dy = 0; dy < depth + 1; dy++) {
                for (int dx = -depth + dy; dx < depth - dy + 1; dx++) {
                    // TODO 将来的にはisAvailableに置き換えられると思う
                    if (x + dx < 1 || 15 < x + dx) {
                        continue;
                    }
                    if (y + dy < 1 || 15 < y + dy) {
                        continue;
                    }
                    if (dy + Math.abs(dx) != depth) {
                        continue;
                    }
                    if (map.getTile(x + dx, y + dy).getOwner() == myCountry) {
                        if (!route.contains(new Point(x + dx, y + dy))) {
                            candidatePoints.add(new Point(x + dx, y + dy));
                        }
                    }
                }
            }
        }
        // 下方向に候補が見つからない場合、左方向から候補を探します
        if (candidatePoints.size() == 0) {
            searchBlockWest(x, y);
        }
    }

    /**
     * 指定した座標から左方向にある自分の色のマスを取得します
     * 
     * @param x
     *            起点とするマスのx座標
     * @param y
     *            起点とするマスのy座標
     */
    private void searchBlockWest(final int x, final int y) {
        for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
            for (int dx = 0; dx < depth + 1; dx++) {
                for (int dy = -depth + dx; dy < depth - dx + 1; dy++) {
                    if (x - dx < 1 || 15 < x - dx) {
                        continue;
                    }
                    if (y + dy < 1 || 15 < y + dy) {
                        continue;
                    }
                    if (dx + Math.abs(dy) != depth) {
                        continue;
                    }
                    if (map.getTile(x - dx, y + dy).getOwner() == myCountry) {
                        if (!route.contains(new Point(x - dx, y + dy))) {
                            candidatePoints.add(new Point(x - dx, y + dy));
                        }
                    }
                }
            }
        }
        // 左方向に候補が見つからない場合、上方向から候補を探します
        if (candidatePoints.size() == 0) {
            searchBlockNorth(x, y);
        }
    }

    private void setHistory(final Point location) {
        actionHistory[iAction % 4] = location;
        iAction++;
    }
}
