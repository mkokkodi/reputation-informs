results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/predictions_0.5.csv",head=TRUE,sep=",")
summary(results)



printErrors<-function(approach){
cat(sprintf("Approach: %s \n",approach) )
for(history in unique(results$HistoryThreshold)){
  mult<-results[results$HistoryThreshold ==history & results$approach==approach,]
  head(mult)
  errorsR<-abs(mult$Nohierarchies - mult$actual)
  errorsEM<-abs(mult$Shrinkage - mult$actual)
  cat(sprintf("%i &  %.5f & (%.5f) &  %.5f & (%.5f) & \n ",history,mean(errorsR),sqrt(var(errorsR)), 
              mean(errorsEM),sqrt(var(errorsEM))
  ))
}
}


printErrors("PE")