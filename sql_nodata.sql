CREATE DATABASE  IF NOT EXISTS `memory_game` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `memory_game`;
-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: memory_game
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `card`
--

DROP TABLE IF EXISTS `card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_path` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `theme_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmvsdre52f4g05lori1y1qkq25` (`theme_id`),
  CONSTRAINT `FKmvsdre52f4g05lori1y1qkq25` FOREIGN KEY (`theme_id`) REFERENCES `theme` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=94 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `friend`
--

DROP TABLE IF EXISTS `friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accepted_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING') NOT NULL,
  `receiver_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnbki3da2tox3pedxa9qs2gaqi` (`receiver_id`),
  KEY `FK52xr8lgynu3wfwu6fm2g4l5dj` (`sender_id`),
  CONSTRAINT `FK52xr8lgynu3wfwu6fm2g4l5dj` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnbki3da2tox3pedxa9qs2gaqi` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `game_match`
--

DROP TABLE IF EXISTS `game_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_match` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_size` varchar(255) DEFAULT NULL,
  `end_time` datetime(6) DEFAULT NULL,
  `player1_score` int DEFAULT NULL,
  `player2_score` int DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `status` enum('LOSE','PLAYING','WIN') DEFAULT NULL,
  `time_per_move` int DEFAULT NULL,
  `player1_id` bigint NOT NULL,
  `player2_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `theme_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlnxy3a8td544gixq7f1mh3lu7` (`player1_id`),
  KEY `FK2larsq43tyb2yotjvc7gpq8oe` (`player2_id`),
  KEY `FKnq8sbmkit127n9hgww88uyba3` (`room_id`),
  KEY `FK4f04xcriydb2c6cr6palrv2h1` (`theme_id`),
  CONSTRAINT `FK2larsq43tyb2yotjvc7gpq8oe` FOREIGN KEY (`player2_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK4f04xcriydb2c6cr6palrv2h1` FOREIGN KEY (`theme_id`) REFERENCES `theme` (`id`),
  CONSTRAINT `FKlnxy3a8td544gixq7f1mh3lu7` FOREIGN KEY (`player1_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnq8sbmkit127n9hgww88uyba3` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=84 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) DEFAULT NULL,
  `receiver_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `type_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3ely3fa0vfkswch305omy1c38` (`type_id`),
  CONSTRAINT `FK3ely3fa0vfkswch305omy1c38` FOREIGN KEY (`type_id`) REFERENCES `notification_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_type`
--

DROP TABLE IF EXISTS `notification_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` enum('ACCOUNT_CREATED','ACCOUNT_UPDATED','FRIEND_ONLINE','FRIEND_REQUEST_ACCEPTED','FRIEND_REQUEST_RECEIVED','MATCH_INVITE_DECLINED','MATCH_INVITE_RECEIVED','PASSWORD_CHANGED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKppuamykfseooqqskdqrr8iiy7` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `private_message`
--

DROP TABLE IF EXISTS `private_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `match_id` bigint DEFAULT NULL,
  `receiver_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `message_type` enum('TEXT','STICKER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT',
  `sticker_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsp4jk6p6ys7q4mr93fkod6qj9` (`match_id`),
  KEY `FKq4f43ef8dtn1m1eglg64mkp9x` (`receiver_id`),
  KEY `FKf6nbmipk0d9vln6rpcml7x883` (`sender_id`),
  KEY `FKtqddsmp7dacshs9otq8suxx3y` (`sticker_id`),
  CONSTRAINT `fk_private_message_sticker` FOREIGN KEY (`sticker_id`) REFERENCES `sticker` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `FKf6nbmipk0d9vln6rpcml7x883` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq4f43ef8dtn1m1eglg64mkp9x` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsp4jk6p6ys7q4mr93fkod6qj9` FOREIGN KEY (`match_id`) REFERENCES `game_match` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=593 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `guest_id` bigint DEFAULT NULL,
  `host_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf1o1ypmivmbpy3qa7ue7ji4rb` (`guest_id`),
  KEY `FKmrgc67nlcet3vmu97bieetay4` (`host_id`),
  CONSTRAINT `FKf1o1ypmivmbpy3qa7ue7ji4rb` FOREIGN KEY (`guest_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmrgc67nlcet3vmu97bieetay4` FOREIGN KEY (`host_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=294 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room_invite`
--

DROP TABLE IF EXISTS `room_invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_invite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING','REJECTED') NOT NULL,
  `receiver_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9xlvkp6uh0ar27udxnwwcepvo` (`receiver_id`),
  KEY `FKbj9s6dxsft5igdya571nui1gk` (`room_id`),
  KEY `FKs3bjxs8x3bxpttps2m7ldbo6x` (`sender_id`),
  CONSTRAINT `FK9xlvkp6uh0ar27udxnwwcepvo` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbj9s6dxsft5igdya571nui1gk` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `FKs3bjxs8x3bxpttps2m7ldbo6x` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=150 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sticker`
--

DROP TABLE IF EXISTS `sticker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sticker` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sticker_path` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `type` varchar(50) NOT NULL DEFAULT 'NORMAL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `theme`
--

DROP TABLE IF EXISTS `theme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `theme` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_path` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `south_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_setting`
--

DROP TABLE IF EXISTS `user_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_setting` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `language` varchar(255) DEFAULT NULL,
  `music_volume` int DEFAULT NULL,
  `notification_enabled` bit(1) DEFAULT NULL,
  `sound_fx_volume` int DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `status` enum('BUSY','OFFLINE','ONLINE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `score` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `world_message`
--

DROP TABLE IF EXISTS `world_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `world_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `sender_id` bigint NOT NULL,
  `message_type` enum('TEXT','STICKER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT',
  `sticker_id` bigint DEFAULT NULL,
  `sticker_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8k3jv7dwxv01ubulox4hqohn4` (`sender_id`),
  KEY `FK9xv86gvy8ktpna35uuici0aq5` (`sticker_id`),
  CONSTRAINT `FK8k3jv7dwxv01ubulox4hqohn4` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_world_message_sticker` FOREIGN KEY (`sticker_id`) REFERENCES `sticker` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1356 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-09 13:34:03
