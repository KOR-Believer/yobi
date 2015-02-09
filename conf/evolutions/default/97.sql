# --- !Ups
create table SSH_STORE (
  ssh_key                    varchar(588) not null,
  user_id                    bigint,
  comment                    varchar(255),
  finger_print               varchar(255),
  register_date				 timestamp,
  last_connected_date        timestamp,
  constraint pk_SSH_STORE primary key (ssh_key))
;
create sequence SSH_STORE_seq;
alter table SSH_STORE add constraint fk_ssh_store_user_1 foreign key (user_id) references n4user (id) on delete restrict on update restrict;
create index ix_ssh_store_user_1 on SSH_STORE (user_id);

# --- !Downs
SET REFERENTIAL_INTEGRITY FALSE;
drop table if exists SSH_STORE;
SET REFERENTIAL_INTEGRITY TRUE;
drop sequence if exists SSH_STORE_seq;
