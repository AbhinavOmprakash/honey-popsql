\set dbname honey_popsql 

CREATE DATABASE :dbname;

-- user with full access to do everything in kapital database
-- create drop tables & schemas and so on
CREATE USER popsicle;

\c honey_popsql popsicle;

begin;
create table popsicles
	(flavor text,
	 cost int);

insert into popsicles 
	values 
		('Vanilla', 10),
		('Chocolate', 13),
		('Rainbow', 7),
		('Honey', 100);

create table honey 
(honey_type text,
	cost int);

insert into honey 
	values 
		('Manuka honey', 100),
		('Sage honey', 100),
	  ('HoneySQL', 1000);

create table consumer
	(name text,
 	 age int,
	 miscellenous_data jsonb);

insert into consumer 
	values
		('Gandalf', 24000 , '{"favorite_quote": "A wizard is never late..."}'),
		('Yoda', 900, '{"favorite_quote": "Judge me by my size, do you?"}');

commit;
