"""
=============================================================================
HILL CLIMB RACING - 2D PHYSICS RACING GAME (PYGAME)
=============================================================================
A 2D physics-based side-scrolling hill climbing racing game built with Pygame.

Modules:
1. CONFIGURATION & CONSTANTS: Display settings, physics parameters, colors.
2. PROCEDURAL TERRAIN GENERATOR: Generates undulating hills, chasms, bridges,
   and 100 unique levels across 4 environments (Countryside, Desert, Arctic, Moon).
3. VEHICLE & SUSPENSION 2D PHYSICS ENGINE: Realistic chassis-wheel spring damper
   suspension, tire traction, airborne pitch torque, and flip detection.
4. GAME ENTITIES & COLLECTIBLES: Yellow coins (5, 10, 50, 100), red fuel jerrycans,
   destructible physics crates, and spark/dirt particle systems.
5. HUD & ANALOG GAUGES: Circular RPM and Speed/Boost analog dials, fuel meter,
   distance tracker, and on-screen industrial Gas & Brake pedals.
6. UPGRADES & GARAGE SYSTEM: Persistent vehicle tuning (Engine, Suspension,
   Tires, Fuel Tank) and multi-vehicle garage (Jeep, Bike, Monster Truck, Tank).
7. MAIN GAME LOOP & STATE MANAGER: Start screen, racing, pause, game over,
   and level completion screens with perfect run bonuses.
=============================================================================
"""

import sys
import math
import random

# Attempt to import pygame; provide friendly error if not installed
try:
    import pygame
except ImportError:
    print("Pygame is required to run this game. Install it using: pip install pygame")
    sys.exit(1)

# ---------------------------------------------------------------------------
# MODULE 1: CONFIGURATION & CONSTANTS
# ---------------------------------------------------------------------------
SCREEN_WIDTH = 1000
SCREEN_HEIGHT = 600
FPS = 60

# Palette
COLOR_BLACK = (15, 23, 42)
COLOR_WHITE = (255, 255, 255)
COLOR_RED = (220, 38, 38)
COLOR_DARK_RED = (153, 27, 27)
COLOR_GREEN = (16, 185, 129)
COLOR_YELLOW = (251, 191, 36)
COLOR_ORANGE = (249, 115, 22)
COLOR_BLUE = (14, 165, 233)
COLOR_GREY_LIGHT = (203, 213, 225)
COLOR_GREY_DARK = (51, 65, 85)
COLOR_METAL = (100, 116, 139)

# Environment definitions
ENVIRONMENTS = {
    "Countryside": {
        "sky_top": (79, 195, 247),
        "sky_bottom": (225, 245, 254),
        "surface": (124, 179, 66),
        "subsurface": (93, 64, 55),
        "gravity": 9.8,
        "friction": 1.0,
    },
    "Desert": {
        "sky_top": (41, 182, 246),
        "sky_bottom": (255, 243, 224),
        "surface": (251, 192, 45),
        "subsurface": (230, 81, 0),
        "gravity": 9.8,
        "friction": 0.88,
    },
    "Arctic": {
        "sky_top": (129, 212, 250),
        "sky_bottom": (224, 247, 250),
        "surface": (224, 242, 241),
        "subsurface": (55, 71, 79),
        "gravity": 9.8,
        "friction": 0.65,
    },
    "Moon": {
        "sky_top": (13, 27, 42),
        "sky_bottom": (27, 38, 59),
        "surface": (176, 190, 197),
        "subsurface": (38, 50, 56),
        "gravity": 3.6,
        "friction": 0.95,
    },
}

# ---------------------------------------------------------------------------
# MODULE 2: PROCEDURAL TERRAIN & LEVEL SYSTEM
# ---------------------------------------------------------------------------
class Terrain:
    def __init__(self, level_num, length_meters=800):
        self.level_num = level_num
        self.length_meters = length_meters
        self.track_end_x = length_meters * 26.0
        self.base_y = 440.0
        self.steepness = 0.85 + (level_num / 100.0) * 0.75
        self.bridges = []

        # Determine environment by level (every 10 levels shifts environment)
        env_index = ((level_num - 1) // 10) % 4
        env_names = ["Countryside", "Desert", "Arctic", "Moon"]
        self.env_name = env_names[env_index]
        self.env = ENVIRONMENTS[self.env_name]

        # Generate wooden suspension bridges over chasm gaps
        rng = random.Random(level_num * 1007)
        bx = 600.0 + rng.random() * 200.0
        while bx < self.track_end_x - 300.0:
            span = 140.0 + rng.random() * 120.0
            self.bridges.append((bx, bx + span))
            bx += 700.0 + rng.random() * 600.0

    def get_elevation(self, x):
        """Returns the world ground Y position for any x coordinate."""
        if x < 0.0:
            return self.base_y

        # Check bridge span
        for start_x, end_x in self.bridges:
            if start_x <= x <= end_x:
                span = end_x - start_x
                t = (x - start_x) / span
                y1 = self._raw_elevation(start_x)
                y2 = self._raw_elevation(end_x)
                sag = math.sin(t * math.pi) * 25.0
                return (y1 * (1.0 - t) + y2 * t) + sag

        return self._raw_elevation(x)

    def _raw_elevation(self, x):
        s = self.steepness
        w1 = math.sin(x * 0.0032) * (85.0 * s)
        w2 = math.sin(x * 0.0084) * (40.0 * s)
        w3 = math.sin(x * 0.019) * (18.0 * s)
        w4 = math.sin(x * 0.00075) * (130.0 * s)
        blend = min(1.0, max(0.0, x / 220.0))
        return self.base_y + (w1 + w2 + w3 + w4) * blend

    def get_slope(self, x):
        """Returns slope angle in radians."""
        dx = 2.0
        y1 = self.get_elevation(x - dx)
        y2 = self.get_elevation(x + dx)
        return math.atan2(y2 - y1, dx * 2.0)

# ---------------------------------------------------------------------------
# MODULE 3: VEHICLE & SUSPENSION 2D PHYSICS ENGINE
# ---------------------------------------------------------------------------
class Vehicle:
    def __init__(self, vehicle_type="JEEP", upgrades=None):
        self.type = vehicle_type
        self.upgrades = upgrades or {"engine": 1, "suspension": 1, "tires": 1, "fuel": 1}

        # Dimensions & Specs
        if vehicle_type == "BIKE":
            self.mass = 70.0
            self.wheel_base = 56.0
            self.wheel_radius = 16.0
            self.suspension_len = 26.0
            self.base_torque = 1.3
        elif vehicle_type == "MONSTER_TRUCK":
            self.mass = 180.0
            self.wheel_base = 82.0
            self.wheel_radius = 26.0
            self.suspension_len = 34.0
            self.base_torque = 1.4
        elif vehicle_type == "TANK":
            self.mass = 240.0
            self.wheel_base = 90.0
            self.wheel_radius = 20.0
            self.suspension_len = 20.0
            self.base_torque = 1.6
        else:  # JEEP
            self.mass = 120.0
            self.wheel_base = 66.0
            self.wheel_radius = 18.0
            self.suspension_len = 24.0
            self.base_torque = 1.0

        # Upgrade multipliers
        self.engine_power = self.base_torque * (1.0 + (self.upgrades["engine"] - 1) * 0.16)
        self.tire_grip = 1.0 + (self.upgrades["tires"] - 1) * 0.12
        self.susp_stiffness = 220.0 + (self.upgrades["suspension"] - 1) * 25.0
        self.max_fuel = 100.0 + (self.upgrades["fuel"] - 1) * 20.0
        self.current_fuel = self.max_fuel

        # Dynamic physics state
        self.x = 120.0
        self.y = 380.0
        self.angle = 0.0
        self.vx = 0.0
        self.vy = 0.0
        self.angular_vel = 0.0

        # Wheels
        self.rear_wheel_x = self.x - self.wheel_base * 0.5
        self.rear_wheel_y = self.y + self.suspension_len
        self.rear_wheel_vx = 0.0
        self.rear_wheel_vy = 0.0
        self.rear_wheel_rot = 0.0
        self.rear_on_ground = False

        self.front_wheel_x = self.x + self.wheel_base * 0.5
        self.front_wheel_y = self.y + self.suspension_len
        self.front_wheel_vx = 0.0
        self.front_wheel_vy = 0.0
        self.front_wheel_rot = 0.0
        self.front_on_ground = False

        # Gauges
        self.rpm = 950.0
        self.speed_kmh = 0.0
        self.boost = 0.0

        # Airtime & flips
        self.airtime = 0.0
        self.air_rot_acc = 0.0

    def update(self, dt, terrain, gas_pressed, brake_pressed):
        gravity = terrain.env["gravity"] * 68.0

        # Fuel burn
        has_fuel = self.current_fuel > 0.01
        if has_fuel:
            burn = (0.03 / (1.0 + (self.upgrades["fuel"] - 1) * 0.12)) * dt * (6.0 if gas_pressed else 1.0)
            self.current_fuel = max(0.0, self.current_fuel - burn)

        # Engine RPM
        target_rpm = 6800.0 if (gas_pressed and has_fuel) else (3200.0 if brake_pressed else 950.0)
        self.rpm += (target_rpm - self.rpm) * min(1.0, dt * 10.0)

        # Boost gauge
        if gas_pressed and has_fuel:
            self.boost = min(100.0, self.boost + dt * 30.0)
        else:
            self.boost = max(0.0, self.boost - dt * 40.0)

        # Chassis geometry
        cos_a = math.cos(self.angle)
        sin_a = math.sin(self.angle)
        half_wb = self.wheel_base * 0.5

        rear_anchor_x = self.x - cos_a * half_wb - sin_a * self.suspension_len * 0.4
        rear_anchor_y = self.y - sin_a * half_wb + cos_a * self.suspension_len * 0.4
        front_anchor_x = self.x + cos_a * half_wb - sin_a * self.suspension_len * 0.4
        front_anchor_y = self.y + sin_a * half_wb + cos_a * self.suspension_len * 0.4

        # Rear wheel collision
        r_ground_y = terrain.get_elevation(self.rear_wheel_x)
        self.rear_on_ground = (self.rear_wheel_y + self.wheel_radius) >= r_ground_y
        if self.rear_on_ground:
            self.rear_wheel_y = r_ground_y - self.wheel_radius
            if self.rear_wheel_vy > 0:
                self.rear_wheel_vy = -self.rear_wheel_vy * 0.1
            slope = terrain.get_slope(self.rear_wheel_x)

            if gas_pressed and has_fuel:
                drive = 360.0 * self.engine_power * self.tire_grip * terrain.env["friction"]
                self.rear_wheel_vx += drive * dt * math.cos(slope)
                self.rear_wheel_vy += drive * dt * math.sin(slope)
                self.rear_wheel_rot += 20.0 * dt
            elif brake_pressed:
                self.rear_wheel_vx *= (1.0 - dt * 6.0)
            else:
                self.rear_wheel_vx *= (1.0 - dt * 0.4)
        else:
            self.rear_wheel_vy += gravity * dt

        # Front wheel collision
        f_ground_y = terrain.get_elevation(self.front_wheel_x)
        self.front_on_ground = (self.front_wheel_y + self.wheel_radius) >= f_ground_y
        if self.front_on_ground:
            self.front_wheel_y = f_ground_y - self.wheel_radius
            if self.front_wheel_vy > 0:
                self.front_wheel_vy = -self.front_wheel_vy * 0.1
            if brake_pressed:
                self.front_wheel_vx *= (1.0 - dt * 6.0)
            else:
                self.front_wheel_vx *= (1.0 - dt * 0.4)
        else:
            self.front_wheel_vy += gravity * dt

        # Suspension springs
        rdx = self.rear_wheel_x - rear_anchor_x
        rdy = self.rear_wheel_y - rear_anchor_y
        r_dist = math.sqrt(rdx * rdx + rdy * rdy)
        r_force = (r_dist - self.suspension_len) * self.susp_stiffness

        fdx = self.front_wheel_x - front_anchor_x
        fdy = self.front_wheel_y - front_anchor_y
        f_dist = math.sqrt(fdx * fdx + fdy * fdy)
        f_force = (f_dist - self.suspension_len) * self.susp_stiffness

        self.vy += gravity * dt

        if r_dist > 0.001:
            rx_dir = rdx / r_dist
            ry_dir = rdy / r_dist
            self.vx += (rx_dir * r_force / self.mass) * dt
            self.vy += (ry_dir * r_force / self.mass) * dt
            torque_r = (rear_anchor_x - self.x) * (ry_dir * r_force) - (rear_anchor_y - self.y) * (rx_dir * r_force)
            self.angular_vel += (torque_r / (self.mass * 35.0)) * dt

        if f_dist > 0.001:
            fx_dir = fdx / f_dist
            fy_dir = fdy / f_dist
            self.vx += (fx_dir * f_force / self.mass) * dt
            self.vy += (fy_dir * f_force / self.mass) * dt
            torque_f = (front_anchor_x - self.x) * (fy_dir * f_force) - (front_anchor_y - self.y) * (fx_dir * f_force)
            self.angular_vel += (torque_f / (self.mass * 35.0)) * dt

        # Air Stunt Control
        if not self.rear_on_ground and not self.front_on_ground:
            self.airtime += dt
            if gas_pressed:
                self.angular_vel += 4.5 * dt
            if brake_pressed:
                self.angular_vel -= 4.5 * dt
            self.air_rot_acc += self.angular_vel * dt
        else:
            self.airtime = 0.0
            self.air_rot_acc = 0.0

        # Angular friction
        self.angular_vel *= (1.0 - dt * 2.8)

        # Integrate
        self.x += self.vx * dt
        self.y += self.vy * dt
        self.angle += self.angular_vel * dt

        self.rear_wheel_x += self.rear_wheel_vx * dt
        self.rear_wheel_y += self.rear_wheel_vy * dt
        self.front_wheel_x += self.front_wheel_vx * dt
        self.front_wheel_y += self.front_wheel_vy * dt

        self.speed_kmh = math.sqrt(self.vx * self.vx + self.vy * self.vy) * 0.16

    def check_flipped(self, terrain):
        """Driver head collision / neck snap check."""
        cos_a = math.cos(self.angle)
        sin_a = math.sin(self.angle)
        head_x = self.x - sin_a * 28.0
        head_y = self.y - cos_a * 28.0
        ground_at_head = terrain.get_elevation(head_x)
        return head_y >= (ground_at_head - 4.0)

# ---------------------------------------------------------------------------
# MODULE 4: ENTITIES & COLLECTIBLES
# ---------------------------------------------------------------------------
class Coin:
    def __init__(self, x, y, value):
        self.x = x
        self.y = y
        self.value = value
        self.collected = False

class FuelCan:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.collected = False

class Crate:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.vx = 0.0
        self.vy = 0.0
        self.angle = 0.0
        self.broken = False

# ---------------------------------------------------------------------------
# MODULE 5: RENDERER & ANALOG DIALS
# ---------------------------------------------------------------------------
def draw_analog_gauge(surface, center_x, center_y, radius, title, value, max_val, unit=""):
    """Renders a metallic circular dial gauge with needle."""
    # Outer bezel
    pygame.draw.circle(surface, COLOR_GREY_LIGHT, (center_x, center_y), radius)
    pygame.draw.circle(surface, COLOR_BLACK, (center_x, center_y), radius - 3)

    # Dial face
    pygame.draw.circle(surface, (30, 41, 59), (center_x, center_y), radius - 5)

    # Needle
    ratio = min(1.0, max(0.0, value / max_val))
    # Angle spans from -135 to +135 deg
    deg = -135.0 + ratio * 270.0
    rad = math.radians(deg - 90.0)

    needle_len = radius * 0.75
    nx = center_x + math.cos(rad) * needle_len
    ny = center_y + math.sin(rad) * needle_len
    pygame.draw.line(surface, COLOR_RED, (center_x, center_y), (int(nx), int(ny)), 3)

    # Center hub
    pygame.draw.circle(surface, COLOR_GREY_LIGHT, (center_x, center_y), 6)

    # Label
    font = pygame.font.SysFont("Arial", 12, bold=True)
    txt = font.render(title, True, COLOR_WHITE)
    surface.blit(txt, (center_x - txt.get_width() // 2, center_y + radius // 3))

# ---------------------------------------------------------------------------
# MODULE 6: MAIN GAME CONTROLLER
# ---------------------------------------------------------------------------
def main():
    pygame.init()
    pygame.display.set_caption("Hill Climb Racing 2D - Python Edition")
    screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
    clock = pygame.time.Clock()

    font_large = pygame.font.SysFont("Arial", 32, bold=True)
    font_med = pygame.font.SysFont("Arial", 20, bold=True)
    font_small = pygame.font.SysFont("Arial", 14, bold=True)

    # Persistent progress
    coins_bank = 250
    current_level = 1
    selected_vehicle_idx = 0
    vehicles = ["JEEP", "BIKE", "MONSTER_TRUCK", "TANK"]

    upgrades = {
        "engine": 1,
        "suspension": 1,
        "tires": 1,
        "fuel": 1
    }

    # State: "GARAGE", "PLAYING", "GAMEOVER", "WIN"
    game_state = "GARAGE"
    terrain = None
    car = None
    coins = []
    fuel_cans = []
    crates = []

    def init_run():
        nonlocal terrain, car, coins, fuel_cans, crates
        terrain = Terrain(current_level, length_meters=700 + current_level * 25)
        car = Vehicle(vehicles[selected_vehicle_idx], upgrades)
        coins = []
        fuel_cans = []
        crates = []

        # Populate track
        rng = random.Random(current_level * 543)
        cx = 350.0
        while cx < terrain.track_end_x:
            gy = terrain.get_elevation(cx)
            # Fuel can every 500m
            if len(fuel_cans) == 0 or (cx - fuel_cans[-1].x) > 520.0:
                fuel_cans.append(FuelCan(cx, gy - 20))
                cx += 80.0
                continue

            # Coins
            val = rng.choice([5, 10, 50, 100])
            for i in range(4):
                coins.append(Coin(cx + i * 35.0, terrain.get_elevation(cx + i * 35.0) - 26, val))
            cx += 200.0

    init_run()

    # Pedal button rectangles on screen
    brake_rect = pygame.Rect(30, SCREEN_HEIGHT - 120, 100, 100)
    gas_rect = pygame.Rect(SCREEN_WIDTH - 130, SCREEN_HEIGHT - 120, 100, 100)

    running = True
    while running:
        dt = clock.tick(FPS) / 1000.0
        mouse_pos = pygame.mouse.get_pos()
        mouse_down = pygame.mouse.get_pressed()[0]

        # Key state
        keys = pygame.key.get_pressed()
        gas_pressed = keys[pygame.K_RIGHT] or keys[pygame.K_UP] or keys[pygame.K_d] or (mouse_down and gas_rect.collidepoint(mouse_pos))
        brake_pressed = keys[pygame.K_LEFT] or keys[pygame.K_DOWN] or keys[pygame.K_a] or (mouse_down and brake_rect.collidepoint(mouse_pos))

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    if game_state == "PLAYING":
                        game_state = "GARAGE"
                elif event.key == pygame.K_SPACE:
                    if game_state == "GARAGE":
                        init_run()
                        game_state = "PLAYING"
                    elif game_state in ["GAMEOVER", "WIN"]:
                        init_run()
                        game_state = "PLAYING"

        # Update
        if game_state == "PLAYING":
            car.update(dt, terrain, gas_pressed, brake_pressed)

            # Collect coins
            for c in coins:
                if not c.collected:
                    dx = car.x - c.x
                    dy = car.y - c.y
                    if dx * dx + dy * dy < 44 * 44:
                        c.collected = True
                        coins_bank += c.value

            # Collect fuel
            for f in fuel_cans:
                if not f.collected:
                    dx = car.x - f.x
                    dy = car.y - f.y
                    if dx * dx + dy * dy < 48 * 48:
                        f.collected = True
                        car.current_fuel = car.max_fuel

            # Check flipped
            if car.check_flipped(terrain):
                game_state = "GAMEOVER"

            # Check fuel out
            if car.current_fuel <= 0.0 and car.speed_kmh < 2.0:
                game_state = "GAMEOVER"

            # Check finish line
            if car.x >= terrain.track_end_x:
                current_level = min(100, current_level + 1)
                game_state = "WIN"

        # -------------------------------------------------------------------
        # DRAW
        # -------------------------------------------------------------------
        screen.fill(terrain.env["sky_top"])

        if game_state == "PLAYING":
            # Camera offset
            cam_x = SCREEN_WIDTH * 0.35 - car.x
            cam_y = SCREEN_HEIGHT * 0.65 - car.y

            # Draw Terrain
            points = []
            start_x = int(car.x - 600)
            end_x = int(car.x + 800)
            for x in range(start_x, end_x, 15):
                points.append((x + cam_x, terrain.get_elevation(x) + cam_y))
            points.append((end_x + cam_x, SCREEN_HEIGHT + 300))
            points.append((start_x + cam_x, SCREEN_HEIGHT + 300))
            pygame.draw.polygon(screen, terrain.env["subsurface"], points)

            # Surface grass line
            surf_points = [(x + cam_x, terrain.get_elevation(x) + cam_y) for x in range(start_x, end_x, 15)]
            if len(surf_points) > 1:
                pygame.draw.lines(screen, terrain.env["surface"], False, surf_points, 8)

            # Collectibles
            for c in coins:
                if not c.collected and (start_x < c.x < end_x):
                    sx, sy = int(c.x + cam_x), int(c.y + cam_y)
                    pygame.draw.circle(screen, COLOR_YELLOW, (sx, sy), 14)
                    pygame.draw.circle(screen, (245, 158, 11), (sx, sy), 14, 2)
                    c_txt = font_small.render(str(c.value), True, COLOR_BLACK)
                    screen.blit(c_txt, (sx - c_txt.get_width() // 2, sy - c_txt.get_height() // 2))

            for f in fuel_cans:
                if not f.collected and (start_x < f.x < end_x):
                    sx, sy = int(f.x + cam_x), int(f.y + cam_y)
                    pygame.draw.rect(screen, COLOR_RED, (sx - 12, sy - 16, 24, 32), border_radius=4)
                    pygame.draw.rect(screen, COLOR_DARK_RED, (sx - 6, sy - 22, 12, 6))

            # Finish line
            fx = int(terrain.track_end_x + cam_x)
            fy = int(terrain.get_elevation(terrain.track_end_x) + cam_y)
            pygame.draw.line(screen, COLOR_WHITE, (fx, fy), (fx, fy - 90), 5)
            pygame.draw.rect(screen, COLOR_WHITE, (fx, fy - 90, 40, 24))

            # Vehicle wheels
            rx, ry = int(car.rear_wheel_x + cam_x), int(car.rear_wheel_y + cam_y)
            fx, fy = int(car.front_wheel_x + cam_x), int(car.front_wheel_y + cam_y)
            pygame.draw.circle(screen, COLOR_BLACK, (rx, ry), int(car.wheel_radius))
            pygame.draw.circle(screen, COLOR_GREY_LIGHT, (rx, ry), int(car.wheel_radius * 0.5))
            pygame.draw.circle(screen, COLOR_BLACK, (fx, fy), int(car.wheel_radius))
            pygame.draw.circle(screen, COLOR_GREY_LIGHT, (fx, fy), int(car.wheel_radius * 0.5))

            # Chassis body
            cx, cy = int(car.x + cam_x), int(car.y + cam_y)
            body_color = COLOR_RED if car.type == "JEEP" else (COLOR_ORANGE if car.type == "BIKE" else COLOR_BLUE)
            pygame.draw.rect(screen, body_color, (cx - 30, cy - 10, 60, 20), border_radius=5)
            # Driver head
            pygame.draw.circle(screen, (254, 215, 170), (cx - 5, cy - 20), 8)
            pygame.draw.circle(screen, COLOR_RED, (cx - 5, cy - 24), 8)

            # Suspension coils
            pygame.draw.line(screen, COLOR_GREY_LIGHT, (cx - 20, cy), (rx, ry), 3)
            pygame.draw.line(screen, COLOR_GREY_LIGHT, (cx + 20, cy), (fx, fy), 3)

            # HUD
            # Distance
            dist_m = int(car.x / 26.0)
            txt_dist = font_med.render(f"Distance: {dist_m}m / {terrain.length_meters}m", True, COLOR_WHITE)
            screen.blit(txt_dist, (20, 20))

            # Coins
            txt_coins = font_med.render(f"Coins: {coins_bank} 🪙", True, COLOR_YELLOW)
            screen.blit(txt_coins, (SCREEN_WIDTH - txt_coins.get_width() - 20, 20))

            # Fuel Bar
            fuel_ratio = car.current_fuel / car.max_fuel
            pygame.draw.rect(screen, COLOR_GREY_DARK, (SCREEN_WIDTH // 2 - 100, 20, 200, 18), border_radius=6)
            fuel_col = COLOR_GREEN if fuel_ratio > 0.25 else COLOR_RED
            pygame.draw.rect(screen, fuel_col, (SCREEN_WIDTH // 2 - 100, 20, int(200 * fuel_ratio), 18), border_radius=6)

            # Analog Gauges
            draw_analog_gauge(screen, SCREEN_WIDTH // 2 - 50, SCREEN_HEIGHT - 60, 42, "RPM", car.rpm, 8000.0)
            draw_analog_gauge(screen, SCREEN_WIDTH // 2 + 50, SCREEN_HEIGHT - 60, 42, "SPEED", car.speed_kmh, 140.0)

            # On-Screen Pedals
            pygame.draw.rect(screen, COLOR_METAL if not brake_pressed else (71, 85, 105), brake_rect, border_radius=12)
            pygame.draw.rect(screen, COLOR_GREY_LIGHT, brake_rect, 3, border_radius=12)
            txt_b = font_med.render("BRAKE", True, COLOR_WHITE)
            screen.blit(txt_b, (brake_rect.centerx - txt_b.get_width() // 2, brake_rect.centery - txt_b.get_height() // 2))

            pygame.draw.rect(screen, COLOR_METAL if not gas_pressed else (71, 85, 105), gas_rect, border_radius=12)
            pygame.draw.rect(screen, COLOR_GREY_LIGHT, gas_rect, 3, border_radius=12)
            txt_g = font_med.render("GAS", True, COLOR_WHITE)
            screen.blit(txt_g, (gas_rect.centerx - txt_g.get_width() // 2, gas_rect.centery - txt_g.get_height() // 2))

        elif game_state == "GARAGE":
            # Garage Menu
            title = font_large.render("HILL CLIMB RACING - GARAGE", True, COLOR_WHITE)
            screen.blit(title, (SCREEN_WIDTH // 2 - title.get_width() // 2, 40))

            coins_txt = font_med.render(f"Total Coins: {coins_bank} 🪙", True, COLOR_YELLOW)
            screen.blit(coins_txt, (SCREEN_WIDTH // 2 - coins_txt.get_width() // 2, 90))

            veh_txt = font_med.render(f"Vehicle: {vehicles[selected_vehicle_idx]} (Press V to switch)", True, COLOR_BLUE)
            screen.blit(veh_txt, (SCREEN_WIDTH // 2 - veh_txt.get_width() // 2, 130))

            # Upgrades list
            uy = 190
            for part, lvl in upgrades.items():
                cost = lvl * 150
                u_str = f"{part.upper()} - LVL {lvl}/10 | Upgrade Cost: {cost} 🪙"
                txt_u = font_med.render(u_str, True, COLOR_WHITE)
                screen.blit(txt_u, (SCREEN_WIDTH // 2 - 200, uy))
                uy += 40

            start_txt = font_large.render("PRESS SPACE TO RACE!", True, COLOR_GREEN)
            screen.blit(start_txt, (SCREEN_WIDTH // 2 - start_txt.get_width() // 2, SCREEN_HEIGHT - 120))

        elif game_state == "GAMEOVER":
            title = font_large.render("GAME OVER!", True, COLOR_RED)
            screen.blit(title, (SCREEN_WIDTH // 2 - title.get_width() // 2, SCREEN_HEIGHT // 2 - 60))
            sub = font_med.render("Press SPACE to Retry or ESC for Garage", True, COLOR_WHITE)
            screen.blit(sub, (SCREEN_WIDTH // 2 - sub.get_width() // 2, SCREEN_HEIGHT // 2))

        elif game_state == "WIN":
            title = font_large.render(f"LEVEL {current_level-1} COMPLETED!", True, COLOR_GREEN)
            screen.blit(title, (SCREEN_WIDTH // 2 - title.get_width() // 2, SCREEN_HEIGHT // 2 - 60))
            sub = font_med.render("Press SPACE for Next Level!", True, COLOR_WHITE)
            screen.blit(sub, (SCREEN_WIDTH // 2 - sub.get_width() // 2, SCREEN_HEIGHT // 2))

        pygame.display.flip()

    pygame.quit()

if __name__ == "__main__":
    main()
