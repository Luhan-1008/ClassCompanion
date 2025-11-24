-- 北航课程伴侣 MySQL 数据库设计

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` INT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
  `student_id` VARCHAR(20) UNIQUE COMMENT '学号',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `email` VARCHAR(100) COMMENT '邮箱',
  `avatar_url` VARCHAR(255) COMMENT '头像URL',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_username` (`username`),
  INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 课程表
CREATE TABLE IF NOT EXISTS `courses` (
  `course_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '所属用户ID',
  `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(20) COMMENT '课程代码',
  `teacher_name` VARCHAR(50) COMMENT '任课教师',
  `location` VARCHAR(100) COMMENT '上课地点',
  `day_of_week` TINYINT NOT NULL COMMENT '星期几（1-7，1为周一）',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间',
  `start_week` TINYINT DEFAULT 1 COMMENT '起始周',
  `end_week` TINYINT DEFAULT 16 COMMENT '结束周',
  `reminder_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用提醒',
  `reminder_minutes` INT DEFAULT 15 COMMENT '提前提醒分钟数',
  `color` VARCHAR(7) DEFAULT '#2196F3' COMMENT '课程颜色',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_day_of_week` (`day_of_week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 作业/实验任务表
CREATE TABLE IF NOT EXISTS `assignments` (
  `assignment_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '所属用户ID',
  `course_id` INT COMMENT '所属课程ID',
  `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
  `description` TEXT COMMENT '任务描述',
  `type` ENUM('作业', '实验', '其他') DEFAULT '作业' COMMENT '任务类型',
  `due_date` DATETIME NOT NULL COMMENT '截止时间',
  `reminder_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用提醒',
  `reminder_time` DATETIME COMMENT '提醒时间',
  `status` ENUM('未开始', '进行中', '已完成', '已逾期') DEFAULT '未开始' COMMENT '完成状态',
  `priority` ENUM('低', '中', '高') DEFAULT '中' COMMENT '优先级',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `courses`(`course_id`) ON DELETE SET NULL,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_due_date` (`due_date`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业/实验任务表';

-- 学习小组表
CREATE TABLE IF NOT EXISTS `study_groups` (
  `group_id` INT PRIMARY KEY AUTO_INCREMENT,
  `creator_id` INT NOT NULL COMMENT '创建者ID',
  `group_name` VARCHAR(100) NOT NULL COMMENT '小组名称',
  `description` TEXT COMMENT '小组描述',
  `course_id` INT COMMENT '关联课程ID',
  `topic` VARCHAR(100) COMMENT '主题',
  `max_members` INT DEFAULT 20 COMMENT '最大成员数',
  `is_public` BOOLEAN DEFAULT TRUE COMMENT '是否公开',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`creator_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `courses`(`course_id`) ON DELETE SET NULL,
  INDEX `idx_creator_id` (`creator_id`),
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习小组表';

-- 小组成员表
CREATE TABLE IF NOT EXISTS `group_members` (
  `member_id` INT PRIMARY KEY AUTO_INCREMENT,
  `group_id` INT NOT NULL COMMENT '小组ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `role` ENUM('创建者', '管理员', '成员') DEFAULT '成员' COMMENT '角色',
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `status` ENUM('待审核', '已加入', '已退出') DEFAULT '已加入' COMMENT '状态',
  FOREIGN KEY (`group_id`) REFERENCES `study_groups`(`group_id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组成员表';

-- 小组消息表
CREATE TABLE IF NOT EXISTS `group_messages` (
  `message_id` INT PRIMARY KEY AUTO_INCREMENT,
  `group_id` INT NOT NULL COMMENT '小组ID',
  `user_id` INT NOT NULL COMMENT '发送者ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` ENUM('文本', '图片', '文件') DEFAULT '文本' COMMENT '消息类型',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`group_id`) REFERENCES `study_groups`(`group_id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组消息表';

-- 笔记表（选做功能）
CREATE TABLE IF NOT EXISTS `notes` (
  `note_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `course_id` INT COMMENT '关联课程ID',
  `title` VARCHAR(200) NOT NULL COMMENT '笔记标题',
  `content` TEXT COMMENT '笔记内容',
  `ai_summary` TEXT COMMENT 'AI生成的摘要',
  `file_type` ENUM('录音', '图片', '文本') COMMENT '文件类型',
  `file_url` VARCHAR(255) COMMENT '文件URL',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`course_id`) REFERENCES `courses`(`course_id`) ON DELETE SET NULL,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 作业辅导记录表（选做功能）
CREATE TABLE IF NOT EXISTS `assignment_help` (
  `help_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `assignment_id` INT COMMENT '关联作业ID',
  `question` TEXT NOT NULL COMMENT '问题内容',
  `ai_response` TEXT COMMENT 'AI回答',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`assignment_id`) REFERENCES `assignments`(`assignment_id`) ON DELETE SET NULL,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_assignment_id` (`assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业辅导记录表';

-- 学习分析记录表（选做功能）
CREATE TABLE IF NOT EXISTS `learning_analytics` (
  `analytics_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `report_date` DATE NOT NULL COMMENT '报告日期',
  `total_courses` INT DEFAULT 0 COMMENT '总课程数',
  `completed_assignments` INT DEFAULT 0 COMMENT '已完成作业数',
  `pending_assignments` INT DEFAULT 0 COMMENT '待完成作业数',
  `overdue_assignments` INT DEFAULT 0 COMMENT '逾期作业数',
  `group_activity_score` INT DEFAULT 0 COMMENT '小组活跃度分数',
  `suggestions` TEXT COMMENT '学习建议',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_user_date` (`user_id`, `report_date`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习分析记录表';

-- 通知提醒表
CREATE TABLE IF NOT EXISTS `notifications` (
  `notification_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `type` ENUM('课程提醒', '作业提醒', '小组消息', '系统通知') NOT NULL COMMENT '通知类型',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `related_id` INT COMMENT '关联ID（课程ID、作业ID等）',
  `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_is_read` (`is_read`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知提醒表';

