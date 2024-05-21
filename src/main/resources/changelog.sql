--liquibase formatted sql

--changeset emaysyuk:202405210001
CREATE TABLE `cars` (
  `id` int unsigned NOT NULL,
  `production_year` int unsigned NOT NULL,
  `version` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;