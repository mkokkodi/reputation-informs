﻿
drop table if exists mkokkodi.contractor_categories;
create table mkokkodi.contractor_categories as

select count(distinct(contractor)) from(
select contractor from mkokkodi.contractor_categories
where cat in (10,20,40,50,60,80)
group by contractor
having count(*) > 2) t

drop table if exists mkokkodi.reputation_train_test;
create table mkokkodi.reputation_train_test as
select t1."Record ID#" as "feedbackId", 
"Developer (ref)" as contractor, order1 as category, "Related JobCategory" as special_cat, "Provider Availability Score"
as availability,  "EndDate" as job_date,  
"Provider Total Score" as total, 
"Provider Communication Score" as communication,"Provider Cooperation Score" as cooperation,
"Provider Deadlines Score" as deadlines , "Provider Quality Score" as quality, 
"Provider Skills Score" as skills
from "oDesk DB"."Feedbacks"  t1 inner join  "oDesk DB"."Assignments"  
t2 on t1."Related Assignment"=t2."Record ID#"  inner join 
mkokkodi.categories2level1 cat on cat_id = "Related JobCategory"
inner join 
(
select contractor
from(
select contractor from mkokkodi.contractor_categories
where cat in (10,20,40,50,60,80)) t
group by contractor
having count(*) >= 1) r
on r.contractor = "Developer (ref)"
where "Related JobCategory" is not null  and "Provider Total Score"!=0 
and order1 in (10,20,40,50,60,80)
distributed randomly;

select count(distinct(contractor)) from mkokkodi.reputation_train_test

drop table if exists mkokkodi.reputation_train_test_3to100;
create table mkokkodi.reputation_train_test_3to100 as
select t.* from mkokkodi.reputation_train_test t inner join  
(
select contractor as c from mkokkodi.reputation_train_test
group by contractor having count(*) > 3 and count(*) < 100) r
on r.c = t.contractor
distributed randomly



/* Split 70-30 on contractors */
drop table if exists mkokkodi.reputation_test;
create table mkokkodi.reputation_test as
select * from mkokkodi.reputation_train_test_3to100
where contractor % 7= 1
distributed randomly

drop table if exists mkokkodi.reputation_train;
create table mkokkodi.reputation_train as
select * from mkokkodi.reputation_train_test_3to100
where contractor % 13 != 1  and contractor %5 != 1 and contractor %7 != 1  
distributed randomly

create index someRandIndex on mkokkodi.reputation_train(job_date);

create index someRandIndex2 on mkokkodi.reputation_test(job_date);


select  contractor,category,
(availability+communication+cooperation+deadlines+quality+skills+total)/7/5 as score
from mkokkodi.reputation_train  where category is not null and length(category)>1
order by job_date

select  contractor,special_cat,
(availability+communication+cooperation+deadlines+quality+skills+total)/7/5 as score
from mkokkodi.reputation_train  where category is not null and length(category)>1
order by job_date

select (availability+communication+cooperation+deadlines+quality+skills+total)/7/5 as score 
from mkokkodi.reputation_train  where category is not null and length(category)>1

select * from mkokkodi.categories2level1
where order1 in (10,20,40,50,60,80)
order by order1

select "Level1", "Level2"
from "oDesk DB"."JobCategories"
where "Order1" in (10,20,40,50,60,80)