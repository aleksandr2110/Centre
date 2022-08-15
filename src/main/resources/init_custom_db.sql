ALTER TABLE logtest CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE logtest DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;


create table supplier_app
(
    supplier_app_id  int auto_increment primary key,
    url              varchar(255) not null,
    name             varchar(255) not null,
    name_manufacture varchar(255) not null,
    markup           int          not null
);

create table order_process_app
(
    order_process_id int auto_increment primary key,
    supplier_id      int       not null,
    start_process    timestamp not null default current_timestamp,
    end_process      timestamp not null default current_timestamp,
    foreign key (supplier_id) references supplier_app (supplier_app_id)
);

create table product_app
(
    product_app_id   int auto_increment primary key,
    order_process_id int          not null,
    url              varchar(255) not null default '',
    name             varchar(255) not null default '',
    status           varchar(255) not null default '',
    old_price        decimal(15, 4),
    new_price        decimal(15, 4),
    foreign key (order_process_id) references order_process_app (order_process_id)
);


create table attribute_app
(
    attribute_id     int auto_increment primary key,
    supplier_id      int          not null,
    supplier_title   varchar(255) not null,
    opencart_title   varchar(255) not null,
    replacement_text varchar(255) not null,
    math_sign        varchar(255) not null,
    math_number      int          not null,
    foreign key (supplier_id) references supplier_app (supplier_app_id)
);

ALTER TABLE attribute_app CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE attribute_app DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;


create table manufacturer_app
(
    manufacturer_id int auto_increment primary key,
    supplier_id     int          not null,
    supplier_title  varchar(255) not null,
    opencart_title  varchar(255) not null,
    markup          int          not null,
    foreign key (supplier_id) references supplier_app (supplier_app_id)
);

ALTER TABLE manufacturer_app CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE manufacturer_app DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;


create table category_app
(
    category_id    int auto_increment primary key,
    supplier_id    int          not null,
    supplier_title varchar(255) not null,
    opencart_title varchar(255) not null,
    markup         int          not null,
    foreign key (supplier_id) references supplier_app (supplier_app_id)
);

ALTER TABLE category_app CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE category_app DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

create table product_profile_app
(
    product_profile_id int auto_increment primary key,
    url                varbinary(8000) not null,
    sku                varchar(255)    not null,
    title              varchar(255)    not null,
    supplier_id        int             not null,
    manufacturer_id    int             not null,
    category_id        int             not null,
    price              decimal(15, 4),
    foreign key (supplier_id) references supplier_app (supplier_app_id),
    foreign key (manufacturer_id) references manufacturer_app (manufacturer_id),
    foreign key (category_id) references category_app (category_id)
);

ALTER TABLE product_profile_app CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
ALTER TABLE product_profile_app DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;





#   latest update data

#   app db

alter table supplier_app change  column name_manufacture display_name varchar(255);

#   oc db

update oc_category_description
set description = 'nowystyl'
where  description = 'НОВИЙ СТИЛЬ';

update oc_category_description
set description = 'maresto'
where  description = 'МАРЕСТО';

update oc_category_description
set description = 'kodaki'
where  description = 'КОДАКИ';

update oc_category_description
set description = 'indigowood'
where  description = 'INDIGOWOOD';

-----

update oc_product
set jan = 'nowystyl'
where  jan = 'НОВИЙ СТИЛЬ';

update oc_product
set jan = 'maresto'
where  jan = 'МАРЕСТО';

update oc_product
set jan = 'kodaki'
where  jan = 'КОДАКИ';

update oc_product
set jan = 'indigowood'
where  jan = 'INDIGOWOOD';



alter table attribute_app change  column replacement_text replacement_from varchar(255);
alter table attribute_app add column replacement_to varchar (255) after replacement_from;
update attribute_app
set attribute_app.replacement_to = ''
where attribute_app.replacement_to is null;