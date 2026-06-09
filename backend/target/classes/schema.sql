-- =============================================
-- Healify 数据库表设计
-- =============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50),
    avatar_url  VARCHAR(255),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. 健康档案表
CREATE TABLE IF NOT EXISTS health_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    gender              VARCHAR(10),
    birth_date          DATE,
    height_cm           DECIMAL(5,1),
    current_weight_kg   DECIMAL(5,1),
    target_weight_kg    DECIMAL(5,1),
    activity_level      VARCHAR(20),  -- SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE
    diet_preference     VARCHAR(20),  -- OMNIVORE, VEGETARIAN, VEGAN, KETO
    allergies           TEXT,         -- JSON array of allergy strings
    daily_calorie_goal  INT,
    daily_protein_g     INT,
    exercise_frequency  INT,          -- days per week
    exercise_duration   INT,          -- minutes per session
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. 体重记录表
CREATE TABLE IF NOT EXISTS weight_records (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id),
    weight_kg   DECIMAL(5,1) NOT NULL,
    bmi         DECIMAL(4,1),
    note        TEXT,
    recorded_at DATE      NOT NULL DEFAULT CURRENT_DATE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_weight_records_user_date
    ON weight_records(user_id, recorded_at DESC);

-- 4. 饮食计划表
CREATE TABLE IF NOT EXISTS meal_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT    NOT NULL REFERENCES users(id),
    week_start_date DATE      NOT NULL,
    day_of_week     INT       NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    meal_type       VARCHAR(20) NOT NULL,  -- BREAKFAST, LUNCH, DINNER, SNACK
    food_name       VARCHAR(200) NOT NULL,
    portion_g       INT,
    calories        INT,
    protein_g       DECIMAL(5,1),
    carbs_g         DECIMAL(5,1),
    fat_g           DECIMAL(5,1),
    recipe          TEXT,
    generated_by_ai BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_meal_plans_user_week
    ON meal_plans(user_id, week_start_date);

-- 5. 运动计划表
CREATE TABLE IF NOT EXISTS exercise_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT    NOT NULL REFERENCES users(id),
    week_start_date DATE      NOT NULL,
    day_of_week     INT       NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    exercise_name   VARCHAR(200) NOT NULL,
    sets            INT,
    reps            INT,
    duration_min    INT,
    intensity       VARCHAR(20),  -- LOW, MEDIUM, HIGH
    notes           TEXT,
    generated_by_ai BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_exercise_plans_user_week
    ON exercise_plans(user_id, week_start_date);
