package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;

public class Enemy extends GameObject {

    public enum Direction {
        LEFT, RIGHT
    }

    private final EnemyKind kind;
    private final int roomLevel;

    private int hp;
    private int damage;
    private int moveCooldownTicks;
    private int shotCooldownTicks;
    private int sightRange;

    private long lastMoveTick;
    private long lastShotTick;

    private boolean tankShielded = false;
    private int tankShieldedMoveCooldownTicks;
    private int tankUnshieldedMoveCooldownTicks;

    private long lastContactDamageTick = -1000L;
    private int contactDamageCooldownTicks = 20;

    private static int moveAnimationHoldTicks = 2;
    private int moveAnimationTicksLeft = 0;

    // ДЛЯ БОССА
    private static final int[][] BOSS_BURST_DIRS = {
        { 0, -1 }, // вверх
        { 1,  0 }, // вправо
        { 0,  1 }, // вниз
        { -1, 0 }  // влево
    };

    private static final int BOSS_NORMAL_SHOTS_BEFORE_SPECIAL = 10;
    private static final int BOSS_SPECIAL_DURATION_TICKS = 50;
    private static final int BOSS_BURST_GAP_TICKS = 4;
    private static final int BOSS_SPECIAL_RECOVERY_TICKS = 30;

    private int bossNormalShotsSinceSpecial = 0;
    private int bossBurstIndex = -1;
    private int bossBurstGapTicksLeft = 0;
    private int bossSpecialTicksLeft = 0 * 60;
    private int bossRecoveryTicksLeft = 0 * 60;

    private boolean moving = false;
    private Direction facing = Direction.LEFT;


    public Enemy(int x, int y, EnemyKind kind, int roomLevel) {
        super(
                x, y,
                kind == EnemyKind.BOSS ? GameBalance.BOSS_SIZE : GameBalance.ENEMY_SIZE,
                kind == EnemyKind.BOSS ? GameBalance.BOSS_SIZE : GameBalance.ENEMY_SIZE,
                false
        );

        this.kind = kind;
        this.roomLevel = Math.max(1, roomLevel);
        this.lastMoveTick = -1000L;
        this.lastShotTick = -1000L;

        applyStats(kind, this.roomLevel);
    }

    private void applyStats(EnemyKind kind, int level) {
        switch (kind) {
            case SLIME -> {
                hp = 2 + level;
                damage = 1;
                moveCooldownTicks = 14 - level / 2;
                if (moveCooldownTicks < 8) {
                    moveCooldownTicks = 8;
                }
                moveAnimationHoldTicks = 6;
                shotCooldownTicks = 9999; // не стреляет
                sightRange = 1;
                contactDamageCooldownTicks = 20;
            }
            case SHOOTER -> {
                hp = 3 + level;
                damage = 1 + level / 4;
                moveCooldownTicks = Math.max(3, 7 - level / 3);
                shotCooldownTicks = Math.max(4, 8 - level / 2);
                sightRange = 13 + level;
                contactDamageCooldownTicks = 30;
            }
            case TANK -> {
                hp = 6 + level * 2;
                damage = 2 + level / 3;
                tankShieldedMoveCooldownTicks = Math.max(4, 9 - level / 4);
                tankUnshieldedMoveCooldownTicks = tankShieldedMoveCooldownTicks - 2;

                moveCooldownTicks = tankShieldedMoveCooldownTicks;
                shotCooldownTicks = 9999; // не стреляет
                sightRange = 1;
                contactDamageCooldownTicks = 12;
                tankShielded = true;
            }
            case BOSS -> {
                hp = 20 + level * 5;
                damage = 3 + level;
                moveCooldownTicks = Math.max(2, 5 - level / 4);
                shotCooldownTicks = Math.max(2, 5 - level / 3);
                sightRange = 40;
                contactDamageCooldownTicks = 10;
                bossNormalShotsSinceSpecial = 0;
                bossBurstIndex = -1;
                bossBurstGapTicksLeft = 0;
                bossSpecialTicksLeft = 0;
                bossRecoveryTicksLeft = 0;
            }
        }
    }

    @Override
    public void update(Game game) {
        if (!isAlive()) {
            return;
        }

        if (moveAnimationTicksLeft > 0) {
            moveAnimationTicksLeft--;
        }
        moving = moveAnimationTicksLeft > 0;

        if (hp <= 0) {
            die(game);
            return;
        }

        Player player = game.getPlayer().orElse(null);
        if (player == null || !player.isAlive()) {
            return;
        }

        updateTankMode(player);
        long tick = game.getTickCount();

        if (kind == EnemyKind.BOSS) {
            updateBoss(game, player, tick);
            return;
        }

        tryShoot(game, player, tick);
        tryMove(game, player, tick);
    }

    private void tryMove(Game game, Player player, long tick) {
        if (tick - lastMoveTick < moveCooldownTicks) {
            return;
        }

        int dx = player.getCenterX() - getCenterX();
        int dy = player.getCenterY() - getCenterY();

        int stepX = 0;
        int stepY = 0;

        if (Math.abs(dx) >= Math.abs(dy)) {
            stepX = Integer.signum(dx);
        } else {
            stepY = Integer.signum(dy);
        }

        if (stepX == 0 && stepY == 0) {
            return;
        }

        if (stepX < 0) {
            facing = Direction.LEFT;
        } else if (stepX > 0) {
            facing = Direction.RIGHT;
        }

        int nx = getX() + stepX;
        int ny = getY() + stepY;

        if (player.getX() == nx && player.getY() == ny) {
            setPosition(nx, ny);
            player.takeDamage(game, damage);
            lastMoveTick = tick;
            moveAnimationTicksLeft = moveAnimationHoldTicks;
            moving = true;
            return;
        }

        if (canOccupy(game, nx, ny, getWidth(), getHeight())) {
            setPosition(nx, ny);
            lastMoveTick = tick;
            moveAnimationTicksLeft = moveAnimationHoldTicks;
            moving = true;
        }
    }

    private boolean tryShoot(Game game, Player player, long tick) {
        if (tick - lastShotTick < shotCooldownTicks) {
            return false;
        }

        int dx = player.getCenterX() - getCenterX();
        int dy = player.getCenterY() - getCenterY();

        if (dx != 0 && dy != 0) {
            return false;
        }

        int distance = Math.max(Math.abs(dx), Math.abs(dy));
        if (distance > sightRange) {
            return false;
        }

        int sx = Integer.signum(dx);
        int sy = Integer.signum(dy);

        if (sx == 0 && sy == 0) {
            return false;
        }

        int bx = getProjectileSpawnX(sx);
        int by = getProjectileSpawnY(sy);

        if (!isInsideRoom(game, bx, by, 1, 1)) {
            return false;
        }

        if (!canOccupy(game, bx, by, 1, 1)) {
            return false;
        }

        game.addObject(new Bullet(bx, by, sx, sy, false, damage));
        lastShotTick = tick;
        return true;
    }

    public void tryDealContactDamage(Game game, Player player, long tick) {
        if (!isAlive() || player == null || !player.isAlive()) {
            return;
        }

        if (tick - lastContactDamageTick < contactDamageCooldownTicks) {
            return;
        }

        if (!getBounds().intersects(player.getBounds())) {
            return;
        }

        player.takeDamage(game, damage);
        lastContactDamageTick = tick;
    }

    private void fireNextBossBullet(Game game) {
        if (bossBurstIndex < 0 || bossBurstIndex >= BOSS_BURST_DIRS.length) {
            return;
        }

        int dx = BOSS_BURST_DIRS[bossBurstIndex][0];
        int dy = BOSS_BURST_DIRS[bossBurstIndex][1];

        fireBossDoubleShot(game, dx, dy);

        bossBurstIndex++;
        if (bossBurstIndex >= BOSS_BURST_DIRS.length) {
            bossBurstIndex = 0;
        }
    }

    private void fireBossDoubleShot(Game game, int dx, int dy) {
        if (dx == 0 && dy == -1) {
            fireBossBullet(game, getX() + 2, getY() - 1, dx, dy);
            fireBossBullet(game, getX() + 3, getY() - 1, dx, dy);
        } else if (dx == 1 && dy == 0) {
            fireBossBullet(game, getX() + getWidth(), getY() + 2, dx, dy);
            fireBossBullet(game, getX() + getWidth(), getY() + 3, dx, dy);
        } else if (dx == 0 && dy == 1) {
            fireBossBullet(game, getX() + 2, getY() + getHeight(), dx, dy);
            fireBossBullet(game, getX() + 3, getY() + getHeight(), dx, dy);
        } else if (dx == -1 && dy == 0) {
            fireBossBullet(game, getX() - 1, getY() + 2, dx, dy);
            fireBossBullet(game, getX() - 1, getY() + 3, dx, dy);
        }
    }

    private void fireBossBullet(Game game, int bx, int by, int dx, int dy)
    {
        if (isInsideRoom(game, bx, by, 1, 1))
        {
            game.addObjectLater(new Bullet(bx, by, dx, dy, false, damage), 0);
        }
    }

    private void startBossSpecialAttack(Game game) {
        bossBurstIndex = 0;
        bossBurstGapTicksLeft = BOSS_BURST_GAP_TICKS;
        bossSpecialTicksLeft = BOSS_SPECIAL_DURATION_TICKS;

        moving = false;
        moveAnimationTicksLeft = 0;

        int dx = (facing == Direction.LEFT) ? -1 : 1;
        int dy = 0;
        fireBossDoubleShot(game, dx, dy);
    }

    private void finishBossSpecialAttack(long tick) {
        bossBurstIndex = -1;
        bossBurstGapTicksLeft = 0;
        bossSpecialTicksLeft = 0;
        bossRecoveryTicksLeft = BOSS_SPECIAL_RECOVERY_TICKS;
        bossNormalShotsSinceSpecial = 0;
        moving = false;
        moveAnimationTicksLeft = 0;

        lastShotTick = tick - shotCooldownTicks;
    }

    private void advanceBossSpecialAttack(Game game, long tick) {
        if (bossSpecialTicksLeft > 0) {
            bossSpecialTicksLeft--;
        }

        if (bossBurstGapTicksLeft > 0) {
            bossBurstGapTicksLeft--;
        }

        while (bossSpecialTicksLeft > 0 && bossBurstGapTicksLeft <= 0) {
            if (bossBurstIndex < 0 || bossBurstIndex >= BOSS_BURST_DIRS.length) {
                bossBurstIndex = 0;
            }

            fireNextBossBullet(game);
            bossBurstGapTicksLeft = BOSS_BURST_GAP_TICKS;
        }

        if (bossSpecialTicksLeft <= 0) {
            finishBossSpecialAttack(tick);
        }
    }

    private void updateBoss(Game game, Player player, long tick) {

        // Во время special-атаки босс стоит на месте
        if (bossSpecialTicksLeft > 0) {
            moving = false;
            moveAnimationTicksLeft = 0;
            advanceBossSpecialAttack(game, tick);
            return;
        }

        // После special-атаки босс 0.5 сек не стреляет, но может двигаться
        if (bossRecoveryTicksLeft > 0) {
            bossRecoveryTicksLeft--;
        }
        else {
            if (tryShoot(game, player, tick)) {
                bossNormalShotsSinceSpecial++;

            if (bossNormalShotsSinceSpecial >= BOSS_NORMAL_SHOTS_BEFORE_SPECIAL) {
                startBossSpecialAttack(game);
                return;
            }
        }
        }

        // Обычное движение босса
        if (tick - lastMoveTick < moveCooldownTicks) {
            return;
        }

        int dx = Integer.signum(player.getCenterX() - getCenterX());
        int dy = Integer.signum(player.getCenterY() - getCenterY());

        int stepX = 0;
        int stepY = 0;

        if (Math.abs(player.getCenterX() - getCenterX()) >= Math.abs(player.getCenterY() - getCenterY())) {
            stepX = dx;
        } else {
            stepY = dy;
        }

        if (stepX < 0) {
            facing = Direction.LEFT;
        } else if (stepX > 0) {
            facing = Direction.RIGHT;
        }

        int nx = getX() + stepX;
        int ny = getY() + stepY;

        if (player.getX() == nx && player.getY() == ny) {
            setPosition(nx, ny);
            player.takeDamage(game, damage);
            lastMoveTick = tick;
            moveAnimationTicksLeft = moveAnimationHoldTicks;
            moving = true;
            return;
        }

        if (canOccupy(game, nx, ny, getWidth(), getHeight())) {
            setPosition(nx, ny);
            lastMoveTick = tick;
            moveAnimationTicksLeft = moveAnimationHoldTicks;
            moving = true;
        }
    }

    private static final int TANK_SHIELD_DIST_TILES = 13;
    private static final int TANK_UNSHIELD_DIST_TILES = 12;

    private void updateTankMode(Player player) {
        if (kind != EnemyKind.TANK || player == null) {
            return;
        }

        // Используем именно координаты клетки, а не centerX/centerY.
        int dx = Math.abs(player.getX() - getX());
        int dy = Math.abs(player.getY() - getY());
        int dist = Math.max(dx, dy);

        boolean newShielded;
        if (tankShielded) {

            newShielded = dist > TANK_UNSHIELD_DIST_TILES;
        } else {
            newShielded = dist > TANK_SHIELD_DIST_TILES;
        }

        if (newShielded != tankShielded) {
            tankShielded = newShielded;
            moveAnimationTicksLeft = 0;
            moving = false;
        }

        moveCooldownTicks = tankShielded
                ? tankShieldedMoveCooldownTicks
                : tankUnshieldedMoveCooldownTicks;
    }

    public void takeDamage(Game game, int amount) {
        if (!isAlive()) {
            return;
        }

        hp -= Math.max(0, amount);

        if (hp <= 0) {
            hp = 0;
            die(game);
        }
    }

    private void die(Game game) {
        setAlive(false);

        if (kind == EnemyKind.BOSS) {
            if (game != null) {
                game.win();
            }
            return;
        }

        if (game != null) {
            game.removeObject(this);
        }
    }

    public EnemyKind getKind() {
        return kind;
    }

    public boolean isMoving() {
        return moving;
    }

    public Direction getFacing() {
        return facing;
    }

    public int getHp() {
        return hp;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public char getSymbol() {
        return switch (kind) {
            case SLIME -> '1';
            case SHOOTER -> '2';
            case TANK -> '3';
            case BOSS -> 'B';
        };
    }

    @Override
    public int getRenderLayer() {
        return kind == EnemyKind.BOSS ? 40 : 30;
    }

    public boolean isTankShielded() {
        return kind == EnemyKind.TANK && tankShielded;
    }

    public boolean isBossSpecialAttacking() {
        return kind == EnemyKind.BOSS && bossSpecialTicksLeft > 0;
    }
}
