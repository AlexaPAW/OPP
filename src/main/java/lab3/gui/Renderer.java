    package lab3.gui;

    import java.io.InputStream;
    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.IdentityHashMap;
    import java.util.Map;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
    import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
    import javafx.scene.text.Font;
    import lab3.model.core.Game;
    import lab3.model.objects.Bullet;
    import lab3.model.objects.Chest;
    import lab3.model.objects.Enemy;
    import lab3.model.objects.ExitPortal;
    import lab3.model.objects.GameObject;
    import lab3.model.objects.Player;
    import lab3.model.objects.Wall;
    import lab3.model.world.Room;

    /**
     * GUI-рендерер.
     *
     * Важное:
     * - render(..., alpha) использует интерполяцию между previousX/previousY и x/y;
     * - render(...) без alpha оставлен для совместимости и рисует без интерполяции;
     * - текстовый режим этим классом не пользуется.
     */
    public final class Renderer {
        private static final double MIN_ALPHA = 0.0;
        private static final double MAX_ALPHA = 1.0;

        private final int tileSize;
        private static final int PLAYER_FRAME_W = 30;
        private static final int PLAYER_FRAME_H = 30;
        private static final long PLAYER_FRAME_NS = 140_000_000L; // 0.14 сек

        private static final int PORTAL_FRAME_W = 95;
        private static final int PORTAL_FRAME_H = 95;
        private static final int PORTAL_FRAME_COUNT = 4;
        private static final long PORTAL_FRAME_NS = 120_000_000L;

        private final Image playerSheet;
        private final Image slimeSheet;
        private final Image shooterSheet;
        private final Image tankSheet;
        private final Image tankShieldedSheet;
        private final Image bossSheet;
        private final Image bossAttackSheet;
        private final Image portalSheet;
        private final Image bulletImage;
        private final Image chestImage;
        private final Image wallImage;

        private final Map<GameObject, AnimState> bossAttackAnimStates = new IdentityHashMap<>();
        private final Map<GameObject, AnimState> animStates = new IdentityHashMap<>();
        private final Map<GameObject, AnimState> portalAnimStates = new IdentityHashMap<>();

        private final ArrayList<GameObject> renderBuffer = new ArrayList<>(256);
        private Room cachedBackgroundRoom;
        private WritableImage cachedBackground;

        private static final Color BG_COLOR = Color.web("#101418");
        private static final Color GRID_COLOR = Color.web("#1f2630");
        private static final Color FRAME_COLOR = Color.web("#4a5568");
        private static final Font FPS_FONT = Font.font("Consolas", 16);

        private static final Color FALLBACK_PLAYER = Color.web("#66ff88");
        private static final Color FALLBACK_ENEMY = Color.web("#ff6666");
        private static final Color FALLBACK_PORTAL = Color.web("#b28dff");
        private static final Color FALLBACK_BULLET = Color.web("#ffd34d");
        private static final Color FALLBACK_CHEST = Color.web("#d9b44a");
        private static final Color FALLBACK_WALL = Color.web("#8a8f98");
        private static final Color FALLBACK_GENERIC = Color.web("#8899aa");
        private static final Color FALLBACK_TEXT = Color.web("#111111");

        private static final long FPS_SAMPLE_NS = 1_000_000_000L;
        private long fpsSampleStartNs = System.nanoTime();
        private int fpsFramesInSample = 0;
        private int fps = 0;

        public Renderer(int tileSize) {
            this.tileSize = Math.max(1, tileSize);

            this.playerSheet = loadImage("/images/player_sheet.png");
            this.slimeSheet = loadImage("/images/enemy_slime_sheet.png");
            this.shooterSheet = loadImage("/images/enemy_shooter_sheet.png");
            this.tankSheet = loadImage("/images/enemy_tank_sheet.png");
            this.tankShieldedSheet = loadImage("/images/enemy_tankshielded_sheet.png");
            this.bossSheet = loadImage("/images/boss_sheet.png");
            this.bossAttackSheet = loadImage("/images/boss_attack_sheet.png");
            this.bulletImage = loadImage("/images/bullet.png");
            this.chestImage = loadImage("/images/chest.png");
            this.wallImage = loadImage("/images/wall.png");
            this.portalSheet = loadImage("/images/portal_sheet.png");
        }

        private void ensureBackground(Room room)
        {
            if (room == null) {
                return;
            }

            if (cachedBackgroundRoom == room && cachedBackground != null) {
                return;
            }

            int roomPixelWidth = room.getWidth() * tileSize;
            int roomPixelHeight = room.getHeight() * tileSize;

            Canvas bgCanvas = new Canvas(roomPixelWidth, roomPixelHeight);
            GraphicsContext bgGc = bgCanvas.getGraphicsContext2D();

            bgGc.setFill(BG_COLOR);
            bgGc.fillRect(0, 0, roomPixelWidth, roomPixelHeight);
            bgGc.setImageSmoothing(false);

            drawGrid(bgGc, room, 0, 0);
            drawFrame(bgGc, 0, 0, roomPixelWidth, roomPixelHeight);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(BG_COLOR);
            cachedBackground = bgCanvas.snapshot(params, null);
            cachedBackgroundRoom = room;
        }

        public void render(GraphicsContext gc, Game game, double canvasWidth, double canvasHeight)
        {
            animStates.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
            portalAnimStates.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
            bossAttackAnimStates.entrySet().removeIf(e -> e.getKey() == null || !e.getKey().isAlive());
            render(gc, game, canvasWidth, canvasHeight, 1.0);
        }

        public void render(GraphicsContext gc, Game game, double canvasWidth, double canvasHeight, double alpha) {
            if (gc == null || game == null) {
                return;
            }

            Room room = game.getCurrentRoom();
            if (room == null) {
                return;
            }

            renderBuffer.clear();
            renderBuffer.addAll(game.getObjectsSnapshot());
            renderBuffer.sort(Comparator.comparingInt(GameObject::getRenderLayer));

            int roomPixelWidth = room.getWidth() * tileSize;
            int roomPixelHeight = room.getHeight() * tileSize;

            double offsetX = Math.max(0.0, (canvasWidth - roomPixelWidth) / 2.0);
            double offsetY = Math.max(0.0, (canvasHeight - roomPixelHeight) / 2.0);

            gc.setImageSmoothing(false);

            ensureBackground(room);
            if (cachedBackground != null) {
                gc.drawImage(cachedBackground, offsetX, offsetY);
            }

            double clampedAlpha = clamp(alpha);
            long now = System.nanoTime();

            for (GameObject object : renderBuffer) {
                if (object == null || !object.isAlive()) {
                    continue;
                }

                double x = offsetX + object.getRenderX(clampedAlpha) * tileSize;
                double y = offsetY + object.getRenderY(clampedAlpha) * tileSize;

                drawObject(gc, object, x, y, now);
            }

            updateFps(now);
            drawFps(gc, offsetX, offsetY);
        }

        private static final class AnimState {
            int frame;
            long accumulator;
            long lastNanos;
        }

        private void drawGrid(GraphicsContext gc, Room room, double offsetX, double offsetY) {
            gc.setStroke(Color.web("#1f2630"));
            gc.setLineWidth(1.0);

            for (int x = 0; x <= room.getWidth(); x++) {
                double px = offsetX + x * tileSize + 0.5;
                gc.strokeLine(px, offsetY, px, offsetY + room.getHeight() * tileSize);
            }

            for (int y = 0; y <= room.getHeight(); y++) {
                double py = offsetY + y * tileSize + 0.5;
                gc.strokeLine(offsetX, py, offsetX + room.getWidth() * tileSize, py);
            }
        }

        private void drawFrame(GraphicsContext gc, double offsetX, double offsetY, int roomPixelWidth, int roomPixelHeight) {
            gc.setStroke(Color.web("#4a5568"));
            gc.setLineWidth(2.0);
            gc.strokeRect(offsetX, offsetY, roomPixelWidth, roomPixelHeight);
        }

        private long playerAnimLastNs = 0L;
        private int playerRunFrame = 0;

        private void drawPlayer(GraphicsContext gc, Player player, double x, double y, double w, double h, long now) {
            if (playerSheet == null) {
                drawGenericFallback(gc, x, y, w, h, player.getSymbol(), FALLBACK_PLAYER, true);
                return;
            }

            int frameIndex;
            //long now = System.nanoTime();

            if (player.isMoving()) {
                if (playerAnimLastNs == 0L) {
                    playerAnimLastNs = now;
                }

                long elapsed = now - playerAnimLastNs;

                if (elapsed >= PLAYER_FRAME_NS) {
                    long steps = elapsed / PLAYER_FRAME_NS;
                    playerRunFrame = (playerRunFrame + (int) steps) % 2;
                    playerAnimLastNs += steps * PLAYER_FRAME_NS;
                }

                frameIndex = 1 + playerRunFrame;
            } else {
                playerAnimLastNs = 0L;
                playerRunFrame = 0;
                frameIndex = 0;
            }

            int sx = frameIndex * PLAYER_FRAME_W;
            int sy = 0;

            gc.save();

            if (player.getFacing() == Player.Direction.LEFT) {
                gc.translate(x + w, y);
                gc.scale(-1, 1);
                gc.drawImage(playerSheet,
                        sx, sy, PLAYER_FRAME_W, PLAYER_FRAME_H,
                        0, 0, w, h);
            } else {
                gc.drawImage(playerSheet,
                        sx, sy, PLAYER_FRAME_W, PLAYER_FRAME_H,
                        x, y, w, h);
            }

            gc.restore();
        }

        private static final int FRAME_W = 30;
        private static final int FRAME_H = 30;
        private static final long FRAME_NS_DEFAULT = 140_000_000L; //0.14 sec
        private static final long FRAME_NS_SLIME = 190_000_000L;
        private static final long FRAME_NS_BOSS = 170_000_000L;

        private long getEnemyFrameNs(Enemy enemy) {
            return switch (enemy.getKind()) {
                case SLIME -> FRAME_NS_SLIME;
                case BOSS -> FRAME_NS_BOSS;
                case SHOOTER, TANK -> FRAME_NS_DEFAULT;
            };
        }

        private void drawEnemy(GraphicsContext gc, Enemy enemy, double x, double y, double w, double h, long now) {
            switch (enemy.getKind()) {
                case SLIME -> {
                    if (slimeSheet == null) {
                        drawGenericFallback(gc, x, y, w, h, enemy.getSymbol(), FALLBACK_ENEMY, true);
                        return;
                    }
                    drawAnimatedSprite(gc, enemy, slimeSheet, enemy.isMoving(), enemy.getFacing() == Enemy.Direction.LEFT,
                            x, y, w, h, getEnemyFrameNs(enemy), 3, FRAME_W, FRAME_H, now);
                }
                case SHOOTER -> {
                    if (shooterSheet == null) {
                        drawGenericFallback(gc, x, y, w, h, enemy.getSymbol(), FALLBACK_ENEMY, true);
                        return;
                    }
                    drawAnimatedSprite(gc, enemy, shooterSheet, enemy.isMoving(), enemy.getFacing() == Enemy.Direction.LEFT,
                            x, y, w, h, getEnemyFrameNs(enemy), 3, FRAME_W, FRAME_H, now);
                }
                case TANK -> {
                    Image tankSheetToDraw = enemy.isTankShielded() ? tankShieldedSheet : tankSheet;
                    if (tankSheetToDraw == null) {
                        drawGenericFallback(gc, x, y, w, h, enemy.getSymbol(), FALLBACK_ENEMY, true);
                        return;
                    }
                    drawAnimatedSprite(gc, enemy, tankSheetToDraw, enemy.isMoving(), enemy.getFacing() == Enemy.Direction.LEFT,
                            x, y, w, h, getEnemyFrameNs(enemy), 4, FRAME_W, FRAME_H, now);
                }
                case BOSS -> {
                    if (bossAttackSheet == null && bossSheet == null) {
                        drawGenericFallback(gc, x, y, w, h, enemy.getSymbol(), FALLBACK_ENEMY, true);
                        return;
                    }
                    if (enemy.isBossSpecialAttacking())
                    {
                        drawAnimatedSprite(gc, enemy, bossAttackSheet, true, enemy.getFacing() == Enemy.Direction.LEFT,
                                x, y, w, h, getEnemyFrameNs(enemy), 7, FRAME_W+5, FRAME_H+5, now);
                    }
                    else
                        drawAnimatedSprite(gc, enemy, bossSheet, enemy.isMoving(), enemy.getFacing() == Enemy.Direction.LEFT,
                                x, y, w, h, getEnemyFrameNs(enemy), 4, FRAME_W+5, FRAME_H+5, now);

                }
            }
        }

        private void drawExitPortal(GraphicsContext gc, ExitPortal portal, double x, double y, double w, double h, long now) {
            if (portalSheet == null) {
                drawGenericFallback(gc, x, y, w, h, portal.getSymbol(), FALLBACK_PORTAL, true);
                return;
            }

            AnimState state = portalAnimStates.computeIfAbsent(portal, p -> new AnimState());

            //long now = System.nanoTime();
            if (state.lastNanos == 0L) {
                state.lastNanos = now;
            }

            long elapsed = now - state.lastNanos;
            state.lastNanos = now;
            state.accumulator += elapsed;

            long steps = state.accumulator / PORTAL_FRAME_NS;
            if (steps > 0) {
                state.accumulator %= PORTAL_FRAME_NS;
                state.frame = (int) ((state.frame + steps) % PORTAL_FRAME_COUNT);
            }

            int frameIndex = state.frame;
            int sx = frameIndex * PORTAL_FRAME_W;
            int sy = 0;

            gc.drawImage(
                    portalSheet,
                    sx, sy, PORTAL_FRAME_W, PORTAL_FRAME_H,
                    x, y, w, h
            );
        }

        private void drawAnimatedSprite(GraphicsContext gc,
                                    GameObject object,
                                    Image sheet,
                                    boolean moving,
                                    boolean faceLeft,
                                    double x, double y, double w, double h,
                                    long frameNs,
                                    int frameCount,
                                    int frameW,
                                    int frameH, long now) {
            AnimState state = animStates.computeIfAbsent(object, o -> new AnimState());

            //long now = System.nanoTime();
            if (state.lastNanos == 0L) {
                state.lastNanos = now;
            }

            int frameIndex;

            if (moving) {
                long elapsed = now - state.lastNanos;
                state.lastNanos = now;

                state.accumulator += elapsed;

                long steps = state.accumulator / frameNs;
                if (steps > 0) {
                    state.accumulator %= frameNs;
                    state.frame = (int) ((state.frame + steps) % frameCount);
                }

                frameIndex = state.frame;
            } else {
                state.frame = 0;
                state.accumulator = 0L;
                state.lastNanos = now;
                frameIndex = 0;
            }

            int sx = frameIndex * frameW;

            gc.save();
            if (faceLeft) {
                gc.translate(x + w, y);
                gc.scale(-1, 1);
                gc.drawImage(sheet, sx, 0, frameW, frameH, 0, 0, w, h);
            } else {
                gc.drawImage(sheet, sx, 0, frameW, frameH, x, y, w, h);
            }
            gc.restore();
        }

        private void updateFps(long now) {
            fpsFramesInSample++;
            //long now = System.nanoTime();

            if (now - fpsSampleStartNs >= FPS_SAMPLE_NS) {
                fps = fpsFramesInSample;
                fpsFramesInSample = 0;
                fpsSampleStartNs = now;
            }
        }

        private void drawFps(GraphicsContext gc, double offsetX, double offsetY) {
            gc.setFill(Color.WHITE);
            gc.setFont(FPS_FONT);
            gc.fillText("FPS: " + fps, offsetX + 10, offsetY + 20);
        }

        private void drawObject(GraphicsContext gc, GameObject object, double x, double y, long now) {
            double w = object.getWidth() * tileSize;
            double h = object.getHeight() * tileSize;

            if (object instanceof Player player) {
                drawPlayer(gc, player, x, y, w, h, now);
                return;
            }

            if (object instanceof Enemy enemy) {
                drawEnemy(gc, enemy, x, y, w, h, now);
                return;
            }

            if (object instanceof Bullet) {
                drawSpriteOrFallback(gc, bulletImage, x, y, w, h, FALLBACK_BULLET, object.getSymbol(), false);
                return;
            }

            if (object instanceof Chest) {
                drawSpriteOrFallback(gc, chestImage, x, y, w, h, FALLBACK_CHEST, object.getSymbol(), true);
                return;
            }

            if (object instanceof Wall) {
                drawSpriteOrFallback(gc, wallImage, x, y, w, h, FALLBACK_WALL, object.getSymbol(), true);
                return;
            }

            if (object instanceof ExitPortal portal) {
                drawExitPortal(gc, portal, x, y, w, h, now);
                return;
            }

            drawGenericFallback(gc, x, y, w, h, object.getSymbol(), object.isSolid() ? FALLBACK_GENERIC : FALLBACK_TEXT);
        }

        private void drawSpriteOrFallback(GraphicsContext gc,
                                        Image image,
                                        double x,
                                        double y,
                                        double w,
                                        double h,
                                        Color fallbackColor,
                                        char symbol,
                                        boolean withSymbol) {
            if (image != null) {
                gc.drawImage(image, x, y, w, h);
                return;
            }

            drawGenericFallback(gc, x, y, w, h, symbol, fallbackColor, withSymbol);
        }

        private void drawGenericFallback(GraphicsContext gc,
                                        double x,
                                        double y,
                                        double w,
                                        double h,
                                        char symbol,
                                        Color fillColor) {
            drawGenericFallback(gc, x, y, w, h, symbol, fillColor, true);
        }

        private void drawGenericFallback(GraphicsContext gc,
                                        double x,
                                        double y,
                                        double w,
                                        double h,
                                        char symbol,
                                        Color fillColor,
                                        boolean withSymbol) {
            gc.setFill(fillColor);
            gc.fillRoundRect(x + 2, y + 2, Math.max(1, w - 4), Math.max(1, h - 4), 8, 8);

            if (withSymbol) {
                gc.setFill(FALLBACK_TEXT);
                gc.setFont(Font.font("Consolas", Math.max(10, Math.min(w, h) * 0.35)));
                gc.fillText(String.valueOf(symbol), x + w * 0.30, y + h * 0.65);
            }
        }

        private Image loadImage(String path) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                if (stream == null) {
                    return null;
                }
                return new Image(stream);
            } catch (Exception e) {
                return null;
            }
        }

        private static double clamp(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return 1.0;
            }
            if (value < MIN_ALPHA) {
                return MIN_ALPHA;
            }
            if (value > MAX_ALPHA) {
                return MAX_ALPHA;
            }
            return value;
        }
    }
