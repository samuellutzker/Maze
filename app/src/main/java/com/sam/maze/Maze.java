package com.sam.maze;
import java.util.Arrays;
import java.util.Random;

public class Maze {
    private final Random random;
    protected final int width, height;
    protected boolean [] horizontal, vertical, visit;

    private static class Pos {
        public int x,y;

        public Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private boolean inField(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private Pos randWalk(Pos p) {
        Pos n = new Pos(p.x, p.y);
        do {
            n.x = p.x; n.y = p.y;
            switch (random.nextInt(4)) {
                case 0 : --n.x; break;
                case 1 : ++n.x; break;
                case 2 : --n.y; break;
                case 3 : ++n.y; break;
            }
        } while (!inField(n.x, n.y));
        return n;
    }

    private void connect(int from, int to) {
        if (Math.abs(to-from) == 1)
            vertical[(from / width)*(width+1) + from % width + (to > from ? 1 : 0)] = false; // x direction
        else
            horizontal[from + (to > from ? width : 0)] = false; // y direction
    }

    private void aldous_broder() {
        int until = width * height;

        Pos p = new Pos(random.nextInt(width), random.nextInt(height));
        int i = p.y * width + p.x;
        visit[i] = true;
        int count = 1;
        while (count < until) {
            p = randWalk(p);
            int j = p.y * width + p.x;
            if (!visit[j]) {
                visit[j] = true;
                ++count;
                connect(i, j);
            }
            i = j;
        }
    }

    boolean dfs(int x, int y) {
        if (x == 0 && y == 0) return true;
        if (visit[y*width+x]) return false;
        visit[y*width+x] = true;
        if (!left(x,y) && dfs(x-1,y)) return true;
        if (!right(x,y) && dfs(x+1,y)) return true;
        if (!top(x,y) && dfs(x,y-1)) return true;
        if (!bottom(x,y) && dfs(x,y+1)) return true;
        visit[y*width+x] = false;
        return false;
    }

    public Maze(int w, int h) {
        width = w;
        height = h;
        random = new Random();
        horizontal = new boolean[w * (h+1)];
        vertical = new boolean[(w+1) * h];
        visit = new boolean[w * h];
        Arrays.fill(horizontal, true);
        Arrays.fill(vertical, true);
        aldous_broder();
        Arrays.fill(visit, false);
        vertical[0] = false;
    }

    public void solve() {
        vertical[0] = true;
        Arrays.fill(visit, false);
        visit[0] = true;
        dfs(width-1, height-1);
        vertical[0] = false;
    }

    public boolean top(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y > height)
            return false;
        return horizontal[y*width + x];
    }
    public boolean left(int x, int y) {
        if (x < 0 || x > width || y < 0 || y >= height)
            return false;
        return vertical[y*(width+1) + x];
    }
    public boolean bottom(int x, int y) {
        return top(x,y+1);
    }
    public boolean right(int x, int y) {
        return left(x+1,y);
    }

    public void print() {
        final char HWALL='-', VWALL='|', CROSS='+', EMPTY=' ', PATH='X';

        boolean hwall = true;

        for (int y=0; y < height || hwall; y += hwall ? 0 : 1, hwall = !hwall) {
            for (int x=0; x <= width; ++x) {
                if (hwall) {
                    boolean h = top(x,y) | top(x-1,y), v = left(x,y) | left(x,y-1);
                    if (h & v) System.out.print( CROSS );
                    else if (h) System.out.print( HWALL );
                    else if (v) System.out.print( VWALL );
                    else System.out.print( EMPTY );
                    if (top(x,y)) System.out.print( HWALL );
                    else System.out.print( EMPTY );
                } else {
                    if (left(x,y)) System.out.print( VWALL );
                    else System.out.print( EMPTY );
                    if (x < width) System.out.print( (visit[y * width + x] ? PATH : EMPTY) );
                }
            }
            System.out.print('\n');
        }
    }
}

