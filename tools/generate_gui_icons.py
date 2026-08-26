"""Generate MotionUI GUI sprites from the supplied SVG silhouettes."""
from pathlib import Path
from math import cos, pi, sin
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/motionui/textures/gui/icons"
SCALE = 4
SIZE = 32


def canvas():
    return Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))


def finish(image, name):
    OUT.mkdir(parents=True, exist_ok=True)
    image.resize((SIZE, SIZE), Image.Resampling.LANCZOS).save(OUT / name, optimize=True)


def arrow():
    image = canvas()
    draw = ImageDraw.Draw(image)
    s = SCALE
    color = (255, 255, 255, 255)
    width = 3 * s
    # Exact geometry of arrow-up-svgrepo-com.svg, scaled from viewBox 24 to 32.
    points = [(16 * s, 7 * s), (16 * s, 25 * s)]
    draw.line(points, fill=color, width=width)
    draw.line([(16 * s, 7 * s), (8 * s, 15 * s)], fill=color, width=width)
    draw.line([(16 * s, 7 * s), (24 * s, 15 * s)], fill=color, width=width)
    radius = width // 2
    for x, y in [(16, 7), (16, 25), (8, 15), (24, 15)]:
        draw.ellipse((x * s - radius, y * s - radius, x * s + radius, y * s + radius), fill=color)
    finish(image, "arrow.png")


def gear():
    image = canvas()
    draw = ImageDraw.Draw(image)
    s = SCALE
    color = (255, 255, 255, 255)
    # Eight-tooth outline derived from setting-svgrepo-com.svg.
    points = []
    for i in range(32):
        angle = -pi / 2 + i * pi / 16
        phase = i % 4
        radius = 14 if phase in (0, 1) else 11
        points.append((16 * s + cos(angle) * radius * s, 16 * s + sin(angle) * radius * s))
    draw.polygon(points, fill=color)
    draw.ellipse((7 * s, 7 * s, 25 * s, 25 * s), fill=(0, 0, 0, 0))
    draw.ellipse((11 * s, 11 * s, 21 * s, 21 * s), fill=color)
    draw.ellipse((14 * s, 14 * s, 18 * s, 18 * s), fill=(0, 0, 0, 0))
    finish(image, "gear.png")


def star(active):
    image = canvas()
    draw = ImageDraw.Draw(image)
    s = SCALE
    color = (255, 255, 255, 255)
    # Pixel adaptation of star-svgrepo-com.svg (24x24 viewBox), inset for filtering.
    source = [(12, 3.5), (14.7, 9.0), (20.8, 9.7), (16.3, 14.1), (17.5, 20.4),
              (12, 17.4), (6.5, 20.4), (7.7, 14.1), (3.2, 9.7), (9.3, 9.0)]
    points = [(x * 4 / 3 * s, y * 4 / 3 * s) for x, y in source]
    if active:
        draw.polygon(points, fill=color)
    else:
        closed = points + [points[0]]
        draw.line(closed, fill=color, width=2 * s, joint="curve")
        radius = s
        for x, y in points:
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=color)
    finish(image, "star_active.png" if active else "star.png")


if __name__ == "__main__":
    arrow()
    gear()
    star(False)
    star(True)
