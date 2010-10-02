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
     * �w�肵�����W����w�肵���F�ɂ��ē��B�\�Ȓn�_���`�F�b�N���܂�
     * 
     * @param x
     *            �N�_�Ƃ���}�X��x���W
     * @param y
     *            �N�_�Ƃ���}�X��y���W
     * @param country
     *            �}�X�̐F
     * @return �A�����Ă���}�X�̐�
     */
    private int combineCheck(final int x, final int y, final CountryInfo country) {
        int com = 0;
        if (x < 0 || 16 < x) {
            return com;
        }
        if (y < 0 || 16 < y) {
            return com;
        }

        // �����̐F�̃}�X���Q�[�g�ł���Έړ��\
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
     * �w�肵��2�n�_�Ԃ�ʂ邽�߂ɕK�v�ȃ}�X������̔z��Ƃ��ĕԂ��܂�
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
     * �w�肵���}�X���N�_�Ƃ���2*2�̃}�X���ׂĂ������̐F�̃}�X�ł��邩�ǂ�����Ԃ��܂�
     * 
     * @param x
     *            �w�肷��}�X��x���W
     * @param y
     *            �w�肷��}�X��y���W
     * @return �w�肵���}�X���N�_�Ƃ���2*2�̃}�X���ׂĂ������̐F�̃}�X�ł���ꍇ��true, �����łȂ��ꍇ��false
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
     * �w�肵��2�n�_�ԂŎw�肵���F�ɂ��ē��B�\���ǂ�����Ԃ��܂�
     * 
     * @param startX
     *            �J�n�n�_�Ƃ���}�X��x���W
     * @param startY
     *            �J�n�n�_�Ƃ���}�X��y���W
     * @param distX
     *            �I�_�n�_�Ƃ���}�X��x���W
     * @param distY
     *            �I�_�n�_�Ƃ���}�X��y���W
     * @param country
     *            �}�X�̐F
     * @return �w�肵��2�n�_�ԂŎw�肵���F�ɂ��ē��B�\�ł����true, �����łȂ��ꍇ��false
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
     * �w�肵��2�n�_�ԂŎw�肵���F�ɂ��ē��B�\���ǂ�����Ԃ��܂�
     * 
     * @param start
     *            �J�n�n�_�Ƃ���}�X�������|�C���g
     * @param dist
     *            �I�_�n�_�Ƃ���}�X�������|�C���g
     * @param country
     *            �}�X�̐F
     * @return �w�肵��2�n�_�ԂŎw�肵���F�ɂ��ē��B�\�ł����true, �����łȂ��ꍇ��false
     */
    private boolean isReachable(final Point start, final Point dist,
            final CountryInfo country) {
        return this.isReachable(start.x, start.y, dist.x, dist.y, country);
    }

    /**
     * �w�肵���}�X�����Ɉړ������܂�
     * 
     * @param x
     *            �ړ�������}�X��x���W
     * @param y
     *            �ړ�������}�X��y���W
     * @param LorR
     *            �ړ�������}�X���J�[�\�����E�ɂ��邩���ɂ��邩���w�肵�܂�
     * @return �ړ��ɕK�v��PlayerAction��Ԃ��܂�
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
     * �w�肵���}�X�����Ɉړ������܂�
     * 
     * @param x
     *            �ړ�������}�X��x���W
     * @param y
     *            �ړ�������}�X��y���W
     * @param NorS
     *            �ړ�������}�X���J�[�\������ɂ��邩���ɂ��邩���w�肵�܂�
     * @return �ړ��ɕK�v��PlayerAction��Ԃ��܂�
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
     * �w�肵���}�X���E�Ɉړ������܂�
     * 
     * @param x
     *            �ړ�������}�X��x���W
     * @param y
     *            �ړ�������}�X��y���W
     * @param UorD
     *            �ړ�������}�X���J�[�\������ɂ��邩���ɂ��邩���w�肵�܂�
     * @return �ړ��ɕK�v��PlayerAction��Ԃ��܂�
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
     * �w�肵���}�X����Ɉړ������܂�
     * 
     * @param x
     *            �ړ�������}�X��x���W
     * @param y
     *            �ړ�������}�X��y���W
     * @param LorR
     *            �ړ�������}�X���J�[�\�����E�ɂ��邩���ɂ��邩���w�肵�܂�
     * @return �ړ��ɕK�v��PlayerAction��Ԃ��܂�
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

        // �Q�[�����Ɋւ�����̍X�V
        map = gameInfo.getMap();
        myCountry = gameInfo.getMyCountry();
        leftCountry = gameInfo.getLeftCountry();
        oppositeCountry = gameInfo.getOppositeCountry();
        rightCountry = gameInfo.getRightCountry();

        // �ړ�������}�X�̌��̃��X�g���N���A
        candidatePoints.clear();

        // �s�������[�v���Ă��Ȃ����`�F�b�N
        checkInfiniteLoopAction();

        // ��铹�����܂��Ă���ꍇ
        if (road != null) {
            // �����̐F���^��ł��鎟�̃}�X�����߂�(nextPoint�̌���)
            while (map.getTile(nextPoint.x, nextPoint.y).getOwner() == myCountry
                    || map.getTile(nextPoint.x, nextPoint.y).isGate()) {
                curPoint = nextPoint;
                route.add(curPoint);
                if (iRoad == road.length) {
                    road = null;
                    iRoad = 0;
                    return null; // �ړI�n�ɓ��B������A���̃^�[���͂Ƃ肠�����������Ȃ�
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

            // �ړ���������}�X������ꍇ
            if (candidatePoints.size() != 0) {

                Point p = null;
                int iCandidate = 0;
                CursorAction action = null;

                while (iCandidate < candidatePoints.size()) {
                    p = candidatePoints.get(iCandidate++);

                    // ��]�ł��Ȃ��ꍇ�͎��̌���
                    if (!map.canRotate(p)) {
                        continue;
                    }
                    // �������[�v����|�C���g�������ꍇ�͎��̌���
                    if (ignorePoint.x == p.x && ignorePoint.y == p.y) {
                        continue;
                    }

                    // ���}�X��nextPoint�ɋ߂Â��悤�Ɉړ�
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
                    // �ړ�������}�X�����肵����while���[�v�𔲂���
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

            // �G���̖�̈ꗗ���쐬���܂�
            final Point[] gateList = { map.getCenterGateLocation(leftCountry),
                    map.getCenterGateLocation(oppositeCountry),
                    map.getCenterGateLocation(rightCountry),
                    map.getLeftGateLocation(leftCountry),
                    map.getRightGateLocation(leftCountry),
                    map.getLeftGateLocation(oppositeCountry),
                    map.getRightGateLocation(oppositeCountry),
                    map.getLeftGateLocation(rightCountry),
                    map.getRightGateLocation(rightCountry) };

            // �Q�[�g���P�����_���ɑI��œ������܂�
            final int i = (int) (Math.random() * 9); // 0�`8

            road = this.getPath(map.getSoldier(myCountry).getLocation(),
                    gateList[i]);
            route.clear();

            return null;
        }

        return null;
    }

    @Override
    public SoldierAction nextSoldierAction(final GameInfo gameInfo) {

        // �Q�[�����Ɋւ�����̍X�V
        map = gameInfo.getMap();
        myCountry = gameInfo.getMyCountry();
        leftCountry = gameInfo.getLeftCountry();
        oppositeCountry = gameInfo.getOppositeCountry();
        rightCountry = gameInfo.getRightCountry();

        // ���m�̍s�����[�g������ꍇ
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

            // �����̕��m�̈ʒu���擾���܂�
            final Point soldierLocation = map.getSoldier(myCountry)
                    .getLocation();
            // �G���̖�̈ꗗ���쐬���܂�
            final Point[] gateList = { map.getCenterGateLocation(leftCountry),
                    map.getCenterGateLocation(oppositeCountry),
                    map.getCenterGateLocation(rightCountry),
                    map.getLeftGateLocation(leftCountry),
                    map.getRightGateLocation(leftCountry),
                    map.getLeftGateLocation(oppositeCountry),
                    map.getRightGateLocation(oppositeCountry),
                    map.getLeftGateLocation(rightCountry),
                    map.getRightGateLocation(rightCountry) };

            // ���B�\�Ȗ�ւ̃p�X���擾���܂�
            for (final Point target : gateList) {
                if (this.isReachable(soldierLocation, target, myCountry)) {
                    soldierPath = PathSearch.getPath(map, soldierLocation,
                            target, myCountry);
                    return SoldierAction.NONE;
                }
            }

        }

        // ���B�\�Ȗ傪�Ȃ���΃����_���ɍs�����܂�
        final int rnd = (int) (Math.random() * 4) + 1; // 1�`4
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
     * �w�肵�����W����E�����ɂ��鎩���̐F�̃}�X���擾���܂�
     * 
     * @param x
     *            �N�_�Ƃ���}�X��x���W
     * @param y
     *            �N�_�Ƃ���}�X��y���W
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
        // �E�����Ɍ�₪������Ȃ��ꍇ�A�����������T���܂�
        if (candidatePoints.size() == 0) {
            searchBlockSouth(x, y);
        }
    }

    /**
     * �w�肵�����W���������ɂ��鎩���̐F�̃}�X���擾���܂�
     * 
     * @param x
     *            �N�_�Ƃ���}�X��x���W
     * @param y
     *            �N�_�Ƃ���}�X��y���W
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
        // ������Ɍ�₪������Ȃ��ꍇ�A�E�����������T���܂�
        if (candidatePoints.size() == 0) {
            searchBlockEast(x, y);
        }
    }

    /**
     * �w�肵�����W���牺�����ɂ��鎩���̐F�̃}�X���擾���܂�
     * 
     * @param x
     *            �N�_�Ƃ���}�X��x���W
     * @param y
     *            �N�_�Ƃ���}�X��y���W
     */
    private void searchBlockSouth(final int x, final int y) {
        for (int depth = 1; depth < MAX_SEARCH_DEPTH; depth++) {
            for (int dy = 0; dy < depth + 1; dy++) {
                for (int dx = -depth + dy; dx < depth - dy + 1; dx++) {
                    // TODO �����I�ɂ�isAvailable�ɒu����������Ǝv��
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
        // �������Ɍ�₪������Ȃ��ꍇ�A�������������T���܂�
        if (candidatePoints.size() == 0) {
            searchBlockWest(x, y);
        }
    }

    /**
     * �w�肵�����W���獶�����ɂ��鎩���̐F�̃}�X���擾���܂�
     * 
     * @param x
     *            �N�_�Ƃ���}�X��x���W
     * @param y
     *            �N�_�Ƃ���}�X��y���W
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
        // �������Ɍ�₪������Ȃ��ꍇ�A������������T���܂�
        if (candidatePoints.size() == 0) {
            searchBlockNorth(x, y);
        }
    }

    private void setHistory(final Point location) {
        actionHistory[iAction % 4] = location;
        iAction++;
    }
}
