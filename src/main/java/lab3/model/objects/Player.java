package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;
import lab3.model.core.GameState;

public class Player extends GameObject {

    private int hp;
    private int maxHp;
    private int damage;
    private int fireCooldownTicks;
    private long lastShotTick;
    private boolean invulnerable = false;
    
    public enum Direction {
        LEFT, RIGHT
    }

    private Direction facing = Direction.RIGHT;
    private boolean moving = false;

    public Player(int x, int y) {
        this(x, y, 10, 1, 10);
    }

    public Player(int x, int y, int maxHp, int damage, int fireCooldownTicks) {
        super(x, y, GameBalance.PLAYER_SIZE, GameBalance.PLAYER_SIZE, true);
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.damage = Math.max(1, damage);
        this.fireCooldownTicks = Math.max(1, fireCooldownTicks);
        this.lastShotTick = -1000L;
    }

    @Override
    public void update(Game game) {
        if (!isAlive()) {
            return;
        }

        if (hp <= 0) {
            setAlive(false);
            game.lose();
        }
    }

    public boolean tryMove(Game game, int dx, int dy) {
        if (!isAlive() || game == null || game.getState() != GameState.RUNNING) {
            return false;
        }

        if (Math.abs(dx) + Math.abs(dy) != 1) {
            return false;
        }

        int nx = getX() + dx;
        int ny = getY() + dy;

        if (!canOccupy(game, nx, ny, getWidth(), getHeight())) {
            return false;
        }

        setPosition(nx, ny);
        return true;
    }

    public boolean shoot(Game game, int dx, int dy) {
        if (!isAlive() || game == null || game.getState() != GameState.RUNNING) {
            return false;
        }

        if (Math.abs(dx) + Math.abs(dy) != 1) {
            return false;
        }

        long tick = game.getTickCount();
        if (tick - lastShotTick < fireCooldownTicks) {
            return false;
        }

        int bx = getProjectileSpawnX(dx);
        int by = getProjectileSpawnY(dy);

        if (!isInsideRoom(game, bx, by, 1, 1)) {
            return false;
        }

        game.addObject(new Bullet(bx, by, dx, dy, true, damage));
        lastShotTick = tick;

        return true;
    }

    public void takeDamage(Game game, int amount) {
        if (invulnerable) {
            return;
        }
        if (!isAlive()) {
            return;
        }

        hp -= Math.max(0, amount);

        if (hp <= 0) {
            hp = 0;
            setAlive(false);

            if (game != null && game.getState() == GameState.RUNNING) {
                game.lose();
            }
        }
    }

    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }

        hp = Math.min(maxHp, hp + amount);
    }

    public void addDamage(int amount) {
        if (amount <= 0) {
            return;
        }

        damage += amount;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getDamage() {
        return damage;
    }

    public int getFireCooldownTicks() {
        return fireCooldownTicks;
    }

    @Override
    public char getSymbol() {
        return 'P';
    }

    @Override
    public int getRenderLayer() { return 50; }

        public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        if (facing != null) {
            this.facing = facing;
        }
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }
}