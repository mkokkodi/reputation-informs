# This code generates all the improvemkent graphs in the paper:
# For cross validation input files: cvAverageResults.csv and cvAverageResults_onTransitions.csv : Output: cv.pdf, cv_onTransitions.pdf
# For hold out evaluation input files: results.csv and results_onTransitions.csv : Output: real.pdf, realOnTransitions.pdf
# For hierarchies: results_hier.csv and results_onTransitions_hier.csv | outpur: realHier.pdf, realOnTransitionsHier.pdf


library(ggplot2)
library(grid)


hier <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/results_onTransitions_hier.csv",head=TRUE,sep=",")
summary(hier)
mae_improvement<-(hier$MAEBaseline -hier$MAEModel )/hier$MAEBaseline

hier_improvements <- transform(hier,improvement=mae_improvement )
hier_improvements


levels(hier_improvements$model)[levels(hier_improvements$model)=="Kalman"] <- "LDS"


ob1 <- ggplot(hier_improvements,aes(HistoryThreshold,improvement*100, colour=model,shape=model))

ob1+geom_point(size=5)+geom_line(size=1)+facet_wrap(~approach,ncol=2) + theme_bw(base_size = 24) +
  xlab(expression(History - eta))+
  ylab("Improvement %")+ 
  labs(colour="",shape="")+theme(legend.position = "right",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/realOnTransitionsHier.pdf",width=15,height=5,dpi=300)

