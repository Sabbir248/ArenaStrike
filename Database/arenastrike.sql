-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 13, 2026 at 02:58 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `arenastrike`
--

-- --------------------------------------------------------

--
-- Table structure for table `flyway_schema_history`
--

CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `flyway_schema_history`
--

INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES
(1, '1', 'create core schema', 'SQL', 'V1__create_core_schema.sql', -2076651833, 'root', '2026-09-11 13:28:35', 68, 1),
(2, '2', 'add account stats and match lifecycle', 'SQL', 'V2__add_account_stats_and_match_lifecycle.sql', 253159105, 'root', '2026-09-11 13:28:35', 37, 1),
(3, '3', 'add lobby room name', 'SQL', 'V3__add_lobby_room_name.sql', -1891717646, 'root', '2026-09-11 13:28:35', 23, 1),
(4, '4', 'refactor stats and history', 'SQL', 'V4__refactor_stats_and_history.sql', 444847430, 'root', '2026-09-11 13:28:35', 106, 1);

-- --------------------------------------------------------

--
-- Table structure for table `lobbies`
--

CREATE TABLE `lobbies` (
  `id` bigint(20) NOT NULL,
  `room_code` varchar(6) NOT NULL,
  `map` varchar(16) NOT NULL,
  `player_limit` int(11) NOT NULL,
  `status` varchar(16) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  `room_name` varchar(64) NOT NULL DEFAULT 'Arena Lobby'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `lobbies`
--

INSERT INTO `lobbies` (`id`, `room_code`, `map`, `player_limit`, `status`, `created_at`, `room_name`) VALUES
(1, 'SWN5FA', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 07:57:08.000000', 'Arena Match'),
(2, 'B8J8ZY', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 08:18:18.000000', 'Test Room'),
(3, 'TC5S6D', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 10:15:44.000000', 'Arena Match'),
(4, '5SZFRP', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 10:17:01.000000', 'Arena Match'),
(5, '2R62XW', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 10:33:56.000000', 'my'),
(6, 'DH8RLQ', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 10:55:17.000000', 'Arena Match'),
(7, 'V7L3UX', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-11 10:55:44.000000', 'Arena Match'),
(8, 'PN9M49', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:34:28.000000', 'Arena Match'),
(9, 'D39L5V', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:34:48.000000', 'Arena Match'),
(10, 'XAZL6G', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:35:03.000000', 'Arena Match'),
(11, '4AN3S6', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:36:27.000000', 'Arena Match'),
(12, 'MKQM2N', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:38:01.000000', 'Arena Match'),
(13, 'XZ8VLU', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:47:40.000000', 'Arena Match'),
(14, '2QDWBZ', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:48:37.000000', 'Arena Match'),
(15, 'NCDCYE', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:49:10.000000', 'my'),
(16, 'S5SP37', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:50:40.000000', 'my');

-- --------------------------------------------------------

--
-- Table structure for table `lobby_participants`
--

CREATE TABLE `lobby_participants` (
  `id` bigint(20) NOT NULL,
  `lobby_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `joined_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  `is_ready` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `lobby_participants`
--

INSERT INTO `lobby_participants` (`id`, `lobby_id`, `user_id`, `joined_at`, `is_ready`) VALUES
(1, 1, 1, '2026-09-11 07:57:08.000000', 0),
(2, 2, 2, '2026-09-11 08:18:18.000000', 0),
(3, 2, 3, '2026-09-11 08:18:32.000000', 0),
(4, 3, 4, '2026-09-11 10:15:44.000000', 0),
(5, 4, 5, '2026-09-11 10:17:01.000000', 0),
(6, 3, 6, '2026-09-11 10:17:41.000000', 0),
(7, 5, 7, '2026-09-11 10:33:56.000000', 0),
(8, 6, 8, '2026-09-11 10:55:17.000000', 0),
(9, 7, 9, '2026-09-11 10:55:44.000000', 0),
(10, 7, 10, '2026-09-11 10:56:54.000000', 0),
(11, 8, 13, '2026-09-12 02:34:28.000000', 0),
(12, 9, 14, '2026-09-12 02:34:48.000000', 0),
(13, 10, 15, '2026-09-12 02:35:03.000000', 0),
(14, 11, 16, '2026-09-12 02:36:27.000000', 0),
(15, 10, 17, '2026-09-12 02:36:47.000000', 0),
(16, 12, 19, '2026-09-12 02:38:01.000000', 0),
(17, 12, 20, '2026-09-12 02:38:16.000000', 0),
(18, 13, 21, '2026-09-12 02:47:40.000000', 0),
(19, 14, 22, '2026-09-12 02:48:37.000000', 0),
(20, 15, 23, '2026-09-12 02:49:10.000000', 0),
(21, 15, 24, '2026-09-12 02:49:20.000000', 0),
(22, 16, 26, '2026-09-12 02:50:40.000000', 0);

-- --------------------------------------------------------

--
-- Table structure for table `match_history`
--

CREATE TABLE `match_history` (
  `id` bigint(20) NOT NULL,
  `room_id` varchar(6) NOT NULL,
  `map_name` varchar(32) NOT NULL,
  `duration` int(11) NOT NULL,
  `played_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  `winner_player_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `players`
--

CREATE TABLE `players` (
  `id` bigint(20) NOT NULL,
  `username` varchar(32) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  `password` varchar(255) NOT NULL DEFAULT '',
  `total_kills` bigint(20) NOT NULL DEFAULT 0,
  `total_deaths` bigint(20) NOT NULL DEFAULT 0,
  `matches_played` bigint(20) NOT NULL DEFAULT 0,
  `wins` bigint(20) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `players`
--

INSERT INTO `players` (`id`, `username`, `created_at`, `password`, `total_kills`, `total_deaths`, `matches_played`, `wins`) VALUES
(1, 'ffrg', '2026-09-11 07:57:08.000000', '', 0, 0, 0, 0),
(2, 'TestPlayer', '2026-09-11 08:18:18.000000', '', 0, 0, 0, 0),
(3, 'Player2', '2026-09-11 08:18:32.000000', '', 0, 0, 0, 0),
(4, 'm', '2026-09-11 10:15:44.000000', '', 0, 0, 0, 0),
(5, 'p', '2026-09-11 10:17:01.000000', '', 0, 0, 0, 0),
(6, 'i', '2026-09-11 10:17:41.000000', '', 0, 0, 0, 0),
(7, 'm-37bd2dba', '2026-09-11 10:33:56.000000', '', 0, 0, 0, 0),
(8, 'm-c95879ce', '2026-09-11 10:55:16.000000', '', 0, 0, 0, 0),
(9, 'm-28c1d109', '2026-09-11 10:55:44.000000', '', 0, 0, 0, 0),
(10, 'f', '2026-09-11 10:56:54.000000', '', 0, 0, 0, 0),
(13, 'q', '2026-09-12 02:34:28.000000', '', 0, 0, 0, 0),
(14, 'q-a8dd8ed3', '2026-09-12 02:34:48.000000', '', 0, 0, 0, 0),
(15, 'q-c1d49059', '2026-09-12 02:35:03.000000', '', 0, 0, 0, 0),
(16, 'm-a9c54ebd', '2026-09-12 02:36:27.000000', '', 0, 0, 0, 0),
(17, 'm-dd6fcb69', '2026-09-12 02:36:47.000000', '', 0, 0, 0, 0),
(19, 'o', '2026-09-12 02:38:01.000000', '', 0, 0, 0, 0),
(20, 'y', '2026-09-12 02:38:16.000000', '', 0, 0, 0, 0),
(21, 'g', '2026-09-12 02:47:40.000000', '', 0, 0, 0, 0),
(22, 'g-dca16722', '2026-09-12 02:48:37.000000', '', 0, 0, 0, 0),
(23, 'g-3406daf2', '2026-09-12 02:49:10.000000', '', 0, 0, 0, 0),
(24, 'h', '2026-09-12 02:49:20.000000', '', 0, 0, 0, 0),
(26, 'g-8e3bf9ee', '2026-09-12 02:50:40.000000', '', 0, 0, 0, 0);

-- --------------------------------------------------------

--
-- Table structure for table `player_match_stats`
--

CREATE TABLE `player_match_stats` (
  `id` bigint(20) NOT NULL,
  `match_id` bigint(20) NOT NULL,
  `player_id` bigint(20) NOT NULL,
  `kills` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  `weapon_used` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `flyway_schema_history`
--
ALTER TABLE `flyway_schema_history`
  ADD PRIMARY KEY (`installed_rank`),
  ADD KEY `flyway_schema_history_s_idx` (`success`);

--
-- Indexes for table `lobbies`
--
ALTER TABLE `lobbies`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_lobby_room_code` (`room_code`);

--
-- Indexes for table `lobby_participants`
--
ALTER TABLE `lobby_participants`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_lobby_user` (`lobby_id`,`user_id`),
  ADD KEY `idx_lobby_participants_user` (`user_id`);

--
-- Indexes for table `match_history`
--
ALTER TABLE `match_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_match_history_winner` (`winner_player_id`);

--
-- Indexes for table `players`
--
ALTER TABLE `players`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `player_match_stats`
--
ALTER TABLE `player_match_stats`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_player_match_stats_match` (`match_id`),
  ADD KEY `fk_player_match_stats_player` (`player_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `lobbies`
--
ALTER TABLE `lobbies`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `lobby_participants`
--
ALTER TABLE `lobby_participants`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `match_history`
--
ALTER TABLE `match_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `players`
--
ALTER TABLE `players`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT for table `player_match_stats`
--
ALTER TABLE `player_match_stats`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `lobby_participants`
--
ALTER TABLE `lobby_participants`
  ADD CONSTRAINT `fk_lobby_participants_lobby` FOREIGN KEY (`lobby_id`) REFERENCES `lobbies` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lobby_participants_player` FOREIGN KEY (`user_id`) REFERENCES `players` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `match_history`
--
ALTER TABLE `match_history`
  ADD CONSTRAINT `fk_match_history_winner` FOREIGN KEY (`winner_player_id`) REFERENCES `players` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `player_match_stats`
--
ALTER TABLE `player_match_stats`
  ADD CONSTRAINT `fk_player_match_stats_match` FOREIGN KEY (`match_id`) REFERENCES `match_history` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_player_match_stats_player` FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
