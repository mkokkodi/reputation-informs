results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/regFile.csv",head=TRUE,sep=",")
head(results)
cat10 <- results[results$baseCat=="10",]


summary(cat10)

lm10 <- lm(cat10$y ~ cat10$overall + cat10$web.dev + cat10$soft.dev + cat10$writing 
           + cat10$administrative +  cat10$des.mult + cat10$sales.and.marketing  -1)
summary(lm10)


results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/bigErrors.csv",head=TRUE,sep=",")
summary(results)

library(ggplot2)
hist3 <-results[results$history==3,]

ggplot(results, aes(x=error,fill=factor(model)))  + geom_density(alpha=.4) 