-- MariaDB structure definition script for schema "poker" (five-card draw)
-- best import using MariaDB client command "source <path to this file>"

SET CHARACTER SET utf8mb4;
DROP DATABASE IF EXISTS poker;
CREATE DATABASE poker CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE poker;

-- define tables, indices, etc.
CREATE TABLE BaseEntity (
	identity BIGINT NOT NULL AUTO_INCREMENT,
	discriminator ENUM("Document", "Person", "PokerTable", "Card", "Game", "Hand") NOT NULL,
	version INTEGER NOT NULL DEFAULT 1,
	creationTimestamp BIGINT NOT NULL,
	PRIMARY KEY (identity),
	KEY (discriminator)
);

CREATE TABLE Document (
	documentIdentity BIGINT NOT NULL,
	hash CHAR(64) NOT NULL,
	type VARCHAR(63) NOT NULL,
	content LONGBLOB NOT NULL,
	PRIMARY KEY (documentIdentity),
	FOREIGN KEY (documentIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	UNIQUE KEY (hash)
);

CREATE TABLE PokerTable (
	pokerTableIdentity BIGINT NOT NULL,
	avatarReference BIGINT NOT NULL,
	alias CHAR(32) NOT NULL,
	PRIMARY KEY (pokerTableIdentity),
	FOREIGN KEY (pokerTableIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (avatarReference) REFERENCES Document (documentIdentity) ON DELETE RESTRICT ON UPDATE CASCADE,
	UNIQUE KEY (alias)
);

CREATE TABLE Person (
	personIdentity BIGINT NOT NULL,
	avatarReference BIGINT NOT NULL,
	pokerTableReference BIGINT NULL,
	email CHAR(128) NOT NULL,
	passwordHash CHAR(64) NOT NULL,
	balance BIGINT NOT NULL,
	groupAlias ENUM("USER", "ADMIN") NOT NULL,
	position TINYINT NULL,
	title VARCHAR(15) NULL,
	surname VARCHAR(31) NOT NULL,
	forename VARCHAR(31) NOT NULL,
	street VARCHAR(63) NOT NULL,
	postcode VARCHAR(15) NOT NULL,
	city VARCHAR(63) NOT NULL,
	country VARCHAR(63) NOT NULL,
	negotiationOffer VARCHAR(2046) NULL,
	negotiationAnswer VARCHAR(2046) NULL,
	negotiationTimestamp BIGINT NULL,
	PRIMARY KEY (personIdentity),
	FOREIGN KEY (personIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (avatarReference) REFERENCES Document (documentIdentity) ON DELETE RESTRICT ON UPDATE CASCADE,
	FOREIGN KEY (pokerTableReference) REFERENCES PokerTable (pokerTableIdentity) ON DELETE RESTRICT ON UPDATE CASCADE,
	UNIQUE KEY (email),
	UNIQUE KEY (pokerTableReference, position)
);

CREATE TABLE Card (
	cardIdentity BIGINT NOT NULL,
	suitAlias ENUM("DIAMONDS", "HEARTS", "SPADES", "CLUBS") NOT NULL,
	rankAlias ENUM("ACE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "JACK", "QUEEN", "KING") NOT NULL,
	PRIMARY KEY (cardIdentity),
	FOREIGN KEY (cardIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	UNIQUE KEY (suitAlias, rankAlias)
);

CREATE TABLE Game (
	gameIdentity BIGINT NOT NULL,
	pokerTableReference BIGINT NOT NULL,
	stateAlias ENUM("DEAL", "DEAL_BET", "DRAW", "DRAW_BET", "SHOWDOWN") NOT NULL,
	activityTimestamp BIGINT NOT NULL,
	PRIMARY KEY (gameIdentity),
	FOREIGN KEY (gameIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (pokerTableReference) REFERENCES PokerTable (pokerTableIdentity) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Hand (
	handIdentity BIGINT NOT NULL,
	gameReference BIGINT NOT NULL,
	playerReference BIGINT NULL,
	bet BIGINT NOT NULL,
	active BOOL NOT NULL,
	folded BOOL NOT NULL,
	PRIMARY KEY (handIdentity),
	FOREIGN KEY (handIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (gameReference) REFERENCES Game (gameIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (playerReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE PersonPhoneAssociation (
	personReference BIGINT NOT NULL,
	phone CHAR(16) NOT NULL,
	PRIMARY KEY (personReference, phone),
	FOREIGN KEY (personReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE HandCardAssociation (
	handReference BIGINT NOT NULL,
	cardReference BIGINT NOT NULL,
	PRIMARY KEY (handReference, cardReference),
	FOREIGN KEY (handReference) REFERENCES Hand (handIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (cardReference) REFERENCES Card (cardIdentity) ON DELETE CASCADE ON UPDATE CASCADE
);
