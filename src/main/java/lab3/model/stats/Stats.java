package lab3.model.stats;

public class Stats
{
    private int maxHp;
    private int hp;
    private int damage;
    private int speed;
    private int fireCooldownTicks;

    public Stats(int maxHp, int damage, int speed, int fireCooldownTicks)
    {
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.damage = Math.max(0, damage);
        this.speed = Math.max(1, speed);
        this.fireCooldownTicks = Math.max(1, fireCooldownTicks);
    }

    public Stats(int maxHp, int hp, int damage, int speed, int fireCooldownTicks)
    {
        this.maxHp = Math.max(1, maxHp);
        this.hp = clamp(hp, 0, this.maxHp);
        this.damage = Math.max(0, damage);
        this.speed = Math.max(1, speed);
        this.fireCooldownTicks = Math.max(1, fireCooldownTicks);
    }

    public int getMaxHp()
    {
        return maxHp;
    }

    public void setMaxHp(int maxHp)
    {
        this.maxHp = Math.max(1, maxHp);
        if (hp > this.maxHp)
        {
            hp = this.maxHp;
        }
    }

    public int getHp()
    {
        return hp;
    }

    public void setHp(int hp)
    {
        this.hp = clamp(hp, 0, maxHp);
    }

    public int getDamage()
    {
        return damage;
    }

    public void setDamage(int damage)
    {
        this.damage = Math.max(0, damage);
    }

    public int getSpeed()
    {
        return speed;
    }

    public void setSpeed(int speed)
    {
        this.speed = Math.max(1, speed);
    }

    public int getFireCooldownTicks()
    {
        return fireCooldownTicks;
    }

    public void setFireCooldownTicks(int fireCooldownTicks)
    {
        this.fireCooldownTicks = Math.max(1, fireCooldownTicks);
    }

    public void heal(int amount)
    {
        if (amount > 0)
        {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    public void damage(int amount)
    {
        if (amount > 0)
        {
            hp = Math.max(0, hp - amount);
        }
    }

    public boolean isAlive() 
    {
        return hp > 0;
    }

    public void restore()
    {
        hp = maxHp;
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString()
    {
        return "Stats{" +
                "maxHp=" + maxHp +
                ", hp=" + hp +
                ", damage=" + damage +
                ", speed=" + speed +
                ", fireCooldownTicks=" + fireCooldownTicks +
                '}';
    }
}