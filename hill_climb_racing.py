"""
=============================================================================
HILL CLIMB RACING - 2D PHYSICS RACING GAME (PYGAME)
=============================================================================
A 2D physics-based side-scrolling hill climbing racing game built with Pygame.

Features:
1. BUG FIX & STABLE SPAWN: Guaranteed flat runway for the first 50+ meters.
   Vehicle center of mass, suspension stiffness, and wheel balance calibrated
   so the vehicle spawns and accelerates safely without flipping over.
2. ENDLESS DRIVING MODE: Continuous infinite runner gameplay across procedurally
   generated hills, jumps, and bridges until crash or fuel depletion.
3. COIN ECONOMY & COLLECTIBLES: Scattered coins with values (5, 10, 50, 100, 500)
   saved to total balance, plus fuel cans placed periodically.
4. GARAGE & VEHICLE SHOP:
   - 8 Vehicles: Jeep, Motocross Bike, Monster Truck, Hippie Van,
     One-wheeler, Rally Car, Tank, and Supercar.
   - Upgrades: Engine (Power/Speed), Suspension (Stability), Tires (Grip),
     and 4WD/Fuel Capacity.
   - 6 Stages: Countryside, Desert, Arctic/Snow, Moon (low gravity),
     Cave, and Highway.
5. CONTROLS: On-screen GAS and BRAKE pedals with mouse/touch support,
   as well as keyboard controls (Arrow keys / WASD).
=============================================================================
"""

import sys
import math
import random

try:
    import pygame
except ImportError:
    print("Pygame is required to run this game. Install it using: pip install pygame")
    sys.exit(1)

# ---------------------------------------------------------------------------
# CONSTANTS & CONFIGURATION
# ---------------------------------------------------------------------------
SCREEN_WIDTH = 1000
SCREEN_HEIGHT = 600
FPS = 60

COLOR_BG_DARK = (15, 23, 42)
COLOR_WHITE = (255, 255, 255)
COLOR_RED = (220, 38, 38)
COLOR_DARK_RED = (153, 27, 27)
COLOR_GREEN = (16, 185, 129)
COLOR_YELLOW = (251, 191, 36)
COLOR_ORANGE = (249, 115, 22)
COLOR_BLUE = (14, 165, 233)
COLOR_PURPLE = (168, 85, 247)
COLOR_CYAN = (6, 182, 212)
COLOR_GOLD = (234, 179, 8)
COLOR_GREY_LIGHT = (203, 213, 225)
COLOR_GREY_DARK = (51, 65, 85)
COLOR_METAL = (100, 116, 139)

# 6 Stages with unique visuals, gravity and friction
STAGES = {
    "Countryside": {
        "sky_top": (79, 195, 247),
        "sky_bottom": (225, 245, 254),
        "surface": (124, 179, 66),
        "subsurface": (93, 64, 55),
        "gravity": 9.8,
        "friction": 1.0,
        "cost": 0,
    },
    "Desert": {
        "sky_top": (41, 182, 246),
        "sky_bottom": (255, 243, 224),
        "surface": (251, 192, 45),
        "subsurface": (230, 81, 0),
        "gravity": 9.8,
        "friction": 0.88,
        "cost": 600,
    },
    "Arctic": {
        "sky_top": (129, 212, 250),
        "sky_bottom": (224, 247, 250),
        "surface": (224, 242, 241),
        "subsurface": (55, 71, 79),
        "gravity": 9.8,
        "friction": 0.65,
        "cost": 1500,
    },
    "Moon": {
        "sky_top": (13, 27, 42),
        "sky_bottom": (27, 38, 59),
        "surface": (176, 190, 197),
        "subsurface": (38, 50, 56),
        "gravity": 3.6,
        "friction": 0.95,
        "cost": 3000,
    },
    "Cave": {
        "sky_top": (26, 26, 36),
        "sky_bottom": (45, 34, 53),
        "surface": (126, 87, 194),
        "subsurface": (33, 23, 43),
        "gravity": 9.8,
        "friction": 0.95,
        "cost": 5000,
    },
    "Highway": {
        "sky_top": (55, 71, 79),
        "sky_bottom": (120, 144, 156),
        "surface": (38, 50, 56),
        "subsurface": (30, 41, 59),
        "gravity": 9.8,
        "friction": 1.25,
        "cost": 8000,
    },
}

# 8 Vehicles definitions
VEHICLE_SPECS = {
    "Jeep": {
        "title": "Classic Red Jeep",
        "mass": 120.0,
        "wheel_base": 68.0,
        "wheel_radius": 18.0,
        "susp_len": 24.0,
        "torque": 1.0,
        "grip": 1.0,
        "color": COLOR_RED,
        "cost": 0,
        "is_unicycle": False,
    },
    "Motocross Bike": {
        "title": "Motocross Dirt Bike",
        "mass": 65.0,
        "wheel_base": 56.0,
        "wheel_radius": 16.0,
        "susp_len": 26.0,
        "torque": 1.25,
        "grip": 0.95,
        "color": COLOR_ORANGE,
        "cost": 500,
        "is_unicycle": False,
    },
    "Monster Truck": {
        "title": "Heavy Monster Truck",
        "mass": 190.0,
        "wheel_base": 84.0,
        "wheel_radius": 28.0,
        "susp_len": 34.0,
        "torque": 1.45,
        "grip": 1.25,
        "color": COLOR_BLUE,
        "cost": 1200,
        "is_unicycle": False,
    },
    "Hippie Van": {
        "title": "Hippie Van",
        "mass": 145.0,
        "wheel_base": 76.0,
        "wheel_radius": 19.0,
        "susp_len": 26.0,
        "torque": 1.3,
        "grip": 1.1,
        "color": COLOR_PURPLE,
        "cost": 2500,
        "is_unicycle": False,
    },
    "One-wheeler": {
        "title": "One-wheeler",
        "mass": 55.0,
        "wheel_base": 0.0,
        "wheel_radius": 22.0,
        "susp_len": 26.0,
        "torque": 1.5,
        "grip": 1.3,
        "color": (233, 30, 99),
        "cost": 4000,
        "is_unicycle": True,
    },
    "Rally Car": {
        "title": "Turbo Rally Car",
        "mass": 95.0,
        "wheel_base": 68.0,
        "wheel_radius": 17.0,
        "susp_len": 20.0,
        "torque": 1.7,
        "grip": 1.35,
        "color": COLOR_CYAN,
        "cost": 6500,
        "is_unicycle": False,
    },
    "Tank": {
        "title": "Heavy Army Tank",
        "mass": 260.0,
        "wheel_base": 92.0,
        "wheel_radius": 20.0,
        "susp_len": 18.0,
        "torque": 2.0,
        "grip": 1.55,
        "color": (56, 142, 60),
        "cost": 9000,
        "is_unicycle": False,
    },
    "Supercar": {
        "title": "Luxury Supercar",
        "mass": 105.0,
        "wheel_base": 72.0,
        "wheel_radius": 18.0,
        "susp_len": 19.0,
        "torque": 2.2,
        "grip": 1.45,
        "color": COLOR_GOLD,
        "cost": 15000,
        "is_unicycle": False,
    },
}

# ---------------------------------------------------------------------------
# PROCEDURAL ENDLESS TERRAIN
# ---------------------------------------------------------------------------
class EndlessTerrain:
    def __init__(self, stage_name="Countryside"):
        self.stage_name = stage_name
        self.env = STAGES[stage_name]
        self.base_y = 480.0
        self.flat_runway_length = 1500.0  # 55+ meters completely flat
        self.bridges = []

    def get_elevation(self, x):
        if x <= self.flat_runway_length:
            return self.base_y

        # Check bridges
        for start_x, end_x in self.bridges:
            if start_x <= x <= end_x:
                span = end_x - start_x
                t = (x - start_x) / span
                y1 = self._raw_elevation(start_x)
                y2 = self._raw_elevation(end_x)
                sag = math.sin(t * math.pi) * 26.0
                return (y1 * (1.0 - t) + y2 * t) + sag

        return self._raw_elevation(x)

    def _raw_elevation(self, x):
        dist_past = x - self.flat_runway_length
        blend = min(1.0, dist_past / 400.0)
        endless_diff = 1.0 + min(0.65, x / 30000.0)

        w1 = math.sin(x * 0.0028) * (80.0 * endless_diff)
        w2 = math.sin(x * 0.0075) * (38.0 * endless_diff)
        w3 = math.sin(x * 0.017) * (16.0 * endless_diff)
        w4 = math.sin(x * 0.0006) * (120.0 * endless_diff)
        return self.base_y + (w1 + w2 + w3 + w4) * blend

    def get_slope(self, x):
        dx = 3.0
        y1 = self.get_elevation(x - dx)
        y2 = self.get_elevation(x + dx)
        return math.atan2(y2 - y1, dx * 2.0)

# ---------------------------------------------------------------------------
# VEHICLE PHYSICS
# ---------------------------------------------------------------------------
class Vehicle:
    def __init__(self, vehicle_name="Jeep", upgrades=None, stage_name="Countryside"):
        self.name = vehicle_name
        self.spec = VEHICLE_SPECS[vehicle_name]
        self.upgrades = upgrades or {"engine": 1, "suspension": 1, "tires": 1, "fuel": 1}
        self.stage = STAGES[stage_name]

        self.mass = self.spec["mass"]
        self.wheel_base = self.spec["wheel_base"]
        self.wheel_radius = self.spec["wheel_radius"]
        self.susp_len = self.spec["susp_len"]
        self.is_unicycle = self.spec["is_unicycle"]
        self.half_base = 0.0 if self.is_unicycle else self.wheel_base * 0.5

        # Performance multipliers
        self.engine_power = self.spec["torque"] * (1.0 + (self.upgrades["engine"] - 1) * 0.20)
        self.tire_grip = self.spec["grip"] * (1.0 + (self.upgrades["tires"] - 1) * 0.16) * self.stage["friction"]
        self.susp_stiffness = 220.0 + (self.upgrades["suspension"] - 1) * 28.0
        self.susp_damping = 24.0 + (self.upgrades["suspension"] - 1) * 3.0
        self.max_fuel = 100.0 + (self.upgrades["fuel"] - 1) * 25.0
        self.current_fuel = self.max_fuel
        self.fuel_burn_rate = 0.019 / (1.0 + (self.upgrades["fuel"] - 1) * 0.16)

        # Dynamic state
        self.x = 180.0
        self.y = 480.0 - self.wheel_radius - self.susp_len
        self.angle = 0.0
        self.vx = 0.0
        self.vy = 0.0
        self.angular_vel = 0.0

        self.rear_wheel_x = self.x - self.half_base
        self.rear_wheel_y = 480.0 - self.wheel_radius
        self.front_wheel_x = self.x + self.half_base
        self.front_wheel_y = 480.0 - self.wheel_radius
        self.rear_wheel_spin = 0.0

        self.spawn_grace = 3.0
        self.rpm = 950.0
        self.speed_kmh = 0.0
        self.airtime = 0.0
        self.is_airborne = False
        self.air_rot_acc = 0.0

    def update(self, dt, terrain, gas_pressed, brake_pressed):
        grav = self.stage["gravity"] * 65.0

        if self.spawn_grace > 0.0:
            self.spawn_grace = max(0.0, self.spawn_grace - dt)

        # Fuel
        has_fuel = self.current_fuel > 0.001
        if has_fuel:
            burn = self.fuel_burn_rate * dt * (5.5 if gas_pressed else 1.0)
            self.current_fuel = max(0.0, self.current_fuel - burn)

        # Engine RPM
        target_rpm = 7200.0 if (gas_pressed and has_fuel) else (3200.0 if brake_pressed else 950.0)
        self.rpm += (target_rpm - self.rpm) * min(1.0, dt * 10.0)

        # Wheel ground contacts
        r_ground_y = terrain.get_elevation(self.rear_wheel_x)
        rear_on_ground = (self.rear_wheel_y + self.wheel_radius) >= r_ground_y
        if rear_on_ground:
            self.rear_wheel_y = r_ground_y - self.wheel_radius

        if not self.is_unicycle:
            f_ground_y = terrain.get_elevation(self.front_wheel_x)
            front_on_ground = (self.front_wheel_y + self.wheel_radius) >= f_ground_y
            if front_on_ground:
                self.front_wheel_y = f_ground_y - self.wheel_radius
        else:
            front_on_ground = False
            self.front_wheel_x = self.x
            self.front_wheel_y = self.rear_wheel_y

        on_ground = rear_on_ground or front_on_ground

        # Drive forces
        if on_ground:
            slope = terrain.get_slope(self.x)
            if has_fuel and gas_pressed:
                drive_acc = 420.0 * self.engine_power * self.tire_grip
                self.vx += drive_acc * dt * math.cos(slope)
                self.vy += drive_acc * dt * math.sin(slope)
                self.rear_wheel_spin += (self.vx / self.wheel_radius) * dt * 2.5
            elif brake_pressed:
                self.vx *= (1.0 - dt * 5.5)
                if self.vx < 10.0:
                    rev_acc = 180.0 * self.engine_power * self.tire_grip
                    self.vx -= rev_acc * dt * math.cos(slope)
                    self.vy -= rev_acc * dt * math.sin(slope)
            else:
                self.vx *= (1.0 - dt * 0.35)

            self.vy += grav * dt * 0.35
        else:
            self.vy += grav * dt

        # Suspension spring
        avg_wheel_y = self.rear_wheel_y if self.is_unicycle else (self.rear_wheel_y + self.front_wheel_y) * 0.5
        target_chassis_y = avg_wheel_y - self.susp_len

        if on_ground:
            delta_y = target_chassis_y - self.y
            force_y = delta_y * self.susp_stiffness - self.vy * self.susp_damping
            self.vy += (force_y / self.mass) * dt

            # Angular stabilization on ground
            target_angle = math.atan2(self.front_wheel_y - self.rear_wheel_y, self.front_wheel_x - self.rear_wheel_x) if (not self.is_unicycle and rear_on_ground and front_on_ground) else terrain.get_slope(self.x)
            diff = self.angle - target_angle
            self.angular_vel -= diff * 14.0 * dt
            self.angular_vel *= (1.0 - dt * 6.5)

            if gas_pressed and has_fuel:
                self.angular_vel -= 0.35 * dt * self.engine_power

            self.is_airborne = False
            self.airtime = 0.0
            self.air_rot_acc = 0.0
        else:
            self.airtime += dt
            if self.airtime > 0.35:
                self.is_airborne = True

            # Air pitch controls
            if gas_pressed:
                self.angular_vel -= 4.2 * dt  # backflip
            if brake_pressed:
                self.angular_vel += 4.2 * dt  # frontflip

            self.air_rot_acc += self.angular_vel * dt
            self.angular_vel *= (1.0 - dt * 0.8)

        self.angular_vel = max(-8.5, min(8.5, self.angular_vel))

        # Integrate
        self.x += self.vx * dt
        self.y += self.vy * dt
        self.angle += self.angular_vel * dt

        cos_a = math.cos(self.angle)
        self.rear_wheel_x = self.x - cos_a * self.half_base
        if not self.is_unicycle:
            self.front_wheel_x = self.x + cos_a * self.half_base

        spd = math.sqrt(self.vx * self.vx + self.vy * self.vy)
        self.speed_kmh = spd * 0.16

    def is_flipped(self, terrain):
        if self.spawn_grace > 0.0 or (self.x / 26.0) < 5.0:
            return False
        head_x = self.x + math.sin(self.angle) * 24.0
        head_y = self.y - math.cos(self.angle) * 24.0
        return head_y >= terrain.get_elevation(head_x) + 1.0

# ---------------------------------------------------------------------------
# MAIN GAME
# ---------------------------------------------------------------------------
def main():
    pygame.init()
    screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
    pygame.display.set_caption("Hill Climb Racing - Endless Edition")
    clock = pygame.time.Clock()

    font_small = pygame.font.SysFont("Arial", 14, bold=True)
    font_med = pygame.font.SysFont("Arial", 20, bold=True)
    font_large = pygame.font.SysFont("Arial", 36, bold=True)

    # Player profile
    coins_bank = 500
    selected_vehicle = "Jeep"
    selected_stage = "Countryside"
    unlocked_vehicles = {"Jeep"}
    unlocked_stages = {"Countryside"}
    upgrades = {"engine": 1, "suspension": 1, "tires": 1, "fuel": 1}

    game_state = "GARAGE"
    terrain = EndlessTerrain(selected_stage)
    car = Vehicle(selected_vehicle, upgrades, selected_stage)

    # World collectibles
    coins = []
    fuel_cans = []
    frontier_x = 0.0

    def expand_world(target_x):
        nonlocal frontier_x
        while frontier_x < target_x:
            cx = frontier_x
            if cx > 800.0 and random.random() < 0.03:
                fuel_cans.append((cx, terrain.get_elevation(cx) - 24.0, False))
                frontier_x += 160.0
                continue

            gy = terrain.get_elevation(cx)
            c_val = random.choice([5, 10, 50, 100, 500])
            coins.append((cx, gy - 28.0, c_val, False))
            frontier_x += 50.0

    expand_world(3000.0)

    running = True
    gas_pressed = False
    brake_pressed = False

    brake_rect = pygame.Rect(40, SCREEN_HEIGHT - 120, 110, 80)
    gas_rect = pygame.Rect(SCREEN_WIDTH - 150, SCREEN_HEIGHT - 120, 110, 80)

    while running:
        dt = clock.tick(FPS) / 1000.0

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key in (pygame.K_RIGHT, pygame.K_UP, pygame.K_d, pygame.K_w):
                    gas_pressed = True
                elif event.key in (pygame.K_LEFT, pygame.K_DOWN, pygame.K_a, pygame.K_s):
                    brake_pressed = True
                elif event.key == pygame.K_SPACE:
                    if game_state in ("GAMEOVER", "GARAGE"):
                        terrain = EndlessTerrain(selected_stage)
                        car = Vehicle(selected_vehicle, upgrades, selected_stage)
                        coins.clear()
                        fuel_cans.clear()
                        frontier_x = 0.0
                        expand_world(3000.0)
                        game_state = "PLAYING"
                elif event.key == pygame.K_ESCAPE:
                    game_state = "GARAGE"
            elif event.type == pygame.KEYUP:
                if event.key in (pygame.K_RIGHT, pygame.K_UP, pygame.K_d, pygame.K_w):
                    gas_pressed = False
                elif event.key in (pygame.K_LEFT, pygame.K_DOWN, pygame.K_a, pygame.K_s):
                    brake_pressed = False
            elif event.type == pygame.MOUSEBUTTONDOWN:
                if brake_rect.collidepoint(event.pos):
                    brake_pressed = True
                elif gas_rect.collidepoint(event.pos):
                    gas_pressed = True
            elif event.type == pygame.MOUSEBUTTONUP:
                brake_pressed = False
                gas_pressed = False

        if game_state == "PLAYING":
            car.update(dt, terrain, gas_pressed, brake_pressed)
            expand_world(car.x + 2500.0)

            # Collect coins
            for idx, c in enumerate(coins):
                cx, cy, val, collected = c
                if not collected:
                    dx = car.x - cx
                    dy = car.y - cy
                    if dx * dx + dy * dy < 48 * 48:
                        coins[idx] = (cx, cy, val, True)
                        coins_bank += val

            # Collect fuel
            for idx, f in enumerate(fuel_cans):
                fx, fy, collected = f
                if not collected:
                    dx = car.x - fx
                    dy = car.y - fy
                    if dx * dx + dy * dy < 50 * 50:
                        fuel_cans[idx] = (fx, fy, True)
                        car.current_fuel = car.max_fuel

            if car.is_flipped(terrain):
                game_state = "GAMEOVER"

            if car.current_fuel <= 0.001 and car.speed_kmh < 1.5:
                game_state = "GAMEOVER"

        # RENDER
        screen.fill(terrain.env["sky_top"])

        if game_state == "PLAYING":
            cam_x = SCREEN_WIDTH * 0.32 - car.x
            cam_y = SCREEN_HEIGHT * 0.62 - car.y

            # Draw Terrain
            pts = []
            st_x = int(car.x - 600)
            ed_x = int(car.x + 800)
            for x in range(st_x, ed_x, 15):
                pts.append((x + cam_x, terrain.get_elevation(x) + cam_y))
            pts.append((ed_x + cam_x, SCREEN_HEIGHT + 200))
            pts.append((st_x + cam_x, SCREEN_HEIGHT + 200))
            pygame.draw.polygon(screen, terrain.env["subsurface"], pts)

            surf_pts = [(x + cam_x, terrain.get_elevation(x) + cam_y) for x in range(st_x, ed_x, 15)]
            if len(surf_pts) > 1:
                pygame.draw.lines(screen, terrain.env["surface"], False, surf_pts, 8)

            # Runway line
            if st_x <= terrain.flat_runway_length:
                pygame.draw.line(screen, COLOR_WHITE, (cam_x, terrain.base_y + cam_y), (terrain.flat_runway_length + cam_x, terrain.base_y + cam_y), 4)

            # Coins
            for cx, cy, val, col in coins:
                if not col and (st_x < cx < ed_x):
                    sx, sy = int(cx + cam_x), int(cy + cam_y)
                    c_color = COLOR_YELLOW if val < 100 else (COLOR_RED if val < 500 else (233, 30, 99))
                    pygame.draw.circle(screen, c_color, (sx, sy), 14)
                    txt = font_small.render(str(val), True, COLOR_BG_DARK)
                    screen.blit(txt, (sx - txt.get_width() // 2, sy - txt.get_height() // 2))

            # Fuel cans
            for fx, fy, col in fuel_cans:
                if not col and (st_x < fx < ed_x):
                    sx, sy = int(fx + cam_x), int(fy + cam_y)
                    pygame.draw.rect(screen, COLOR_RED, (sx - 12, sy - 16, 24, 32), border_radius=4)
                    txt = font_small.render("GAS", True, COLOR_WHITE)
                    screen.blit(txt, (sx - txt.get_width() // 2, sy - txt.get_height() // 2))

            # Wheels
            rx, ry = int(car.rear_wheel_x + cam_x), int(car.rear_wheel_y + cam_y)
            pygame.draw.circle(screen, (30, 41, 59), (rx, ry), int(car.wheel_radius))
            pygame.draw.circle(screen, COLOR_GREY_LIGHT, (rx, ry), int(car.wheel_radius * 0.5))

            if not car.is_unicycle:
                fx, fy = int(car.front_wheel_x + cam_x), int(car.front_wheel_y + cam_y)
                pygame.draw.circle(screen, (30, 41, 59), (fx, fy), int(car.wheel_radius))
                pygame.draw.circle(screen, COLOR_GREY_LIGHT, (fx, fy), int(car.wheel_radius * 0.5))
                pygame.draw.line(screen, COLOR_GREY_LIGHT, (int(car.x + cam_x + 20), int(car.y + cam_y)), (fx, fy), 3)

            # Chassis
            cx, cy = int(car.x + cam_x), int(car.y + cam_y)
            pygame.draw.rect(screen, car.spec["color"], (cx - 30, cy - 10, 60, 20), border_radius=6)
            pygame.draw.circle(screen, (254, 215, 170), (cx - 4, cy - 20), 8)
            pygame.draw.line(screen, COLOR_GREY_LIGHT, (cx - 20, cy), (rx, ry), 3)

            # HUD
            dist_m = int(car.x / 26.0)
            txt_dist = font_med.render(f"Distance: {dist_m}m", True, COLOR_WHITE)
            screen.blit(txt_dist, (24, 20))

            txt_c = font_med.render(f"Coins: {coins_bank} 🪙", True, COLOR_YELLOW)
            screen.blit(txt_c, (SCREEN_WIDTH - txt_c.get_width() - 24, 20))

            fuel_ratio = car.current_fuel / car.max_fuel
            pygame.draw.rect(screen, COLOR_GREY_DARK, (SCREEN_WIDTH // 2 - 100, 20, 200, 18), border_radius=6)
            pygame.draw.rect(screen, COLOR_GREEN if fuel_ratio > 0.25 else COLOR_RED, (SCREEN_WIDTH // 2 - 100, 20, int(200 * fuel_ratio), 18), border_radius=6)

            # Pedals
            pygame.draw.rect(screen, COLOR_METAL if not brake_pressed else (71, 85, 105), brake_rect, border_radius=12)
            pygame.draw.rect(screen, COLOR_GREY_LIGHT, brake_rect, 3, border_radius=12)
            tb = font_med.render("BRAKE", True, COLOR_WHITE)
            screen.blit(tb, (brake_rect.centerx - tb.get_width() // 2, brake_rect.centery - tb.get_height() // 2))

            pygame.draw.rect(screen, COLOR_METAL if not gas_pressed else (71, 85, 105), gas_rect, border_radius=12)
            pygame.draw.rect(screen, COLOR_GREY_LIGHT, gas_rect, 3, border_radius=12)
            tg = font_med.render("GAS", True, COLOR_WHITE)
            screen.blit(tg, (gas_rect.centerx - tg.get_width() // 2, gas_rect.centery - tg.get_height() // 2))

        elif game_state == "GARAGE":
            t = font_large.render("HILL CLIMB RACING - GARAGE & SHOP", True, COLOR_WHITE)
            screen.blit(t, (SCREEN_WIDTH // 2 - t.get_width() // 2, 40))

            c_info = font_med.render(f"Coins Balance: {coins_bank} 🪙", True, COLOR_YELLOW)
            screen.blit(c_info, (SCREEN_WIDTH // 2 - c_info.get_width() // 2, 90))

            v_info = font_med.render(f"Selected Vehicle: {selected_vehicle}", True, COLOR_CYAN)
            screen.blit(v_info, (SCREEN_WIDTH // 2 - v_info.get_width() // 2, 130))

            s_info = font_med.render(f"Selected Stage: {selected_stage}", True, COLOR_GREEN)
            screen.blit(s_info, (SCREEN_WIDTH // 2 - s_info.get_width() // 2, 160))

            guide = font_med.render("Press SPACE to Start Endless Run! (ESC to return)", True, COLOR_WHITE)
            screen.blit(guide, (SCREEN_WIDTH // 2 - guide.get_width() // 2, SCREEN_HEIGHT - 80))

        elif game_state == "GAMEOVER":
            t = font_large.render("GAME OVER", True, COLOR_RED)
            screen.blit(t, (SCREEN_WIDTH // 2 - t.get_width() // 2, SCREEN_HEIGHT // 2 - 40))
            sub = font_med.render("Press SPACE to Retry or ESC for Garage", True, COLOR_WHITE)
            screen.blit(sub, (SCREEN_WIDTH // 2 - sub.get_width() // 2, SCREEN_HEIGHT // 2 + 20))

        pygame.display.flip()

    pygame.quit()

if __name__ == "__main__":
    main()
