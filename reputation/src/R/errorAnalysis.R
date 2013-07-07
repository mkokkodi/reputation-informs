results <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/allPredictions.csv",head=TRUE,sep=",")
summary(results)
library(ggplot2)
library(grid)
library(scales)     # Need the scales package
printErrors<-function(){
  cat(sprintf("Model & Approachs &  History & Baseline & Basic Model \n"))
for(model in unique(results$model)){
  for(approach in unique(results$approach)){
    for(history in unique(results$HistoryThreshold)){
  mult<-results[results$model==model & results$approach==approach & results$HistoryThreshold==history,]
  errorsModel<-abs(mult$Nohierarchies - mult$actual)
  errorsBaseline<-abs(mult$baseline -mult$actual)
  cat(sprintf("%s & %s & %s & %.3f  (%.4f) &  %.3f (%.4f)  \\\\ \n ",model,approach,history,
              mean(errorsBaseline),sqrt(var(errorsBaseline)), 
              mean(errorsModel), sqrt(var(errorsModel))
              ))
}
}
}
}

printErrors()


  
kalmanErrors<-transform(results,error=abs(results$Nohierarchies - results$actual),Method="LDS")
kalmanErrors<-kalmanErrors[kalmanErrors$HistoryThreshold==15 & kalmanErrors$model=='Kalman',]
  
binErrors<-transform(results,error=abs(results$Nohierarchies - results$actual),Method="Binomial")
binErrors<-binErrors[binErrors$HistoryThreshold==15 & binErrors$model=='Binomial',]

multErrors<-transform(results,error=abs(results$Nohierarchies - results$actual),Method="Multinomial")
multErrors<-multErrors[multErrors$HistoryThreshold==15 & multErrors$model=='Multinomial',]

baselineErrors<-transform(results,error=abs(results$baseline - results$actual),Method="Baseline")
baselineErrors<-baselineErrors[baselineErrors$HistoryThreshold==15 & baselineErrors$model=='Kalman',]


allErrors <- rbind(baselineErrors,kalmanErrors)  
summary(allErrors)
mysample <- allErrors[sample(1:nrow(allErrors), 100000,replace=FALSE),]
summary(mysample)

ggplot(allErrors, aes(x=error,fill=method))  + geom_density(alpha=.4) +facet_wrap(~category,ncol=6)+theme_bw(base_size = 16) +
  xlab("Mean Absolute Error")+
  ylab("Density")+labs(fill="")+ scale_x_continuous(limits = c(0, 1),breaks=c(0,0.25,0.5,0.75), 
                                      labels=c("0","0.25","0.5","0.75"))+
  theme(legend.position = "top",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm")) 

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/errorsTransitions.pdf",width=16,height=16,dpi=300)


allErrors <- rbind(baselineErrors,kalmanErrors,binErrors,multErrors)  
mysample <- allErrors[sample(1:nrow(allErrors), 50000,replace=FALSE),]
ggplot(mysample, aes(x=error, linetype=Method, colour=Method),log="y")  + 
  #geom_histogram(binwidth=.01, alpha=.5, position="identity")+ 
  geom_density(alpha=0.3,adjust=10, size=1.8) +
  theme_bw(base_size = 20) + #facet_wrap(~method,ncol=2) +
  xlab("Mean Absolute Error")+
  ylab("Log-Density")+labs(fill="")+ scale_x_continuous(limits = c(0, 1),breaks=c(0,1), 
                                                    labels=c("0","1"))+ #scale_y_log10(breaks = c(0.01,0.1,1,10))+
  theme(legend.position = "top",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm")) 

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/allModelsErrorsOnTransitions.pdf",width=10,height=8,dpi=300)

printErrorsByCategory()

printErrorsByCategory<-function(){
  cat(sprintf("Transition &    Baseline & Binomial &  Multinomial & LDS  \n"))
  sortedCats<-sort(unique(results$category))
  for(cat in sortedCats){
      bin<-results[results$category==cat & results$HistoryThreshold==15 & results$model=='Binomial',]
      mult<-results[results$category==cat & results$HistoryThreshold==15 & results$model=='Multinomial',]
      kalman<-results[results$category==cat & results$HistoryThreshold==15 & results$model=='Kalman',]
      
      
      errorsBin<-abs(bin$Nohierarchies - bin$actual)
      errorsMult<-abs(mult$Nohierarchies - mult$actual)
      errorsKalman<-abs(kalman$Nohierarchies -kalman$actual)
      errorsBaseline<-abs(kalman$baseline - kalman$actual)
      #cat(sprintf("%s   & %.3f  (%.4f) &  %.3f (%.4f)  &  %.3f  (%.4f) &  %.3f  (%.4f) \\\\ \n ",cat,mean(errorsBaseline), sqrt(var(errorsBaseline)),
       #           mean(errorsBin),sqrt(var(errorsBin)), 
        #          mean(errorsMult),sqrt(var(errorsMult)),
         #         mean(errorsKalman),sqrt(var(errorsKalman))))
      
      cat(sprintf("%s   & %.3f  &  %.3f &  %.3f &  %.3f   \\\\ \n ",cat,mean(errorsBaseline), mean(errorsBin),
                  mean(errorsMult), mean(errorsKalman)))
      
    }
}