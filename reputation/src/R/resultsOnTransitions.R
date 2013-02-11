
library(ggplot2)
library(grid)



hier <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/results/results_onTransitions.csv",head=TRUE,sep=",")
summary(hier)
mae_improvement<-(hier$MAEBaseline -hier$MAEModel )/hier$MAEBaseline
hier_improvements = transform(hier,improvement=mae_improvement )
hier_improvements
summary(hier_improvements)

ob1 <- ggplot(hier_improvements,aes(HistoryThreshold,mae_improvement*100, colour=factor(exactModel),shape=exactModel))

ob1+geom_point(size=4.5)+facet_wrap(~model*approach * ScoreThreshold,ncol=3) + theme_bw(base_size = 22) +
  xlab(expression(History - eta))+
  ylab("Improvement %")+ 
  labs(colour="",shape="")+theme(legend.position = "top",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/results_onTransitions.pdf",width=17,height=10,dpi=300)
