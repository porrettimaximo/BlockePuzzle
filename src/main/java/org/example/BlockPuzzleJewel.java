import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlockPuzzleJewel {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Block Puzzle Jewel (Java)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class GamePanel extends JPanel implements KeyListener, MouseListener {

        private static final int ROWS = 10;
        private static final int COLS = 10;

        private static final int CELL_SIZE = 32;       // tamaño tablero
        private static final int PIECE_CELL_SIZE = 26; // tamaño bloques de abajo

        // tablero (centrado a ojo)
        private static final int BOARD_X = 50;
        private static final int BOARD_Y = 30;

        // zona de piezas
        private static final int PIECES_Y = 380;

        private final boolean[][] board = new boolean[ROWS][COLS];
        private final Color[][] boardColor = new Color[ROWS][COLS];

        private final List<Piece> pieces = new ArrayList<>();
        private final Random random = new Random();

        private int selectedIndex = 0;
        private boolean selectingPiece = true;

        private int cursorRow = 0;
        private int cursorCol = 0;

        private int score = 0;
        private int highScore = 0;

        private double pulse = 0.0;
        private Timer animationTimer;

        private final List<Integer> rowsToClear = new ArrayList<>();
        private final List<Integer> colsToClear = new ArrayList<>();
        private boolean clearingLines = false;
        private int clearAnimTick = 0;
        private int pendingClears = 0;

        // estados de overlay
        private enum OverlayState { NONE, START, PAUSE, GAME_OVER }
        private OverlayState overlayState = OverlayState.START;

        // botones
        private final Rectangle pauseButtonBounds = new Rectangle();
        private final Rectangle startPlayButtonBounds = new Rectangle();
        private final Rectangle pauseResumeButtonBounds = new Rectangle();
        private final Rectangle pauseRestartButtonBounds = new Rectangle();
        private final Rectangle gameOverRestartButtonBounds = new Rectangle();
        private final Rectangle gameOverCloseButtonBounds = new Rectangle();

        private static final boolean[][][] SHAPES = {
                { { true } },
                { { true, true } },
                { { true }, { true } },
                { { true, true, true } },
                { { true }, { true }, { true } },
                { { true, true }, { true, true } },
                { { true, false }, { true, false }, { true, true } },
                { { false, true }, { false, true }, { true, true } },
                { { true, true, true, true } },
                { { true, true, true }, { false, true, false } }
        };

        private static final Color[] COLORS = {
                new Color(0xFFCC00),
                new Color(0xFF6666),
                new Color(0x66CCFF),
                new Color(0x66FF66),
                new Color(0xCC66FF),
                new Color(0xFF9966)
        };

        public GamePanel() {
            setPreferredSize(new Dimension(420, 540));
            setBackground(new Color(30, 30, 40));

            setFocusable(true);
            addKeyListener(this);
            addMouseListener(this);

            animationTimer = new Timer(30, e -> {
                pulse += 0.12;
                if (clearingLines) {
                    clearAnimTick++;
                    if (clearAnimTick >= 16) {
                        performClearAndScore();
                    }
                }
                repaint();
            });
            animationTimer.start();

            resetBoard();
            generateNewPieces();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        private void resetBoard() {
            for (int r = 0; r < ROWS; r++) {
                Arrays.fill(board[r], false);
                Arrays.fill(boardColor[r], null);
            }
            score = 0;
            selectingPiece = true;
            selectedIndex = 0;
            cursorRow = 0;
            cursorCol = 0;
            pieces.clear();

            rowsToClear.clear();
            colsToClear.clear();
            clearingLines = false;
            clearAnimTick = 0;
            pendingClears = 0;
        }

        private void resetGame() {
            resetBoard();
            generateNewPieces();
            repaint();
        }

        private void generateNewPieces() {
            pieces.clear();
            for (int i = 0; i < 3; i++) {
                pieces.add(randomPiece());
            }
            selectedIndex = Math.min(selectedIndex, pieces.size() - 1);
            if (selectedIndex < 0) selectedIndex = 0;
        }

        private Piece randomPiece() {
            boolean[][] shape = SHAPES[random.nextInt(SHAPES.length)];
            Color color = COLORS[random.nextInt(COLORS.length)];
            return new Piece(shape, color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBoard(g2);
            drawBoardCursorAndGhost(g2);
            drawPiecesPanel(g2);
            drawPieces(g2);
            drawHUD(g2);
            drawOverlay(g2);
        }

        private void drawBoard(Graphics2D g2) {
            g2.setColor(new Color(20, 20, 30));
            g2.fillRoundRect(BOARD_X - 8, BOARD_Y - 8,
                    COLS * CELL_SIZE + 16, ROWS * CELL_SIZE + 16, 18, 18);

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    int x = BOARD_X + c * CELL_SIZE;
                    int y = BOARD_Y + r * CELL_SIZE;

                    boolean isClearing = clearingLines &&
                            (rowsToClear.contains(r) || colsToClear.contains(c));

                    if (board[r][c]) {
                        Color base = boardColor[r][c];
                        if (isClearing) {
                            float f = (float) ((Math.sin(clearAnimTick * 0.5) + 1) / 2.0);
                            int rr = (int) (base.getRed() * (1 - f) + 255 * f);
                            int gg = (int) (base.getGreen() * (1 - f) + 255 * f);
                            int bb = (int) (base.getBlue() * (1 - f) + 255 * f);
                            g2.setColor(new Color(rr, gg, bb));
                        } else {
                            g2.setColor(base);
                        }
                    } else {
                        g2.setColor(new Color(50, 50, 70));
                    }

                    g2.fillRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);

                    g2.setColor(new Color(15, 15, 25));
                    g2.drawRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);
                }
            }
        }

        private void drawBoardCursorAndGhost(Graphics2D g2) {
            if (selectingPiece || overlayState != OverlayState.NONE) return;
            Piece current = getCurrentPiece();
            if (current == null) return;

            int cx = BOARD_X + cursorCol * CELL_SIZE;
            int cy = BOARD_Y + cursorRow * CELL_SIZE;

            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cx + 2, cy + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);

            boolean can = canPlace(current, cursorRow, cursorCol);
            Color base = can ? current.color : Color.RED;
            Color translucent = new Color(base.getRed(), base.getGreen(), base.getBlue(), 120);

            for (int r = 0; r < current.rows; r++) {
                for (int c = 0; c < current.cols; c++) {
                    if (!current.shape[r][c]) continue;

                    int br = cursorRow + r;
                    int bc = cursorCol + c;

                    if (br < 0 || br >= ROWS || bc < 0 || bc >= COLS) continue;

                    int x = BOARD_X + bc * CELL_SIZE;
                    int y = BOARD_Y + br * CELL_SIZE;

                    g2.setColor(translucent);
                    g2.fillRoundRect(x + 4, y + 4, CELL_SIZE - 8, CELL_SIZE - 8, 10, 10);
                }
            }
        }

        private void drawPiecesPanel(Graphics2D g2) {
            int panelX = 16;
            int panelY = PIECES_Y - 24;
            int panelW = getWidth() - 32;
            int panelH = 130;

            g2.setColor(new Color(22, 22, 40));
            g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);

            g2.setColor(new Color(60, 60, 100));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

            g2.setColor(Color.LIGHT_GRAY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("Bloques disponibles", panelX + 12, panelY + 18);
        }

        private void drawPieces(Graphics2D g2) {
            int panelX = 16;
            int baseX = panelX + 40;
            int spacing = 120;

            for (int i = 0; i < 3; i++) {
                if (i >= pieces.size()) continue;

                Piece p = pieces.get(i);
                int px = baseX + i * spacing;

                boolean isSelected = (i == selectedIndex);
                drawSinglePiece(g2, p, px, PIECES_Y, isSelected);

                g2.setColor(Color.LIGHT_GRAY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString("[" + (i + 1) + "]",
                        px + (p.cols * PIECE_CELL_SIZE) / 2 - 4,
                        PIECES_Y + p.rows * PIECE_CELL_SIZE + 16);
            }
        }

        private void drawSinglePiece(Graphics2D g2, Piece p, int baseX, int baseY, boolean selected) {
            double scale = 1.0;
            if (selected) {
                scale = 1.0 + 0.08 * Math.sin(pulse);
            }

            for (int r = 0; r < p.rows; r++) {
                for (int c = 0; c < p.cols; c++) {
                    if (!p.shape[r][c]) continue;

                    int x = baseX + c * PIECE_CELL_SIZE;
                    int y = baseY + r * PIECE_CELL_SIZE;

                    int size = (int) ((PIECE_CELL_SIZE - 4) * scale);
                    int offset = (PIECE_CELL_SIZE - 4 - size) / 2;

                    Color fill = p.color;
                    if (selected) {
                        fill = fill.brighter();
                    }

                    g2.setColor(fill);
                    g2.fillRoundRect(x + 2 + offset, y + 2 + offset, size, size, 12, 12);

                    g2.setColor(new Color(20, 20, 30));
                    g2.drawRoundRect(x + 2 + offset, y + 2 + offset, size, size, 12, 12);
                }
            }

            if (selected && selectingPiece && overlayState == OverlayState.NONE) {
                int width = p.cols * PIECE_CELL_SIZE;
                int height = p.rows * PIECE_CELL_SIZE;
                g2.setColor(new Color(255, 255, 255, 150));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(baseX - 4, baseY - 4, width + 8, height + 8, 16, 16);
            }
        }

        private void drawHUD(Graphics2D g2) {
            // PUNTAJE CENTRADO ARRIBA
            String scoreText = "Score: " + score;
            String highText = "High: " + highScore;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();

            int centerX = getWidth() / 2;
            int gap = 24;

            int scoreWidth = fm.stringWidth(scoreText);
            int highWidth = fm.stringWidth(highText);
            int totalWidth = scoreWidth + gap + highWidth;

            int startX = centerX - totalWidth / 2;
            int baseY = 22;

            // Score
            g2.setColor(Color.WHITE);
            g2.drawString(scoreText, startX, baseY);

            // High
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(highText, startX + scoreWidth + gap, baseY);

            // Botón de pausa
            drawPauseButton(g2);
        }

        private void drawPauseButton(Graphics2D g2) {
            String label = "PAUSE";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();

            int w = fm.stringWidth(label) + 20;
            int h = 24;
            int x = getWidth() - w - 12;
            int y = 8;

            pauseButtonBounds.setBounds(x, y, w, h);

            g2.setColor(new Color(60, 60, 90));
            g2.fillRoundRect(x, y, w, h, 12, 12);

            g2.setColor(new Color(120, 120, 180));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(x, y, w, h, 12, 12);

            int textX = x + (w - fm.stringWidth(label)) / 2;
            int textY = y + (h + fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(Color.WHITE);
            g2.drawString(label, textX, textY);
        }

        private void drawOverlay(Graphics2D g2) {
            if (overlayState == OverlayState.NONE) return;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, getWidth(), getHeight());

            switch (overlayState) {
                case START -> drawStartOverlay(g2);
                case PAUSE -> drawPauseOverlay(g2);
                case GAME_OVER -> drawGameOverOverlay(g2);
                default -> {}
            }
        }

        private void drawStartOverlay(Graphics2D g2) {
            int cardW = 360;
            int cardH = 280;
            int x = (getWidth() - cardW) / 2;
            int y = (getHeight() - cardH) / 2;

            g2.setColor(new Color(25, 25, 45));
            g2.fillRoundRect(x, y, cardW, cardH, 20, 20);

            g2.setColor(new Color(80, 80, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, cardW, cardH, 20, 20);

            // Título
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String title = "BLOCK PUZZLE JEWEL";
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (cardW - fm.stringWidth(title)) / 2;
            int ty = y + 40;
            g2.drawString(title, tx, ty);

            // Texto
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(220, 220, 240));

            int textX = x + 24;
            int lineY = y + 70;
            int lineStep = 16;

            g2.drawString("Objetivo: completar filas o columnas con los bloques.", textX, lineY);
            lineY += lineStep;
            g2.drawString("• MODO PIEZAS: flechas para elegir bloque.", textX, lineY);
            lineY += lineStep;
            g2.drawString("  ENTER / ESPACIO para ir al tablero.", textX, lineY);
            lineY += lineStep;
            g2.drawString("• MODO TABLERO: flechas mueven el cursor.", textX, lineY);
            lineY += lineStep;
            g2.drawString("  ENTER / ESPACIO coloca el bloque, ESC vuelve a las piezas.", textX, lineY);
            lineY += lineStep;
            g2.drawString("• Botón PAUSE arriba a la derecha.", textX, lineY);

            // Botón JUGAR
            String label = "JUGAR";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            fm = g2.getFontMetrics();
            int bw = 120;
            int bh = 32;
            int bx = x + (cardW - bw) / 2;
            int by = y + cardH - 60;

            startPlayButtonBounds.setBounds(bx, by, bw, bh);

            g2.setColor(new Color(90, 170, 90));
            g2.fillRoundRect(bx, by, bw, bh, 16, 16);

            g2.setColor(new Color(150, 220, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, bw, bh, 16, 16);

            int lx = bx + (bw - fm.stringWidth(label)) / 2;
            int ly = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(label, lx, ly);
        }

        private void drawPauseOverlay(Graphics2D g2) {
            int cardW = 300;
            int cardH = 200;
            int x = (getWidth() - cardW) / 2;
            int y = (getHeight() - cardH) / 2;

            g2.setColor(new Color(25, 25, 45));
            g2.fillRoundRect(x, y, cardW, cardH, 20, 20);

            g2.setColor(new Color(80, 80, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, cardW, cardH, 20, 20);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            String title = "Pausa";
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (cardW - fm.stringWidth(title)) / 2;
            int ty = y + 40;
            g2.drawString(title, tx, ty);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(220, 220, 240));

            String s1 = "Score: " + score;
            String s2 = "High Score: " + highScore;
            fm = g2.getFontMetrics();
            int s1x = x + (cardW - fm.stringWidth(s1)) / 2;
            int s2x = x + (cardW - fm.stringWidth(s2)) / 2;
            int sy = y + 70;
            g2.drawString(s1, s1x, sy);
            g2.drawString(s2, s2x, sy + 18);

            // Botones Reanudar / Reiniciar
            String labelResume = "Reanudar";
            String labelRestart = "Reiniciar";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            fm = g2.getFontMetrics();

            int bw = 110;
            int bh = 30;
            int space = 20;
            int totalButtonsWidth = bw * 2 + space;
            int bx = x + (cardW - totalButtonsWidth) / 2;
            int by = y + cardH - 60;

            // Reanudar
            pauseResumeButtonBounds.setBounds(bx, by, bw, bh);
            g2.setColor(new Color(90, 170, 90));
            g2.fillRoundRect(bx, by, bw, bh, 14, 14);
            g2.setColor(new Color(150, 220, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, bw, bh, 14, 14);
            int lx1 = bx + (bw - fm.stringWidth(labelResume)) / 2;
            int ly1 = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(labelResume, lx1, ly1);

            // Reiniciar
            int bx2 = bx + bw + space;
            pauseRestartButtonBounds.setBounds(bx2, by, bw, bh);
            g2.setColor(new Color(180, 110, 90));
            g2.fillRoundRect(bx2, by, bw, bh, 14, 14);
            g2.setColor(new Color(230, 170, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx2, by, bw, bh, 14, 14);
            int lx2 = bx2 + (bw - fm.stringWidth(labelRestart)) / 2;
            int ly2 = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(labelRestart, lx2, ly2);
        }

        private void drawGameOverOverlay(Graphics2D g2) {
            int cardW = 320;
            int cardH = 210;
            int x = (getWidth() - cardW) / 2;
            int y = (getHeight() - cardH) / 2;

            g2.setColor(new Color(25, 25, 45));
            g2.fillRoundRect(x, y, cardW, cardH, 20, 20);

            g2.setColor(new Color(80, 80, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, cardW, cardH, 20, 20);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            String title = "Sin movimientos";
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (cardW - fm.stringWidth(title)) / 2;
            int ty = y + 40;
            g2.drawString(title, tx, ty);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(220, 220, 240));

            String s1 = "Score final: " + score;
            String s2 = "High Score: " + highScore;
            fm = g2.getFontMetrics();
            int s1x = x + (cardW - fm.stringWidth(s1)) / 2;
            int s2x = x + (cardW - fm.stringWidth(s2)) / 2;
            int sy = y + 70;
            g2.drawString(s1, s1x, sy);
            g2.drawString(s2, s2x, sy + 18);

            // Botones Reiniciar / Cerrar
            String labelRestart = "Reiniciar";
            String labelClose = "Cerrar";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            fm = g2.getFontMetrics();

            int bw = 110;
            int bh = 30;
            int space = 20;
            int totalButtonsWidth = bw * 2 + space;
            int bx = x + (cardW - totalButtonsWidth) / 2;
            int by = y + cardH - 60;

            // Reiniciar
            gameOverRestartButtonBounds.setBounds(bx, by, bw, bh);
            g2.setColor(new Color(90, 170, 90));
            g2.fillRoundRect(bx, by, bw, bh, 14, 14);
            g2.setColor(new Color(150, 220, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, bw, bh, 14, 14);
            int lx1 = bx + (bw - fm.stringWidth(labelRestart)) / 2;
            int ly1 = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(labelRestart, lx1, ly1);

            // Cerrar
            int bx2 = bx + bw + space;
            gameOverCloseButtonBounds.setBounds(bx2, by, bw, bh);
            g2.setColor(new Color(120, 120, 140));
            g2.fillRoundRect(bx2, by, bw, bh, 14, 14);
            g2.setColor(new Color(180, 180, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx2, by, bw, bh, 14, 14);
            int lx2 = bx2 + (bw - fm.stringWidth(labelClose)) / 2;
            int ly2 = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(labelClose, lx2, ly2);
        }

        private Piece getCurrentPiece() {
            if (pieces.isEmpty()) return null;
            if (selectedIndex < 0 || selectedIndex >= pieces.size()) return null;
            return pieces.get(selectedIndex);
        }

        private boolean canPlace(Piece p, int row, int col) {
            for (int r = 0; r < p.rows; r++) {
                for (int c = 0; c < p.cols; c++) {
                    if (!p.shape[r][c]) continue;

                    int br = row + r;
                    int bc = col + c;

                    if (br < 0 || br >= ROWS || bc < 0 || bc >= COLS) {
                        return false;
                    }
                    if (board[br][bc]) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void placePiece(Piece p, int row, int col) {
            int placedBlocks = 0;

            for (int r = 0; r < p.rows; r++) {
                for (int c = 0; c < p.cols; c++) {
                    if (!p.shape[r][c]) continue;

                    int br = row + r;
                    int bc = col + c;

                    board[br][bc] = true;
                    boardColor[br][bc] = p.color;
                    placedBlocks++;
                }
            }

            score += placedBlocks;
            if (score > highScore) highScore = score;

            clearCompleteLines();
        }

        private void clearCompleteLines() {
            if (clearingLines) return;

            boolean[] fullRow = new boolean[ROWS];
            boolean[] fullCol = new boolean[COLS];
            Arrays.fill(fullRow, true);
            Arrays.fill(fullCol, true);

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (!board[r][c]) {
                        fullRow[r] = false;
                        fullCol[c] = false;
                    }
                }
            }

            rowsToClear.clear();
            colsToClear.clear();

            for (int r = 0; r < ROWS; r++) {
                if (fullRow[r]) rowsToClear.add(r);
            }
            for (int c = 0; c < COLS; c++) {
                if (fullCol[c]) colsToClear.add(c);
            }

            int cleared = rowsToClear.size() + colsToClear.size();
            if (cleared == 0) return;

            pendingClears = cleared;
            clearingLines = true;
            clearAnimTick = 0;
        }

        private void performClearAndScore() {
            for (int r : rowsToClear) {
                for (int c = 0; c < COLS; c++) {
                    board[r][c] = false;
                    boardColor[r][c] = null;
                }
            }
            for (int c : colsToClear) {
                for (int r = 0; r < ROWS; r++) {
                    board[r][c] = false;
                    boardColor[r][c] = null;
                }
            }

            if (pendingClears > 0) {
                score += pendingClears * 10;
                if (score > highScore) highScore = score;
            }

            rowsToClear.clear();
            colsToClear.clear();
            clearingLines = false;
            clearAnimTick = 0;
            pendingClears = 0;
        }

        private boolean isGameOver() {
            if (pieces.isEmpty()) return false;

            for (Piece p : pieces) {
                for (int r = 0; r < ROWS; r++) {
                    for (int c = 0; c < COLS; c++) {
                        if (canPlace(p, r, c)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        // ==== TECLADO ====
        @Override
        public void keyPressed(KeyEvent e) {
            if (clearingLines) return;

            int key = e.getKeyCode();

            // primero manejar overlays
            if (overlayState == OverlayState.START) {
                handleStartOverlayKey(key);
                repaint();
                return;
            }
            if (overlayState == OverlayState.PAUSE) {
                handlePauseOverlayKey(key);
                repaint();
                return;
            }
            if (overlayState == OverlayState.GAME_OVER) {
                handleGameOverOverlayKey(key);
                repaint();
                return;
            }

            // juego normal
            if (selectingPiece) {
                handlePieceSelectionKey(key);
            } else {
                handleBoardKey(key);
            }

            repaint();
        }

        private void handleStartOverlayKey(int key) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                overlayState = OverlayState.NONE;
            }
        }

        private void handlePauseOverlayKey(int key) {
            switch (key) {
                case KeyEvent.VK_ESCAPE, KeyEvent.VK_P, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                    overlayState = OverlayState.NONE; // reanudar
                }
                case KeyEvent.VK_R -> {
                    resetGame();
                    overlayState = OverlayState.NONE;
                }
            }
        }

        private void handleGameOverOverlayKey(int key) {
            switch (key) {
                case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                    resetGame();
                    overlayState = OverlayState.NONE;
                }
                case KeyEvent.VK_ESCAPE -> {
                    overlayState = OverlayState.NONE; // solo cerrar overlay
                }
            }
        }

        private void handlePieceSelectionKey(int key) {
            if (pieces.isEmpty()) return;

            switch (key) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_UP:
                    selectedIndex--;
                    if (selectedIndex < 0) selectedIndex = pieces.size() - 1;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_DOWN:
                    selectedIndex++;
                    if (selectedIndex >= pieces.size()) selectedIndex = 0;
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    if (getCurrentPiece() != null) {
                        selectingPiece = false;
                        cursorRow = 0;
                        cursorCol = 0;
                    }
                    break;
                default:
                    break;
            }
        }

        private void handleBoardKey(int key) {
            Piece current = getCurrentPiece();
            if (current == null) {
                selectingPiece = true;
                return;
            }

            switch (key) {
                case KeyEvent.VK_LEFT:
                    cursorCol = Math.max(0, cursorCol - 1);
                    break;
                case KeyEvent.VK_RIGHT:
                    cursorCol = Math.min(COLS - 1, cursorCol + 1);
                    break;
                case KeyEvent.VK_UP:
                    cursorRow = Math.max(0, cursorRow - 1);
                    break;
                case KeyEvent.VK_DOWN:
                    cursorRow = Math.min(ROWS - 1, cursorRow + 1);
                    break;
                case KeyEvent.VK_ESCAPE:
                    selectingPiece = true;
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    if (canPlace(current, cursorRow, cursorCol)) {
                        placePiece(current, cursorRow, cursorCol);
                        pieces.remove(selectedIndex);

                        // volver a selección de bloque
                        selectingPiece = true;

                        if (pieces.isEmpty()) {
                            generateNewPieces();
                        } else if (selectedIndex >= pieces.size()) {
                            selectedIndex = pieces.size() - 1;
                        }

                        if (!clearingLines && isGameOver()) {
                            overlayState = OverlayState.GAME_OVER;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}

        // ==== MOUSE ====
        @Override
        public void mousePressed(MouseEvent e) {
            Point p = e.getPoint();

            if (overlayState == OverlayState.NONE) {
                // botón de pausa
                if (pauseButtonBounds.contains(p)) {
                    overlayState = OverlayState.PAUSE;
                    repaint();
                }
                return;
            }

            // overlays
            switch (overlayState) {
                case START -> {
                    if (startPlayButtonBounds.contains(p)) {
                        overlayState = OverlayState.NONE;
                        repaint();
                    }
                }
                case PAUSE -> {
                    if (pauseResumeButtonBounds.contains(p)) {
                        overlayState = OverlayState.NONE; // reanudar
                        repaint();
                    } else if (pauseRestartButtonBounds.contains(p)) {
                        resetGame();
                        overlayState = OverlayState.NONE;
                        repaint();
                    }
                }
                case GAME_OVER -> {
                    if (gameOverRestartButtonBounds.contains(p)) {
                        resetGame();
                        overlayState = OverlayState.NONE;
                        repaint();
                    } else if (gameOverCloseButtonBounds.contains(p)) {
                        overlayState = OverlayState.NONE;
                        repaint();
                    }
                }
                default -> {}
            }

            requestFocusInWindow();
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
    }

    static class Piece {
        boolean[][] shape;
        int rows;
        int cols;
        Color color;

        public Piece(boolean[][] shape, Color color) {
            this.shape = shape;
            this.rows = shape.length;
            this.cols = shape[0].length;
            this.color = color;
        }
    }
}
