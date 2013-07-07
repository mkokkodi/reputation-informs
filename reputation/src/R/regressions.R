results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/regFile_Kalmanr.csv",head=TRUE,sep=",")
head(results)

orderCats <- unique(results$baseCat)

runRegressions(results)


categories <- c("Web Dev ", "Soft Dev ", "Writing ","Admin. ", "Des Mult. ", "Sales ")
names(categories) <- c(10, 20,40,50,60,80)
categories[as.character(10)]

runRegressions<-function(results){
j<-0
for(cat in orderCats ){
  
  catOnly <- results[results$baseCat==cat,]
  lm10 <- lm(catOnly$y ~  catOnly$X10 + catOnly$X20 + catOnly$X40 
             + catOnly$X50 +  catOnly$X60 + catOnly$X80 - 1 )
  cat(sprintf("& %s &",categories[as.character(cat)]))
  if(cat == 10){
    coeffs<-summary(lm10)$coefficients[,1]
  }
     else{
  coeffs<- cbind(coeffs,summary(lm10)$coefficients[,1])
     }
  j<-j+1
  for(i in  1:6)
    cat(sprintf("%.3f(%.3f) & ",summary(lm10)$coefficients[i,][1],summary(lm10)$coefficients[i,][2]))
  cat(sprintf("%.3f   ",summary(lm10)$adj.r.squared))
                cat("\\\\ \n")
}
#return(coeffs)
}

summary(lm10)
#,summary(lm10)$fstatistic[1]))
summary(lm10)$coefficients
coef
summary(lm10)$coefficients[,1]
summary(lm10)
cat
lm10$model
names(summary(lm10))



for(cat2 in 1:6){
  res<-data.frame(xcat=numeric(0), X10=numeric(0), X20 = numeric(0), X40 = numeric(0), X50 = numeric(0), X60 = numeric(0), X80 = numeric(0))
  
  for(i in 1:10){
    mysample <- results[sample(1:nrow(results), noSamples,replace=FALSE),]
    # cat(sprintf("Running Iteration %i \n ",i))
    coeffs <- runRegressions(mysample)
    #rbind(df, list(A=42, B='foo', C='bar')) 
    res <-rbind(res,list(xcat=cat2,X10=coeffs[1,cat2], X20=coeffs[2,cat2],X40=coeffs[3,cat2], X50=coeffs[4,cat2],
                         X60=coeffs[5,cat2], X80=coeffs[6,cat2]))  
    
    
    
  }
  cat("\n",colMeans(res))
}

coeffs
coeffs<-runRegressions(results)

coeffs[,1]
colMeans(res)
cat<-10

results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/bigErrors.csv",head=TRUE,sep=",")
summary(results)

library(ggplot2)
hist3 <-results[results$history==3,]

ggplot(results, aes(x=error,fill=factor(model)))  + geom_density(alpha=.4) 