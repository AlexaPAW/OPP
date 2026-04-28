package lab3.model.objects;

public enum EnemyKind
{
    SLIME("/images/enemy_slime_sheet.png"),
    SHOOTER("/images/enemy_shooter_sheet.png"),
    TANK("/images/enemy_tank_sheet.png"),
    BOSS("/images/boss_sheet.png");

    private final String spritePath;

    EnemyKind(String spritePath)
    {
        this.spritePath = spritePath;
    }

    public String getSpritePath()
    {
        return spritePath;
    }
}