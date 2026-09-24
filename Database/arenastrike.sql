-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 24, 2026 at 08:43 PM
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
(4, '4', 'refactor stats and history', 'SQL', 'V4__refactor_stats_and_history.sql', 444847430, 'root', '2026-09-11 13:28:35', 106, 1),
(5, '5', 'Optimize Schema', 'SQL', 'V5__Optimize_Schema.sql', -239289735, 'root', '2026-09-23 15:14:54', 8, 1),
(6, '6', 'add headshot kills', 'SQL', 'V6__add_headshot_kills.sql', 1656856051, 'root', '2026-09-24 12:09:39', 38, 0);

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
(16, 'S5SP37', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-12 02:50:40.000000', 'my'),
(17, 'X5NTT8', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-15 04:15:33.000000', 'Arena Match'),
(18, 'CQA6GV', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-23 09:19:25.000000', 'TestRoom'),
(19, '37FK3M', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-23 09:28:08.000000', 'n'),
(20, '38DWEM', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-23 09:45:35.000000', 'Arena Match'),
(21, 'QZUVQR', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 05:20:08.000000', 'Arena Match'),
(22, 'JGJZ53', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 06:05:23.000000', 'Arena Match'),
(23, 'MC48JT', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 06:13:03.000000', 'Arena Match'),
(24, 'BUFWUR', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 06:28:50.000000', 'Arena Match'),
(25, 'AHFMCH', 'MAP_WAREHOUSE', 4, 'WAITING', '2026-09-24 06:29:44.000000', 'Arena Match'),
(26, 'TFHPHN', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 06:38:55.000000', 'Arena Match'),
(27, 'FXGP93', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 06:40:49.000000', 'Arena Match'),
(28, 'A7E9PT', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 07:35:43.000000', 'Arena Match'),
(29, 'JTM3CN', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 07:40:31.000000', 'Arena Match'),
(30, 'AHEJBS', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 07:56:01.000000', 'Arena Match'),
(31, '2AQZGS', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 07:57:24.000000', 'Arena Match'),
(32, 'NDQ2JG', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:02:52.000000', 'Arena Match'),
(33, 'U4EGAY', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:06:59.000000', 'Arena Match'),
(34, '7K5MVA', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:11:45.000000', 'Arena Match'),
(35, 'XZ6U7T', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:18:47.000000', 'Arena Match'),
(36, '26Z5MT', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:23:27.000000', 'Arena Match'),
(37, 'TW72HN', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:26:32.000000', 'Arena Match'),
(38, 'SL3SGD', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:28:46.000000', 'Arena Match'),
(39, 'Y2K65D', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:31:12.000000', 'Arena Match'),
(40, 'FUFVTN', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:37:24.000000', 'Arena Match'),
(41, 'GBP7NP', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 08:50:23.000000', 'Arena Match'),
(42, '952MPD', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:01:52.000000', 'Arena Match'),
(43, 'GRZHE7', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:06:13.000000', 'Arena Match'),
(44, '4UNV6F', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:10:43.000000', 'Arena Match'),
(45, 'C9U9D8', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:15:12.000000', 'Arena Match'),
(46, '99U4Q7', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:19:16.000000', 'Arena Match'),
(47, 'GNJUXR', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:31:24.000000', 'Arena Match'),
(48, '9FHD7U', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:32:09.000000', 'Arena Match'),
(49, '8MNKMM', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:34:35.000000', 'Arena Match'),
(50, 'KE4ZYU', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 09:46:54.000000', 'Arena Match'),
(51, 'TGTXE4', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 10:19:45.000000', 'Arena Match'),
(52, 'E2SWZE', 'MAP_BUNKER', 2, 'WAITING', '2026-09-24 10:23:15.000000', 'Arena Match'),
(53, 'VV2WCP', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 10:23:44.000000', 'Arena Match'),
(54, '5FPAXA', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 10:31:57.000000', 'Arena Match'),
(55, 'BADSAR', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:00:26.000000', 'Arena Match'),
(56, '6ARDJ5', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:01:34.000000', 'Arena Match'),
(57, 'YEWB5L', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:02:17.000000', 'Arena Match'),
(58, '6B6AAL', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:22:22.000000', 'Arena Match'),
(59, 'A276YZ', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:45:55.000000', 'Arena Match'),
(60, 'W9B9WM', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 11:51:15.000000', 'Arena Match'),
(61, 'SWLLA4', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:01:36.000000', 'Arena Match'),
(62, 'MP7QLY', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:17:21.000000', 'Arena Match'),
(63, 'ZPMWQM', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:24:24.000000', 'Arena Match'),
(64, 'EN32XP', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:29:38.000000', 'Arena Match'),
(65, '9FAK7H', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:38:59.000000', 'Arena Match'),
(66, 'Z5YAB2', 'MAP_WAREHOUSE', 2, 'WAITING', '2026-09-24 12:40:39.000000', 'Arena Match');

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
(34, 24, 39, '2026-09-24 06:28:50.000000', 0),
(35, 25, 41, '2026-09-24 06:29:44.000000', 0),
(36, 26, 43, '2026-09-24 06:38:55.000000', 0),
(37, 26, 45, '2026-09-24 06:39:18.000000', 0),
(38, 27, 47, '2026-09-24 06:40:49.000000', 0),
(39, 27, 49, '2026-09-24 06:41:03.000000', 0),
(40, 28, 50, '2026-09-24 07:35:43.000000', 0),
(41, 29, 54, '2026-09-24 07:40:31.000000', 0),
(42, 29, 55, '2026-09-24 07:40:47.000000', 0),
(43, 30, 56, '2026-09-24 07:56:01.000000', 0),
(44, 30, 57, '2026-09-24 07:56:12.000000', 0),
(45, 31, 58, '2026-09-24 07:57:24.000000', 0),
(46, 31, 59, '2026-09-24 07:57:39.000000', 0),
(47, 32, 60, '2026-09-24 08:02:52.000000', 0),
(48, 33, 61, '2026-09-24 08:06:59.000000', 0),
(49, 33, 62, '2026-09-24 08:07:15.000000', 0),
(50, 34, 63, '2026-09-24 08:11:45.000000', 0),
(51, 34, 64, '2026-09-24 08:12:00.000000', 0),
(52, 35, 65, '2026-09-24 08:18:47.000000', 0),
(53, 35, 66, '2026-09-24 08:18:59.000000', 0),
(54, 36, 67, '2026-09-24 08:23:27.000000', 0),
(55, 36, 68, '2026-09-24 08:23:39.000000', 0),
(56, 37, 69, '2026-09-24 08:26:32.000000', 0),
(57, 37, 70, '2026-09-24 08:26:45.000000', 0),
(58, 38, 71, '2026-09-24 08:28:46.000000', 0),
(59, 38, 72, '2026-09-24 08:29:06.000000', 0),
(60, 39, 73, '2026-09-24 08:31:12.000000', 0),
(61, 39, 74, '2026-09-24 08:31:38.000000', 0),
(62, 40, 75, '2026-09-24 08:37:24.000000', 0),
(63, 41, 76, '2026-09-24 08:50:23.000000', 0),
(64, 42, 77, '2026-09-24 09:01:52.000000', 0),
(65, 42, 78, '2026-09-24 09:02:09.000000', 0),
(66, 43, 79, '2026-09-24 09:06:13.000000', 0),
(67, 44, 82, '2026-09-24 09:10:43.000000', 0),
(68, 44, 83, '2026-09-24 09:10:59.000000', 0),
(69, 45, 84, '2026-09-24 09:15:12.000000', 0),
(70, 45, 85, '2026-09-24 09:16:21.000000', 0),
(71, 46, 86, '2026-09-24 09:19:16.000000', 0),
(72, 46, 87, '2026-09-24 09:19:37.000000', 0),
(73, 47, 88, '2026-09-24 09:31:24.000000', 0),
(74, 47, 89, '2026-09-24 09:31:39.000000', 0),
(75, 48, 90, '2026-09-24 09:32:09.000000', 0),
(76, 48, 91, '2026-09-24 09:32:27.000000', 0),
(77, 49, 92, '2026-09-24 09:34:35.000000', 0),
(78, 49, 93, '2026-09-24 09:34:59.000000', 0),
(79, 50, 94, '2026-09-24 09:46:54.000000', 0),
(80, 50, 95, '2026-09-24 09:47:21.000000', 0),
(81, 51, 96, '2026-09-24 10:19:45.000000', 0),
(82, 51, 97, '2026-09-24 10:19:56.000000', 0),
(83, 52, 98, '2026-09-24 10:23:15.000000', 0),
(84, 52, 99, '2026-09-24 10:23:30.000000', 0),
(85, 53, 100, '2026-09-24 10:23:44.000000', 0),
(86, 53, 101, '2026-09-24 10:23:57.000000', 0),
(87, 54, 102, '2026-09-24 10:31:57.000000', 0),
(88, 54, 105, '2026-09-24 10:32:29.000000', 0),
(89, 55, 106, '2026-09-24 11:00:26.000000', 0),
(90, 55, 107, '2026-09-24 11:00:39.000000', 0),
(91, 56, 108, '2026-09-24 11:01:34.000000', 0),
(92, 56, 109, '2026-09-24 11:01:54.000000', 0),
(93, 57, 110, '2026-09-24 11:02:17.000000', 0),
(94, 57, 111, '2026-09-24 11:02:29.000000', 0),
(95, 58, 112, '2026-09-24 11:22:22.000000', 0),
(96, 58, 113, '2026-09-24 11:22:31.000000', 0),
(97, 59, 114, '2026-09-24 11:45:55.000000', 0),
(98, 59, 115, '2026-09-24 11:46:08.000000', 0),
(99, 60, 116, '2026-09-24 11:51:15.000000', 0),
(100, 60, 117, '2026-09-24 11:51:24.000000', 0),
(101, 61, 118, '2026-09-24 12:01:36.000000', 0),
(102, 61, 119, '2026-09-24 12:02:11.000000', 0),
(103, 62, 120, '2026-09-24 12:17:21.000000', 0),
(104, 62, 121, '2026-09-24 12:17:30.000000', 0),
(105, 63, 122, '2026-09-24 12:24:24.000000', 0),
(106, 63, 123, '2026-09-24 12:24:41.000000', 0),
(107, 64, 124, '2026-09-24 12:29:38.000000', 0),
(108, 65, 125, '2026-09-24 12:38:59.000000', 0),
(109, 66, 126, '2026-09-24 12:40:39.000000', 0);

-- --------------------------------------------------------

--
-- Table structure for table `match_history`
--

CREATE TABLE `match_history` (
  `id` bigint(20) NOT NULL,
  `room_id` varchar(6) NOT NULL,
  `map_name` varchar(32) NOT NULL,
  `duration` int(11) NOT NULL,
  `played_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6),
  `winner_player_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `match_history`
--

INSERT INTO `match_history` (`id`, `room_id`, `map_name`, `duration`, `played_at`, `winner_player_id`) VALUES
(1, 'W9B9WM', 'MAP_WAREHOUSE', 300, '2026-09-24 11:56:26.000000', 116),
(2, 'SWLLA4', 'MAP_WAREHOUSE', 300, '2026-09-24 12:07:13.000000', 118),
(3, 'ZPMWQM', 'MAP_WAREHOUSE', 300, '2026-09-24 12:29:27.000000', 122);

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
(38, 'm', '2026-09-24 06:28:49.000000', '', 0, 0, 0, 0),
(39, 'm', '2026-09-24 06:28:49.000000', '', 0, 0, 0, 0),
(40, 'n', '2026-09-24 06:29:44.000000', '', 0, 0, 0, 0),
(41, 'n', '2026-09-24 06:29:44.000000', '', 0, 0, 0, 0),
(42, 'm', '2026-09-24 06:38:55.000000', '', 0, 0, 0, 0),
(43, 'm', '2026-09-24 06:38:55.000000', '', 0, 0, 0, 0),
(44, 'p', '2026-09-24 06:39:18.000000', '', 0, 0, 0, 0),
(45, 'p', '2026-09-24 06:39:18.000000', '', 0, 0, 0, 0),
(46, 'i', '2026-09-24 06:40:49.000000', '', 0, 0, 0, 0),
(47, 'i', '2026-09-24 06:40:49.000000', '', 0, 0, 0, 0),
(48, 'u', '2026-09-24 06:41:03.000000', '', 0, 0, 0, 0),
(49, 'u', '2026-09-24 06:41:03.000000', '', 0, 0, 0, 0),
(50, 'n', '2026-09-24 07:35:42.000000', '', 0, 0, 0, 0),
(51, 'i', '2026-09-24 07:36:09.000000', '', 0, 0, 0, 0),
(52, 'i', '2026-09-24 07:36:12.000000', '', 0, 0, 0, 0),
(53, 'v', '2026-09-24 07:36:40.000000', '', 0, 0, 0, 0),
(54, 'c', '2026-09-24 07:40:30.000000', '', 0, 0, 0, 0),
(55, 'z', '2026-09-24 07:40:46.000000', '', 0, 0, 0, 0),
(56, 'o', '2026-09-24 07:56:01.000000', '', 0, 0, 0, 0),
(57, 'b', '2026-09-24 07:56:12.000000', '', 0, 0, 0, 0),
(58, 't', '2026-09-24 07:57:24.000000', '', 0, 0, 0, 0),
(59, 'v', '2026-09-24 07:57:39.000000', '', 0, 0, 0, 0),
(60, 'c', '2026-09-24 08:02:51.000000', '', 0, 0, 0, 0),
(61, 'n', '2026-09-24 08:06:58.000000', '', 0, 0, 0, 0),
(62, 'o', '2026-09-24 08:07:15.000000', '', 0, 0, 0, 0),
(63, 'jn', '2026-09-24 08:11:45.000000', '', 0, 0, 0, 0),
(64, 'km', '2026-09-24 08:12:00.000000', '', 0, 0, 0, 0),
(65, 'ij', '2026-09-24 08:18:47.000000', '', 0, 0, 0, 0),
(66, 'km', '2026-09-24 08:18:59.000000', '', 0, 0, 0, 0),
(67, 'qqqq', '2026-09-24 08:23:27.000000', '', 0, 0, 0, 0),
(68, 'mmm', '2026-09-24 08:23:39.000000', '', 0, 0, 0, 0),
(69, 'nnnn', '2026-09-24 08:26:32.000000', '', 0, 0, 0, 0),
(70, 'njkk', '2026-09-24 08:26:45.000000', '', 0, 0, 0, 0),
(71, 'kkk', '2026-09-24 08:28:45.000000', '', 0, 0, 0, 0),
(72, 'mkmkmk', '2026-09-24 08:29:06.000000', '', 0, 0, 0, 0),
(73, 'njknkj', '2026-09-24 08:31:12.000000', '', 0, 0, 0, 0),
(74, 'nkjjn', '2026-09-24 08:31:38.000000', '', 0, 0, 0, 0),
(75, 'mmmmmm', '2026-09-24 08:37:24.000000', '', 0, 0, 0, 0),
(76, 'vvvv', '2026-09-24 08:50:23.000000', '', 0, 0, 0, 0),
(77, 'mmmm', '2026-09-24 09:01:52.000000', '', 0, 0, 0, 0),
(78, 'vvvv', '2026-09-24 09:02:08.000000', '', 0, 0, 0, 0),
(79, 'mmmm', '2026-09-24 09:06:13.000000', '', 0, 0, 0, 0),
(80, 'nnn', '2026-09-24 09:06:27.000000', '', 0, 0, 0, 0),
(81, 'nnn', '2026-09-24 09:06:29.000000', '', 0, 0, 0, 0),
(82, 'bhjbbj', '2026-09-24 09:10:43.000000', '', 0, 0, 0, 0),
(83, 'bibbio', '2026-09-24 09:10:59.000000', '', 0, 0, 0, 0),
(84, 'nhjtyh', '2026-09-24 09:15:12.000000', '', 0, 0, 0, 0),
(85, 'jnknu', '2026-09-24 09:16:21.000000', '', 0, 0, 0, 0),
(86, 'nkjnj', '2026-09-24 09:19:16.000000', '', 0, 0, 0, 0),
(87, 'niounoimo', '2026-09-24 09:19:37.000000', '', 0, 0, 0, 0),
(88, 'mkmlk', '2026-09-24 09:31:24.000000', '', 0, 0, 0, 0),
(89, 'nknkj', '2026-09-24 09:31:39.000000', '', 0, 0, 0, 0),
(90, 'nkjnkj', '2026-09-24 09:32:09.000000', '', 0, 0, 0, 0),
(91, 'lnjknjk', '2026-09-24 09:32:27.000000', '', 0, 0, 0, 0),
(92, 'njnkj', '2026-09-24 09:34:34.000000', '', 0, 0, 0, 0),
(93, 'nkjnjk', '2026-09-24 09:34:58.000000', '', 0, 0, 0, 0),
(94, 'MFKLML', '2026-09-24 09:46:53.000000', '', 0, 0, 0, 0),
(95, 'MIOMOI', '2026-09-24 09:47:21.000000', '', 0, 0, 0, 0),
(96, 'mkk', '2026-09-24 10:19:45.000000', '', 0, 0, 0, 0),
(97, 'njknkl', '2026-09-24 10:19:56.000000', '', 0, 0, 0, 0),
(98, 'mkmk', '2026-09-24 10:23:15.000000', '', 0, 0, 0, 0),
(99, 'kmkl', '2026-09-24 10:23:30.000000', '', 0, 0, 0, 0),
(100, 'mklk', '2026-09-24 10:23:44.000000', '', 0, 0, 0, 0),
(101, 'mkkll', '2026-09-24 10:23:57.000000', '', 0, 0, 0, 0),
(102, 'njknjkk', '2026-09-24 10:31:57.000000', '', 0, 0, 0, 0),
(103, 'njknkjk', '2026-09-24 10:32:13.000000', '', 0, 0, 0, 0),
(104, 'njknkjk', '2026-09-24 10:32:16.000000', '', 0, 0, 0, 0),
(105, 'njknkjk', '2026-09-24 10:32:29.000000', '', 0, 0, 0, 0),
(106, 'nkjnkj', '2026-09-24 11:00:26.000000', '', 0, 0, 0, 0),
(107, 'mlkmlk', '2026-09-24 11:00:39.000000', '', 0, 0, 0, 0),
(108, 'mkmkj', '2026-09-24 11:01:34.000000', '', 0, 0, 0, 0),
(109, 'mkm', '2026-09-24 11:01:54.000000', '', 0, 0, 0, 0),
(110, 'MKMK', '2026-09-24 11:02:17.000000', '', 0, 0, 0, 0),
(111, 'MKMM', '2026-09-24 11:02:29.000000', '', 0, 0, 0, 0),
(112, 'jknjk', '2026-09-24 11:22:21.000000', '', 0, 0, 0, 0),
(113, 'mklkm', '2026-09-24 11:22:31.000000', '', 0, 0, 0, 0),
(114, 'kjjnjkn', '2026-09-24 11:45:54.000000', '', 0, 0, 0, 0),
(115, 'kmklml', '2026-09-24 11:46:08.000000', '', 0, 0, 0, 0),
(116, 'mlkmlk', '2026-09-24 17:56:26.176554', '', 4, 0, 1, 1),
(117, 'mlkmlkm', '2026-09-24 17:56:26.183672', '', 0, 4, 1, 0),
(118, 'kjnkj', '2026-09-24 18:07:13.427659', '', 4, 0, 1, 1),
(119, 'mlkmlkm', '2026-09-24 18:07:13.429932', '', 0, 4, 1, 0),
(120, 'kjnkj', '2026-09-24 12:17:21.000000', '', 0, 0, 0, 0),
(121, 'nkjk', '2026-09-24 12:17:30.000000', '', 0, 0, 0, 0),
(122, 'm', '2026-09-24 18:29:27.642513', '', 8, 0, 1, 1),
(123, 'njknkj', '2026-09-24 12:24:41.000000', '', 0, 0, 0, 0),
(124, 'm', '2026-09-24 12:29:38.000000', '', 0, 0, 0, 0),
(125, 'mlk', '2026-09-24 12:38:58.000000', '', 0, 0, 0, 0),
(126, 'nnkj', '2026-09-24 12:40:38.000000', '', 0, 0, 0, 0);

-- --------------------------------------------------------

--
-- Table structure for table `player_match_stats`
--

CREATE TABLE `player_match_stats` (
  `id` bigint(20) NOT NULL,
  `match_id` bigint(20) NOT NULL,
  `player_id` bigint(20) NOT NULL,
  `kills` int(11) NOT NULL,
  `headshot_kills` int(11) NOT NULL DEFAULT 0,
  `score` int(11) NOT NULL,
  `weapon_used` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `player_match_stats`
--

INSERT INTO `player_match_stats` (`id`, `match_id`, `player_id`, `kills`, `headshot_kills`, `score`, `weapon_used`) VALUES
(1, 1, 116, 4, 4, 540, 'ASSAULT_RIFLE'),
(2, 1, 117, 0, 0, 0, 'ASSAULT_RIFLE'),
(3, 2, 118, 4, 3, 518, 'ASSAULT_RIFLE'),
(4, 2, 119, 0, 0, 0, 'ASSAULT_RIFLE'),
(5, 3, 122, 8, 3, 956, 'ASSAULT_RIFLE');

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
  ADD KEY `fk_match_history_winner` (`winner_player_id`),
  ADD KEY `idx_mh_room` (`room_id`);

--
-- Indexes for table `players`
--
ALTER TABLE `players`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_players_leaderboard` (`wins`,`total_kills`);

--
-- Indexes for table `player_match_stats`
--
ALTER TABLE `player_match_stats`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_player_match_stats_match` (`match_id`),
  ADD KEY `idx_pms_player` (`player_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `lobbies`
--
ALTER TABLE `lobbies`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=67;

--
-- AUTO_INCREMENT for table `lobby_participants`
--
ALTER TABLE `lobby_participants`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=110;

--
-- AUTO_INCREMENT for table `match_history`
--
ALTER TABLE `match_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `players`
--
ALTER TABLE `players`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=127;

--
-- AUTO_INCREMENT for table `player_match_stats`
--
ALTER TABLE `player_match_stats`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

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
