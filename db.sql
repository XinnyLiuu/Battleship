-- Database setup
drop database if exists battleship;
create database battleship;
use battleship;

-- Users
create table users (
    id int not null auto_increment,
    username varchar(255) not null unique,
    password varchar(255) not null,
    session_key varchar(255),
    constraint pk_user primary key (id)
);


